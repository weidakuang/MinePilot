package dev.mcai.companion.agent.body;

public record AgentControlFrame(
        float targetYaw,
        float targetPitch,
        float forward,
        float strafe,
        boolean jump,
        boolean sprint,
        boolean sneak
) {
    public static final AgentControlFrame IDLE = new AgentControlFrame(
            0.0F, 0.0F, 0.0F, 0.0F, false, false, false);

    public AgentControlFrame {
        if (!Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)
                || !Float.isFinite(forward) || !Float.isFinite(strafe)
                || Math.abs(forward) > 1.0F || Math.abs(strafe) > 1.0F) {
            throw new IllegalArgumentException("Invalid control frame");
        }
    }
}
