/*
 * Copyright (C) 2026 Zeraph
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the MIT License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * MIT License for more details.
 *
 * You should have received a copy of the MIT License
 * along with this program.  If not, see <https://opensource.org/license/mit>.
 */

package app.zeraph.origo.fabric.mixin;

import app.zeraph.origo.fabric.OrigoFabric;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"net.minecraft.server.network.ServerConnectionListener$1", "net.minecraft.class_3242$1"}, remap = false)
public abstract class ServerConnectionListenerMixin {

    @Inject(method = "initChannel", at = @At("TAIL"), remap = false)
    private void origo$install(Channel channel, CallbackInfo ci) {
        OrigoFabric.install(channel);
    }
}
