package dev.mcai.companion.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.agent.body.HeadlessPlayerSession;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.model.ModelConfig;
import dev.mcai.companion.agent.navigation.NavigationEvent;
import dev.mcai.companion.agent.navigation.NavigationPlannerConfig;
import dev.mcai.companion.agent.navigation.NavigationSnapshotBuilder;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Owns one visible Agent body and its optional model/navigation runtime. */
public final class AgentRuntime implements AutoCloseable {
    private static final String DEFAULT_AGENT_NAME = "MinePilot";
    private static final int CHAT_HISTORY_LIMIT = 128;
    private static final int EXTERNAL_TRACE_LIMIT = 64;
    private static final Map<MinecraftServer, AgentRuntime> ACTIVE = new IdentityHashMap<>();

    private final MinecraftServer server;
    private final HeadlessPlayerSession session;
    private final NavigationToolCoordinator navigation;
    private final AgentBrain brain;
    private final dev.mcai.companion.agent.placement.PlacementCoordinator placement;
    public dev.mcai.companion.agent.placement.PlacementCoordinator placement(){return placement;}
    private final dev.mcai.companion.agent.mining.CollectionCoordinator collection;
    public dev.mcai.companion.agent.mining.CollectionCoordinator collection(){return collection;}
    private final dev.mcai.companion.agent.mining.MiningCoordinator mining;
    public dev.mcai.companion.agent.mining.MiningCoordinator mining(){return mining;}
    private final Deque<VisiblePlayerChat> playerChat = new ArrayDeque<>();
    private final Deque<ExternalToolCall> externalToolTrace = new ArrayDeque<>();
    private long chatSequence;
    private long externalToolSequence;
    private boolean closed;
    private int jumpStartedTick = -1;
    private boolean jumpSawAirborne;
    private double jumpStartY;
    private String jumpPhase = "IDLE";
    private java.util.UUID attentionTarget;
    public final dev.mcai.companion.agent.knowledge.WorldPerception perception;
    public final dev.mcai.companion.agent.knowledge.SoundPerception hearing;
    private final java.util.Deque<com.google.gson.JsonObject> systemChat = new java.util.ArrayDeque<>();
    private long systemSequence;
    public com.google.gson.JsonObject systemChatSince(long after) {
        var out=new com.google.gson.JsonObject();var rows=new com.google.gson.JsonArray();
        for(var row:systemChat)if(row.get("sequence").getAsLong()>after && rows.size()<50)rows.add(row.deepCopy());
        out.add("messages",rows);out.addProperty("latestSequence",systemSequence);return out;
    }
    private Float turnYaw;
    private String turnPhase="IDLE";
    public boolean turnActive(){return turnYaw!=null;}
    public String turnPhase(){return turnPhase;}
    public void turnTo(double heading) {
        requireServerThread();
        if(placement.ownsBody() || collection.ownsBody() || mining.ownsBody() || jumpActive() || !navigation.status().phase().terminal() && navigation.status().phase()!=NavigationToolCoordinator.Phase.IDLE)
            throw new IllegalStateException("Stop the active movement before a turn-only action");
        if(!player().isAlive())throw new IllegalStateException("Turning requires a living body");
        turnYaw=dev.mcai.companion.agent.navigation.NavigationFollower.headingToMinecraftYaw(dev.mcai.companion.agent.knowledge.WorldPerception.normalize(heading));
        attentionTarget=null;turnPhase="EXECUTING";
    }

    public void jumpOnce() {
        requireServerThread();
        if (placement.ownsBody() || collection.ownsBody() || mining.ownsBody() || jumpStartedTick >= 0 || turnActive()) throw new IllegalStateException("A jump or turn is already active");
        if (!navigation.status().phase().terminal()
                && navigation.status().phase() != NavigationToolCoordinator.Phase.IDLE)
            throw new IllegalStateException("Cancel navigation before requesting a jump");
        if (!player().isAlive() || !player().onGround()) throw new IllegalStateException("Jump requires a living body on stable ground");
        jumpStartedTick = server.getTickCount();
        jumpSawAirborne = false;
        jumpStartY = player().getY();
        jumpPhase = "EXECUTING";
    }

