package dev.mcai.companion.agent.mining;

import java.util.*;

/** Pure bounded search over an immutable observed snapshot; never reads a Minecraft world. */
public final class ExcavationPlanner {
    public record Pos(int x,int y,int z) {
        public Pos add(int dx,int dy,int dz){return new Pos(x+dx,y+dy,z+dz);}
        public double distance(Pos b){return Math.sqrt((double)(x-b.x)*(x-b.x)+(double)(y-b.y)*(y-b.y)+(double)(z-b.z)*(z-b.z));}
    }
    public record Tool(String identity,String item,int slot,int remaining,int wear,double ticks,boolean harvests) {}
    public record Voxel(boolean empty,boolean full,boolean safe,boolean accessAllowed,String state,List<Tool> tools) {
        public Voxel {tools=List.copyOf(tools);}
    }
    public record Stock(String identity,String item,int count,int importance) {}
    public record Snapshot(Pos origin,Map<Pos,Voxel> cells,List<Pos> targets,List<Stock> supports,
                           boolean access,boolean useSupports,boolean harvest,boolean approachOnly,int maxBreaks,double maxDistance,boolean returnToOrigin,double reach) {
        public Snapshot(Pos origin,Map<Pos,Voxel> cells,List<Pos> targets,List<Stock> supports,boolean access,boolean useSupports,boolean harvest,boolean approachOnly,int maxBreaks,double maxDistance){this(origin,cells,targets,supports,access,useSupports,harvest,approachOnly,maxBreaks,maxDistance,false,4.5);}
        public Snapshot(Pos origin,Map<Pos,Voxel> cells,List<Pos> targets,List<Stock> supports,boolean access,boolean useSupports,boolean harvest,boolean approachOnly,int maxBreaks,double maxDistance,boolean returnToOrigin){this(origin,cells,targets,supports,access,useSupports,harvest,approachOnly,maxBreaks,maxDistance,returnToOrigin,4.5);}
        public Snapshot {cells=Map.copyOf(cells);targets=List.copyOf(targets);supports=List.copyOf(supports);}
    }
    public record Step(String action,Pos position,Pos from,Tool tool,String material,String expectedState,boolean resource) {}
    public record Plan(String id,List<Step> steps,List<Pos> targets,Map<String,Integer> wear,Map<String,Integer> materials,
                       double distance,double seconds,int accessBlocks,String limitation) {
        public Plan {steps=List.copyOf(steps);targets=List.copyOf(targets);wear=Map.copyOf(wear);materials=Map.copyOf(materials);}
    }
    private static final Voxel AIR=new Voxel(true,false,true,false,"air",List.of());
    private static final Voxel SUPPORT=new Voxel(false,true,true,false,"planned support",List.of());
    private final Snapshot input;
    private final Map<Pos,Voxel> world;
    private final Map<String,Integer> wear=new HashMap<>(), materials=new HashMap<>();
    private final List<Step> steps=new ArrayList<>();
    private final List<Pos> completed=new ArrayList<>();
    private final Set<Pos> resourceTargets;
    private final String mode;
    private double distance,seconds;
    private int breaks,accessBlocks;
    private Pos position;
    private final Set<Pos> passages=new HashSet<>();
    private final long deadline;
    private ExcavationPlanner(Snapshot input,String mode,long deadline){this.input=input;this.mode=mode;this.deadline=deadline;world=new HashMap<>(input.cells);position=input.origin;resourceTargets=new HashSet<>(input.targets);passages.add(position);passages.add(position.add(0,1,0));}
    public static List<Plan> alternatives(Snapshot input){
        var plans=new ArrayList<Plan>();var signatures=new HashSet<String>();
        long deadline=System.nanoTime()+3_000_000_000L;
        for(String mode:List.of("fast","preserve_tools","minimal_excavation")){
            var engine=new ExcavationPlanner(input,mode,deadline);var plan=engine.plan();
            String signature=plan.steps.toString();
            if((!plan.steps.isEmpty() || input.approachOnly && !plan.targets.isEmpty()) && signatures.add(signature))plans.add(plan);
            if(System.nanoTime()>deadline)break;
        }
        return List.copyOf(plans);
    }
    private Plan plan(){
        var todo=new LinkedHashSet<>(input.targets);String limitation="";
        while(!todo.isEmpty()){
            if(Thread.currentThread().isInterrupted() || System.nanoTime()>deadline){limitation="Bounded planner time exhausted";break;}
            var ordered=todo.stream().sorted(Comparator.comparingDouble(p->position.distance(p))).limit(16).toList();
            boolean advanced=false;
            for(Pos target:ordered){
                var voxel=world.get(target);
                if(voxel==null || !voxel.safe || voxel.empty){todo.remove(target);continue;}
                if(!input.access && !resourceTargets.contains(target))continue;
                var path=route(target);
                if(path==null)continue;
                // Validate the entire next operation's resource budget before adding any of it.
                var additions=new ArrayList<Step>();for(var edge:path)additions.addAll(edge.steps);
                Pos stand=path.isEmpty()?position:path.getLast().to;
                var tool=tool(voxel,input.harvest);
                if(tool==null && !input.approachOnly)continue;
                if(!input.approachOnly)additions.add(new Step("break",target,stand,tool,"",voxel.state,true));
                if(!budget(additions))continue;
                for(var step:additions)apply(step);
                position=stand;if(!completed.contains(target))completed.add(target);todo.removeAll(completed);advanced=true;break;
            }
            if(!advanced){limitation="Remaining targets have no observed safe route within the approved excavation, support and tool budgets";break;}
        }
        if(input.returnToOrigin && completed.size()==input.targets.size()){
            var home=route(input.origin,true);if(home==null)limitation="No evaluated return corridor after excavation";
            else {var additions=new ArrayList<Step>();for(var edge:home)additions.addAll(edge.steps);if(!budget(additions))limitation="Return corridor exceeds approved resources or travel budget";else for(var step:additions)apply(step);}
        }
        return new Plan(mode,steps,completed,wear,materials,distance,seconds,accessBlocks,limitation);
    }
    private Tool tool(Voxel voxel,boolean harvest){
        return voxel.tools.stream().filter(t->!harvest || t.harvests)
                .filter(t->t.remaining<0 || t.remaining-wear.getOrDefault(t.identity,0)>t.wear)
                .min(Comparator.comparingDouble(t->t.ticks+(mode.equals("preserve_tools")?t.wear*600:0))).orElse(null);
    }
    private record Edge(Pos from,Pos to,List<Step> steps,double cost) {}
    private record Search(Pos position,double score,Search parent,Edge incoming) {}
    private List<Edge> route(Pos target){return route(target,false);}
    private List<Edge> route(Pos target,boolean standingGoal){
        var best=new HashMap<Pos,Double>();
        var queue=new PriorityQueue<Search>(Comparator.comparingDouble(Search::score));
        best.put(position,0.0);queue.add(new Search(position,0,null,null));int expanded=0;
        while(!queue.isEmpty() && expanded++<16000){
            if((expanded&127)==0 && (System.nanoTime()>deadline || Thread.currentThread().isInterrupted()))return null;
            var current=queue.poll();if(current.score>best.getOrDefault(current.position,Double.POSITIVE_INFINITY)+1e-8)continue;
            if(standingGoal?current.position.equals(target):reaches(current,target)){
                var path=new ArrayList<Edge>();Search end=current;
                while(end.incoming!=null){path.add(end.incoming);end=end.parent;}
                Collections.reverse(path);return path;
            }
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,-1,1}){
                Pos next=current.position.add(d[0],dy,d[1]);
                if(!standingGoal && (next.equals(target) || next.add(0,-1,0).equals(target) || next.add(0,1,0).equals(target)))continue;
                var edge=edge(current,next,standingGoal?null:target);if(edge==null)continue;
                double cost=current.score+edge.cost;
                if(cost+1e-8<best.getOrDefault(next,Double.POSITIVE_INFINITY)){
                    best.put(next,cost);queue.add(new Search(next,cost,current,edge));
                }
            }
        }
        return null;
    }
    private boolean reaches(Search node,Pos target){
        Pos stand=node.position;double ex=stand.x+.5,ey=stand.y+1.62,ez=stand.z+.5;
        if(stand.equals(target) || stand.add(0,-1,0).equals(target) || stand.add(0,1,0).equals(target))return false;
        for(double[] offset:new double[][]{{.01,.5,.5},{.99,.5,.5},{.5,.01,.5},{.5,.99,.5},{.5,.5,.01},{.5,.5,.99}}){
            double dx=target.x+offset[0]-ex,dy=target.y+offset[1]-ey,dz=target.z+offset[2]-ez;
            if(dx*dx+dy*dy+dz*dz>Math.pow(Math.min(5,input.reach),2))continue;
            int x=(int)Math.floor(ex),y=(int)Math.floor(ey),z=(int)Math.floor(ez);
            int sx=dx>0?1:dx<0?-1:0,sy=dy>0?1:dy<0?-1:0,sz=dz>0?1:dz<0?-1:0;
            double tx=boundary(ex,dx,x,sx),ty=boundary(ey,dy,y,sy),tz=boundary(ez,dz,z,sz);
            for(int n=0;n<32;n++){
                var at=new Pos(x,y,z);if(at.equals(target))return true;var cell=voxel(node,at);if(cell==null || !cell.empty)break;
                double next=Math.min(tx,Math.min(ty,tz));if(next>1)break;
                if(tx<=next){x+=sx;tx+=sx==0?Double.POSITIVE_INFINITY:Math.abs(1/dx);}if(ty<=next){y+=sy;ty+=sy==0?Double.POSITIVE_INFINITY:Math.abs(1/dy);}if(tz<=next){z+=sz;tz+=sz==0?Double.POSITIVE_INFINITY:Math.abs(1/dz);}
            }
        }return false;
    }
    private static double boundary(double start,double delta,int cell,int sign){return sign==0?Double.POSITIVE_INFINITY:((sign>0?cell+1:cell)-start)/delta;}
    private Voxel voxel(Search node,Pos p){
        for(var at=node;at!=null && at.incoming!=null;at=at.parent){
            var path=at.incoming.steps;for(int i=path.size()-1;i>=0;i--){var s=path.get(i);if(s.position.equals(p)){if(s.action.equals("break"))return AIR;if(s.action.equals("support"))return SUPPORT;}}
        }return world.get(p);
    }
    private boolean reservedPassage(Search node,Pos p){
        if(passages.contains(p) || resourceTargets.contains(p))return true;
        for(var at=node;at!=null;at=at.parent)if(p.equals(at.position) || p.equals(at.position.add(0,1,0)))return true;
        return false;
    }
    private Edge edge(Search node,Pos to,Pos target){
        Pos from=node.position;
        var floor=voxel(node,to.add(0,-1,0));if(floor==null || !floor.safe)return null;
        var cells=new LinkedHashSet<Pos>();
        if(to.y>from.y)cells.add(from.add(0,2,0));
        cells.add(to.add(0,1,0));cells.add(to);
        var actions=new ArrayList<Step>();double cost=from.distance(to)*5;
        for(Pos p:cells){
            var v=voxel(node,p);if(v==null || !v.safe || p.equals(target))return null;
            if(v.empty)continue;
            if(!resourceTargets.contains(p) && (!input.access || !v.accessAllowed))return null;
            if(p.equals(from) || p.equals(from.add(0,1,0)) || p.equals(from.add(0,-1,0)))return null;
            var tool=tool(v,resourceTargets.contains(p)&&input.harvest);if(tool==null)return null;
            actions.add(new Step("break",p,from,tool,"",v.state,resourceTargets.contains(p)));
            cost+=tool.ticks/20.0+(mode.equals("preserve_tools")?tool.wear*30:mode.equals("minimal_excavation")?200:0);
        }
        if(!floor.full){
            if(!floor.empty || !input.useSupports || reservedPassage(node,to.add(0,-1,0)))return null;
            var stock=input.supports.stream().filter(s->s.importance>=3 && s.importance<=5 && s.count>materials.getOrDefault(s.identity,0)).findFirst().orElse(null);
            if(stock==null)return null;
            // Build from a real adjacent floor. For an upward step, a lower anchor
            // and its top block form a supported stair; never place into unsupported air.
            var anchor=voxel(node,from.add(0,-1,0));if(anchor==null || !anchor.full || to.y<from.y)return null;
            if(to.y==from.y+1){
                var lower=to.add(0,-2,0);var low=voxel(node,lower);
                if(low==null || !low.safe || !low.full && !low.empty)return null;
                if(!low.full){if(reservedPassage(node,lower))return null;actions.add(new Step("support",lower,from,null,stock.identity,low.state,false));cost+=10;}
            }
            actions.add(new Step("support",to.add(0,-1,0),from,null,stock.identity,floor.state,false));cost+=10;
        }
        actions.add(new Step("move",to,from,null,"","",false));
        return new Edge(from,to,List.copyOf(actions),cost);
    }
    private boolean budget(List<Step> additions){
        var trialWear=new HashMap<>(wear);var trialMaterials=new HashMap<>(materials);int count=breaks;double travel=distance;
        var removed=new HashSet<Pos>();
        for(var s:additions){
            if(s.action.equals("break") && removed.add(s.position)){
                if(++count>input.maxBreaks)return false;
                var t=s.tool;int used=trialWear.merge(t.identity,t.wear,Integer::sum);
                if(t.remaining>=0 && used>=t.remaining)return false;
            }else if(s.action.equals("support")){
                int used=trialMaterials.merge(s.material,1,Integer::sum);
                int available=input.supports.stream().filter(v->v.identity.equals(s.material)).mapToInt(Stock::count).sum();if(used>available)return false;
            }else if(s.action.equals("move"))travel+=s.from.distance(s.position);
        }
        return travel<=input.maxDistance;
    }
    private void apply(Step s){
        if(s.action.equals("break")){
            if(world.get(s.position).empty)return;
            world.put(s.position,AIR);wear.merge(s.tool.identity,s.tool.wear,Integer::sum);seconds+=s.tool.ticks/20.0+.15;
            breaks++;if(!s.resource)accessBlocks++;else if(!completed.contains(s.position))completed.add(s.position);
        }else if(s.action.equals("support")){
            world.put(s.position,SUPPORT);materials.merge(s.material,1,Integer::sum);seconds+=1;
        }else {passages.add(s.from);passages.add(s.from.add(0,1,0));passages.add(s.position);passages.add(s.position.add(0,1,0));distance+=s.from.distance(s.position);seconds+=s.from.distance(s.position)/3;}
        steps.add(s);
    }
}
