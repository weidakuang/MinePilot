package dev.mcai.companion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import dev.mcai.companion.agent.body.HeadlessPlayerSession;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve the companion subclass; all respawn state/rules still belong to vanilla. */
@Mixin(value=PlayerList.class,remap=false)
public abstract class CompanionRespawnMixin {
    @WrapOperation(method="respawn",at=@At(value="NEW",target="(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"),remap=false)
    private ServerPlayer minepilot$body(MinecraftServer server, ServerLevel level, GameProfile profile,
                                      ClientInformation information, Operation<ServerPlayer> original) {
        return HeadlessPlayerSession.respawning(profile.id())
                ? new MinePilotServerPlayer(server,level,profile,information)
                : original.call(server,level,profile,information);
    }
}
