package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.*;
import dev.mcai.companion.agent.navigation.NavigationFollower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Source-informed real physics test; the followed player moves via normal control frames. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class FollowGameTests {
    @GameTest(name="continuous_follow_regressions",structure="forge:empty48x32x48",maxTicks=900,padding=8)
    public static void run(GameTestHelper h) {
        if(!Boolean.getBoolean("minepilot.followTest")){h.fail("Follow gate not selected");return;}
        new Gate(h).start();
    }
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime runtime;final CodexToolService tools;
        HeadlessPlayerSession human;String request;int phase;long began;Vec3 held;double humanStart;
        boolean replaced;int restCycles;long restingSince=-1;int movingTicks,stoppedWhileTargetMoved;long resumed=-1;final JsonArray evidence=new JsonArray();
        Gate(GameTestHelper h){this.h=h;runtime=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(runtime);}
        JsonObject call(String name,String args){
            var params=new JsonObject();params.addProperty("name",name);params.add("arguments",JsonParser.parseString(args));
            var req=new JsonObject();req.add("params",params);var out=tools.dispatch("tools/call",req);
            h.assertTrue(!out.get("isError").getAsBoolean(),"Public tool failed: "+out);return out.getAsJsonObject("structuredContent");
        }
        void start(){
            var origin=h.absolutePos(new BlockPos(4,2,20));
            for(int x=-2;x<=38;x++)for(int z=-8;z<=8;z++){
                h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<=3;y++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            var p=runtime.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.setGameMode(GameType.ADVENTURE);p.getInventory().clearContent();p.level().getChunkSource().move(p);
            human=HeadlessPlayerSession.join(runtime.server(),h.getLevel(),"follow-gate","FollowHuman",p.getX()+6,p.getY(),p.getZ());
            human.player().setPos(p.getX()+6,p.getY(),p.getZ());human.player().stopControlling();human.player().setGameMode(GameType.ADVENTURE);
            human.player().level().getChunkSource().move(human.player());
            h.addCleanup(ignored->{human.close();runtime.onChat("TestHuman","停下");});
            begin();h.onEachTick(this::tick);
        }
        void begin(){
            var accepted=call("request_navigation","{\"target_kind\":\"player\",\"target_name\":\"FollowHuman\",\"acceptance_radius\":2,\"preferred_pace\":\"walk\",\"continuous_follow\":true,\"player_intent\":\"keep following\"}");
            request=accepted.get("requestId").getAsString();call("say","{\"navigation_request_id\":\""+request+"\",\"message\":\"好，我会跟着你。\"}");call("plan_navigation","{\"request_id\":\""+request+"\"}");
        }
        void choose(JsonObject status){
            var option=status.getAsJsonArray("routeOptions").get(0).getAsJsonObject();
            call("choose_navigation","{\"request_id\":\""+request+"\",\"option_id\":\""+option.get("optionId").getAsString()+"\",\"pace\":\"walk\"}");
        }
        void tick(){
            if(phase<5)human.tick();var p=runtime.player();var status=call("poll_events","{}").getAsJsonObject("navigation");var state=status.get("phase").getAsString();
            if(h.getTick()%100==0)dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Follow gate stage={} state={} distance={} body={} target={}",phase,state,p.distanceTo(human.player()),p.position(),human.player().position());
            h.assertTrue(p.getInventory().isEmpty() && p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Follow changed adventure inventory/mode");
            var row=new JsonObject();row.addProperty("tick",h.getTick());row.addProperty("phase",state);row.addProperty("x",p.getX());row.addProperty("y",p.getY());row.addProperty("z",p.getZ());row.addProperty("targetX",human.player().getX());row.addProperty("requestId",request);evidence.add(row);
            if(phase>=2 && phase<4)h.assertTrue(!state.equals("PLAN_READY"),"Safe follow unexpectedly needs a new choice: "+status);
            if(phase<4)h.assertTrue(!state.equals("FAILED") && !state.equals("REPLAN_REQUIRED"),"Safe follow failed or required model repair: "+status);
            if(phase==0 && state.equals("PLAN_READY")){choose(status);phase=1;return;}
            if(phase==1 && state.equals("FOLLOWING") && !replaced){
                String old=request;
                for(String target: new String[]{"MissingFollowTarget", "FollowHuman"}) {
                    var args=new JsonObject();args.addProperty("target_kind","player");args.addProperty("target_name",target);
                    args.addProperty("preferred_pace","walk");args.addProperty("player_intent","replace goal");
                    args.addProperty("replace_request_id",target.equals("FollowHuman")?java.util.UUID.randomUUID().toString():old);
                    var params=new JsonObject();params.addProperty("name","request_navigation");params.add("arguments",args);
                    var req=new JsonObject();req.add("params",params);
                    h.assertTrue(tools.dispatch("tools/call",req).get("isError").getAsBoolean(),"Invalid replacement accepted");
                    var unchanged=call("navigation_status","{}");
                    h.assertTrue(unchanged.get("requestId").getAsString().equals(old) && unchanged.get("phase").getAsString().equals("FOLLOWING"),"Invalid replacement stopped the old goal");
                }
                var args=new JsonObject();args.addProperty("target_kind","player");args.addProperty("target_name","FollowHuman");args.addProperty("preferred_pace","walk");args.addProperty("player_intent","replace goal");args.addProperty("replace_request_id",old);args.addProperty("continuous_follow",true);
                var accepted=call("request_navigation",args.toString());request=accepted.get("requestId").getAsString();
                h.assertTrue(!request.equals(old) && accepted.get("phase").getAsString().equals("ACKNOWLEDGEMENT_REQUIRED"),"Replacement skipped acknowledgement");
                call("say","{\"navigation_request_id\":\""+request+"\",\"message\":\"好，按新的目标跟随。\"}");call("plan_navigation","{\"request_id\":\""+request+"\"}");
                replaced=true;phase=0;return;
            }
            if(phase==1 && state.equals("FOLLOWING")){
                h.assertTrue(p.distanceTo(human.player())<=2,"Waiting without actual arrival");
                held=p.position();humanStart=human.player().getX();began=h.getTick();phase=2;
                human.player().applyControlFrame(new AgentControlFrame(NavigationFollower.headingToMinecraftYaw(90),0,1,0,false,false,false));return;
            }
            if(phase==2){
                if(p.tickCount%10==0)h.assertTrue(Math.abs(p.getXRot())<=25,"Walking gaze points steeply at the feet: "+p.getXRot());
                if(resumed>=0 && human.player().getX()-humanStart>4) {
                    movingTicks++;if(p.getDeltaMovement().horizontalDistanceSqr()<.0004)stoppedWhileTargetMoved++;
                }
                if(resumed<0 && p.position().distanceTo(held)>.1)resumed=h.getTick()-began;
                if(h.getTick()-began==12){runtime.onPlayerChat(human.player(),"跟着的时候也能说话吗？");call("say","{\"message\":\"可以，我还在跟着。\"}");}
                h.assertTrue(status.get("requestId").getAsString().equals(request),"Following silently created a new request");
                if(human.player().getX()-humanStart>=10){human.player().stopControlling();phase=3;}
                return;
            }
            if(phase==3 && state.equals("FOLLOWING")){
                var visible=call("read_chat","{}").getAsJsonObject("systemChat").getAsJsonArray("messages");
                boolean ownReply=false;
                for(var message:visible)if(message.getAsJsonObject().get("text").getAsString().contains("可以，我还在跟着。"))
                    ownReply=message.getAsJsonObject().get("origin").getAsString().equals("received_agent_chat");
                h.assertTrue(ownReply,"Public chat failed to expose actual delivered Agent reply");
                h.assertTrue(p.distanceTo(human.player())<=2 && p.position().distanceTo(held)>7,"Follow did not physically reacquire moving player");
                h.assertTrue(resumed>0 && resumed<=35,"Follow resume exceeded 35 ticks: "+resumed);
                h.assertTrue(stoppedWhileTargetMoved<=Math.max(2,movingTicks/10),"Follow repeatedly stopped while target walked: "+stoppedWhileTargetMoved+"/"+movingTicks);
                if(restCycles==0) {
                    if(restingSince<0)restingSince=h.getTick();
                    if(h.getTick()-restingSince<80)return;
                    h.assertTrue(status.get("requestId").getAsString().equals(request),"Rest discarded the follow subscription");
                    restCycles++;held=p.position();humanStart=human.player().getX();began=h.getTick();resumed=-1;phase=2;
                    human.player().applyControlFrame(new AgentControlFrame(-90,0,1,0,false,false,false));return;
                }
                call("cancel_navigation","{\"request_id\":\""+request+"\",\"reason\":\"test stop\"}");
                held=p.position();began=h.getTick();phase=4;human.player().applyControlFrame(new AgentControlFrame(-90,0,1,0,false,false,false));return;
            }
            if(phase==4 && h.getTick()-began>=30){
                h.assertTrue(state.equals("CANCELLED") && p.position().distanceTo(held)<.05,"Cancelled follow resumed itself");
                human.player().stopControlling();begin();phase=5;return;
            }
            if(phase==5 && state.equals("PLAN_READY")){choose(status);human.close();phase=6;return;}
            if(phase==6 && state.equals("FAILED")){
                h.assertTrue(status.get("lastEventMessage").getAsString().contains("No online player"),"Lost target reason missing");
                try {java.nio.file.Files.writeString(runtime.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("follow-physical-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(java.io.IOException e){throw new IllegalStateException(e);}
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Continuous follow physically verified: resume={} ticks, same request, chat during movement, stable stop, lost-player failure",resumed);
                human=HeadlessPlayerSession.join(runtime.server(),h.getLevel(),"knockback-gate","AttackHuman",p.getX()+1,p.getY(),p.getZ());
                human.player().setPos(p.getX()+1,p.getY(),p.getZ());human.player().setYRot(90);human.player().setGameMode(GameType.SURVIVAL);human.player().setSprinting(true);
                p.setInvulnerable(false);p.invulnerableTime=0;p.setHealth(20);held=p.position();
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Attack prerequisites: pvp={} loaded={} changingDimension={} invulnerable={} abilities={} attackDamage={}",p.level().isPvpAllowed(),p.connection.hasClientLoaded(),p.isChangingDimension(),p.isInvulnerableTo(p.level(),p.damageSources().playerAttack(human.player())),p.getAbilities().invulnerable,human.player().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                human.player().attack(p);h.assertTrue(p.getHealth()<20,"Native player attack did not damage body at impact");began=h.getTick();phase=7;return;
            }
            if(phase==7 && h.getTick()-began>=12){
                h.assertTrue(p.position().distanceTo(held)>.3,"Native player attack failed to knock body back: "+p.position().distanceTo(held));
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Native player attack physically displaced MinePilot {} blocks",p.position().distanceTo(held));
                human.close();phase=8;h.succeed();
            }
        }
    }
}