    public String jumpPhase() { return jumpPhase; }
    public boolean jumpActive() { return jumpStartedTick >= 0; }

    private AgentRuntime(
            MinecraftServer server,
            HeadlessPlayerSession session,
            NavigationToolCoordinator navigation,
            AgentBrain brain
    ) {
        this.server = server;
        this.session = session;
        this.navigation = navigation;
        this.brain = brain;
        session.player().inventoryLedger = new dev.mcai.companion.agent.knowledge.InventoryLedger(session.player());
        perception = new dev.mcai.companion.agent.knowledge.WorldPerception(session.player());
        mining = new dev.mcai.companion.agent.mining.MiningCoordinator(this);
        collection = new dev.mcai.companion.agent.mining.CollectionCoordinator(this);
        placement = new dev.mcai.companion.agent.placement.PlacementCoordinator(this);
        hearing = new dev.mcai.companion.agent.knowledge.SoundPerception(session.player());
        session.receivedSound = hearing::receive;
        session.visibleSystemChat = text -> {
            var row=new com.google.gson.JsonObject();row.addProperty("sequence",++systemSequence);row.addProperty("origin",text.startsWith("[AI] ")?"received_agent_chat":"received_system_chat");
            row.addProperty("text",dev.mcai.companion.agent.knowledge.InventoryLedger.bounded(text,512));row.addProperty("gameTick",server.getTickCount());
            systemChat.addLast(row);while(systemChat.size()>128)systemChat.removeFirst();
        };
    }

    public static AgentRuntime start(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Agent runtime must start on the server thread");
        }
        ServerLevel level = server.overworld();
        BlockPos spawn = findSafeSpawn(level);
        String worldIdentity = server.getWorldData().getLevelName();
        HeadlessPlayerSession session = HeadlessPlayerSession.join(
                server,
                level,
                worldIdentity,
                availableName(server),
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5
        );

