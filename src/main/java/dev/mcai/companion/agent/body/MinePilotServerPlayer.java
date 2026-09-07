package dev.mcai.companion.agent.body;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.UseEffects;

/** A real server-side player body driven by server-authored input frames. */
public final class MinePilotServerPlayer extends ServerPlayer {
    private static final float MAX_YAW_CHANGE_PER_TICK = 18.0F;
    private static final float MAX_PITCH_CHANGE_PER_TICK = 12.0F;

    private volatile AgentControlFrame controlFrame = AgentControlFrame.IDLE;
    public dev.mcai.companion.agent.knowledge.InventoryLedger inventoryLedger;

    public MinePilotServerPlayer(
            MinecraftServer server,
            ServerLevel level,
            GameProfile profile,
            ClientInformation clientInformation
    ) {
        super(server, level, profile, clientInformation);
    }

    public void applyControlFrame(AgentControlFrame next) {
        controlFrame = next == null ? AgentControlFrame.IDLE : next;
    }

    public void stopControlling() {
        controlFrame = new AgentControlFrame(
                getYRot(), getXRot(), 0.0F, 0.0F, false, false, false);
    }

    /**
     * Vanilla Player delegates movement authority to its remote client. This
     * body has no remote client, so server-authored input frames must remain
     * authoritative or LivingEntity.aiStep skips travel entirely.
     */
    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    public float foodExhaustionLevel() {
        return getFoodData().exhaustionLevel;
    }

    @Override
    public void doTick() {
        double beforeX = getX();
        double beforeY = getY();
        double beforeZ = getZ();
        int beforeChunkX = blockPosition().getX() >> 4;
        int beforeChunkZ = blockPosition().getZ() >> 4;

        AgentControlFrame frame = controlFrame;
        float yaw = Mth.approachDegrees(getYRot(), frame.targetYaw(), MAX_YAW_CHANGE_PER_TICK);
        float pitch = Mth.approachDegrees(getXRot(), frame.targetPitch(), MAX_PITCH_CHANGE_PER_TICK);
        setYRot(yaw);
        setYHeadRot(yaw);
        setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        float inputScale = 0.98F * (frame.sneak() || isVisuallyCrawling()
                ? (float) getAttributeValue(Attributes.SNEAKING_SPEED) : 1.0F);
        if (isUsingItem() && !isPassenger()) inputScale *= getUseItem().getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
        xxa = frame.strafe() * inputScale;
        zza = frame.forward() * inputScale;
        jumping = frame.jump();
        setShiftKeyDown(frame.sneak());
        setSprinting(frame.sprint() && frame.forward() >= .8F && !frame.sneak() && getFoodData().getFoodLevel() > 6);

        super.doTick();

        double movedX = getX() - beforeX;
        double movedY = getY() - beforeY;
        double movedZ = getZ() - beforeZ;
        if (movedX != 0.0 || movedY != 0.0 || movedZ != 0.0) {
            checkMovementStatistics(movedX, movedY, movedZ);
        }
        int afterChunkX = blockPosition().getX() >> 4;
        int afterChunkZ = blockPosition().getZ() >> 4;
        if (beforeChunkX != afterChunkX || beforeChunkZ != afterChunkZ) {
            level().getChunkSource().move(this);
        }
    }
}