        AgentBrain brain = null;
        try {
            brain = new AgentBrain(server, session.player(), ModelConfig.fromEnvironment());
        } catch (ModelConfig.MissingModelConfigurationException missing) {
            MinecraftAiCompanion.LOGGER.warn(
                    "MinePilot body is online, but model control is disabled: {}",
                    missing.getMessage());
        }
        AgentBrain attachedBrain = brain;
        NavigationToolCoordinator navigation = new NavigationToolCoordinator(
                server,
                session.player(),
                NavigationPlannerConfig.defaults(),
                NavigationSnapshotBuilder.CaptureConfig.defaults(),
                event -> {
                    if (attachedBrain != null) {
                        attachedBrain.onNavigationEvent(event);
                    }
                }
        );
        if (brain != null) {
            brain.attachNavigation(navigation);
        }
        AgentRuntime runtime = new AgentRuntime(server, session, navigation, brain);
        synchronized (ACTIVE) {
            ACTIVE.put(server, runtime);
        }
        return runtime;
    }

    public MinecraftServer server() {
        return server;
    }

    public void onPlayerChat(ServerPlayer player, String text) {
        if (closed || player == session.player() || text == null || text.isBlank()) {
            return;
        }
        attentionTarget = player.getUUID();
        onChat(player.getGameProfile().name(), text);
    }

    /** Ordinary dedicated-server console chat uses the same bounded input queue. */
    public void onChat(String speakerName, String text) {
        requireServerThread();
        if (closed || text == null || text.isBlank()) return;
        String bounded = text.length() > 512 ? text.substring(0, 512) : text;
        // A small explicit stop vocabulary is a server-side safety control, not
        // the general language encoder. Negated or conversational text does not
        // match. Do not wait for a remote model to stop an active body.
        String stop = bounded.strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[。.!！]+$", "");
        boolean handledLocally = false;
        if (java.util.Set.of("停下", "停止", "stop", "stop moving").contains(stop)) {
            handledLocally = true;
            placement.cancelForChat();
            collection.cancelForChat();
            mining.cancelForChat();
            if(brain!=null)brain.onLocallyHandledStop();
            if(turnActive()){turnYaw=null;turnPhase="CANCELLED";}
            if (jumpActive()) { jumpStartedTick = -1; jumpPhase = "CANCELLED"; handledLocally = true; }
            var state = navigation.status();
            if (state.requestId() != null && !state.phase().terminal()) {
                navigation.cancel(state.requestId(), "Stopped by player chat");
                handledLocally = true;
            }
            player().stopControlling();
            say("已停下，我还在这里。你可以继续聊天或告诉我新的目的地。");
        }
        playerChat.addLast(new VisiblePlayerChat(++chatSequence, speakerName, bounded, server.getTickCount(), handledLocally));
        while (playerChat.size() > CHAT_HISTORY_LIMIT) playerChat.removeFirst();
        if (brain != null && !handledLocally) brain.onChat(speakerName, bounded);
    }

    public void tick() {
        if (closed) {
            return;
        }
        navigation.beforePhysicsTick();
        collection.beforePhysics();
        placement.beforePhysics();
        mining.tickBeforePhysics();
        session.tick();
        player().inventoryLedger.tick();
        navigation.tick();
        collection.tickAfterPhysics();
        placement.afterPhysics();
        perception.structures.tick();
        if(turnActive()) {
            if(!player().isAlive()){turnYaw=null;turnPhase="FAILED";player().stopControlling();}
            else if(Math.abs(net.minecraft.util.Mth.wrapDegrees(player().getYRot()-turnYaw))<=1){turnYaw=null;turnPhase="COMPLETED";player().stopControlling();}
            else player().applyControlFrame(new dev.mcai.companion.agent.body.AgentControlFrame(turnYaw,player().getXRot(),0,0,false,false,false));
        } else if (jumpActive()) {
            int age = server.getTickCount() - jumpStartedTick;
            jumpSawAirborne |= !player().onGround() && player().getY() > jumpStartY + .05;
            if (!player().isAlive() || age > 60 || jumpSawAirborne && player().onGround()) {
                jumpPhase = jumpSawAirborne && player().isAlive() && player().onGround() ? "COMPLETED" : "FAILED";
                jumpStartedTick = -1;
                player().stopControlling();
            } else {
                player().applyControlFrame(new dev.mcai.companion.agent.body.AgentControlFrame(
                        player().getYRot(), player().getXRot(), 0, 0, age <= 20 && !jumpSawAirborne && player().onGround(), false, false));
            }
        } else if (!placement.ownsBody() && !collection.ownsBody() && !mining.ownsBody() && (navigation.status().phase().terminal() || navigation.status().phase() == NavigationToolCoordinator.Phase.IDLE || navigation.status().phase() == NavigationToolCoordinator.Phase.FOLLOWING)
                && attentionTarget != null) {
            ServerPlayer target = server.getPlayerList().getPlayer(attentionTarget);
            if (target != null && target.isAlive() && target.level() == player().level()
                    && player().distanceToSqr(target) <= 64 && player().hasLineOfSight(target)) {
                var look = target.getEyePosition().subtract(player().getEyePosition());
                player().applyControlFrame(new dev.mcai.companion.agent.body.AgentControlFrame(
                        (float) Math.toDegrees(Math.atan2(-look.x, look.z)),
                        (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))),
                        0, 0, false, false, false));
            }
        }
        if (brain != null) {
            brain.tick();
        }
    }

    public NavigationToolCoordinator navigation() {
        return navigation;
    }

    public MinePilotServerPlayer player() {
        return session.player();
    }

    public boolean externalControlAvailable() {
        return brain == null;
    }

    public static AgentRuntime active(MinecraftServer server) {
        synchronized (ACTIVE) {
            return ACTIVE.get(server);
        }
    }

    public void recordExternalTool(ExternalToolCall call) {
        requireServerThread();
        Objects.requireNonNull(call, "call");
        ExternalToolCall previous = externalToolTrace.peekLast();
        if (previous != null
                && previous.tool().equals(call.tool())
                && Objects.equals(previous.navigationRequestId(), call.navigationRequestId())
                && Objects.equals(previous.outcome(), call.outcome())) {
            return;
        }
        externalToolTrace.addLast(call.withSequence(
                ++externalToolSequence, server.getTickCount()));
        while (externalToolTrace.size() > EXTERNAL_TRACE_LIMIT) {
            externalToolTrace.removeFirst();
        }
    }

    public List<ExternalToolCall> externalToolCallsSince(long afterSequence) {
        requireServerThread();
        return externalToolTrace.stream()
                .filter(call -> call.sequence() > afterSequence)
                .toList();
    }

    public long latestExternalToolSequence() {
        requireServerThread();
        return externalToolSequence;
    }

    public void say(String text) {
        requireServerThread();
        if (closed) {
            throw new IllegalStateException("MinePilot runtime is closed");
        }
        if (text == null || text.isBlank() || text.length() > 512) {
            throw new IllegalArgumentException("Chat message must contain 1 to 512 characters");
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("[AI] " + player().getGameProfile().name()
                        + ": " + text.strip()),
                false
        );
    }

    public List<VisiblePlayerChat> playerChatSince(long afterSequence, int limit) {
        requireServerThread();
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("Chat read limit must be in [1, 50]");
        }
        List<VisiblePlayerChat> result = new ArrayList<>(Math.min(limit, playerChat.size()));
        for (VisiblePlayerChat message : playerChat) {
            if (message.sequence() > afterSequence) {
                result.add(message);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    public long latestChatSequence() {
        requireServerThread();
        return chatSequence;
    }

    private void requireServerThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("MinePilot runtime access must run on the server thread");
        }
    }

    private static String availableName(MinecraftServer server) {
        if (server.getPlayerList().getPlayerByName(DEFAULT_AGENT_NAME) == null) {
            return DEFAULT_AGENT_NAME;
        }
        for (int suffix = 1; suffix <= 999; suffix++) {
            String candidate = "MinePilot" + suffix;
            if (candidate.length() <= 16
                    && server.getPlayerList().getPlayerByName(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("No available MinePilot player name");
    }

    private static BlockPos findSafeSpawn(ServerLevel level) {
        BlockPos center = level.getRespawnData().pos();
        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos feet = new BlockPos(x, y, z);
                    if (isSafe(level, feet)) {
                        return feet;
                    }
                }
            }
        }
        return center.above();
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY() - 1
                || !level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockState floor = level.getBlockState(feet.below());
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        return !floor.getCollisionShape(level, feet.below()).isEmpty()
                && body.getCollisionShape(level, feet).isEmpty()
                && head.getCollisionShape(level, feet.above()).isEmpty()
                && body.getFluidState().isEmpty()
                && head.getFluidState().isEmpty();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (!server.isSameThread()) {
            throw new IllegalStateException("Agent runtime must stop on the server thread");
        }
        closed = true;
        synchronized (ACTIVE) {
            ACTIVE.remove(server, this);
        }
        if (brain != null) {
            brain.close();
        }
        placement.close();
        collection.close();
        mining.cancelForChat();
        navigation.close();
        perception.structures.close();
        session.close();
    }

    public record VisiblePlayerChat(
            long sequence,
            String playerName,
            String text,
            int serverTick,
            boolean handledLocally
    ) {
    }

    public record ExternalToolCall(
            long sequence,
            String tool,
            String navigationRequestId,
            String outcome,
            String targetIdentity,
            Double destinationX,
            Double destinationY,
            Double destinationZ,
            double bodyX,
            double bodyY,
            double bodyZ,
            int serverTick
    ) {
        public ExternalToolCall {
            Objects.requireNonNull(tool, "tool");
        }

        public ExternalToolCall withSequence(long nextSequence, int tick) {
            return new ExternalToolCall(
                    nextSequence, tool, navigationRequestId, outcome,
                    targetIdentity, destinationX, destinationY, destinationZ,
                    bodyX, bodyY, bodyZ, tick);
        }
    }
}
