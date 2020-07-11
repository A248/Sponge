/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.tracker.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.chunk.AbstractChunkProviderBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;

import java.util.Optional;
import java.util.UUID;

@Mixin(value = net.minecraft.world.World.class, priority = 1111)
public abstract class WorldMixin_OwnershipTracked implements World {

    @Shadow public abstract AbstractChunkProvider shadow$getChunkProvider();

    @Override
    public Optional<UUID> getCreator(int x, int y, int z) {
        final Chunk chunk = ((AbstractChunkProviderBridge) this.shadow$getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(x >> 4, z >> 4);
        if (chunk == null) {
            return Optional.empty();
        }

        final BlockPos pos = new BlockPos(x, y, z);
        // The difference here saves the user lookup check for snapshot creation, very hot when considering
        // blocks changing with potentially n block notifiers and n block owners.
        return ((ChunkBridge) chunk).bridge$getBlockCreatorUUID(pos);
    }

    @Override
    public Optional<UUID> getNotifier(int x, int y, int z) {
        final Chunk chunk = ((AbstractChunkProviderBridge) this.shadow$getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(x >> 4, z >> 4);
        if (chunk == null) {
            return Optional.empty();
        }

        final BlockPos pos = new BlockPos(x, y, z);
        // The difference here saves the user lookup check for snapshot creation, very hot when considering
        // blocks changing with potentially n block notifiers and n block owners.
        return ((ChunkBridge) chunk).bridge$getBlockNotifierUUID(pos);
    }

    @Override
    public void setCreator(int x, int y, int z, @Nullable UUID uuid) {
        final Chunk chunk = ((AbstractChunkProviderBridge) this.shadow$getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        final BlockPos pos = new BlockPos(x, y, z);
        ((ChunkBridge) chunk).bridge$setBlockCreator(pos, uuid);
    }

    @Override
    public void setNotifier(int x, int y, int z, @Nullable UUID uuid) {
        final Chunk chunk = ((AbstractChunkProviderBridge) this.shadow$getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        final BlockPos pos = new BlockPos(x, y, z);
        ((ChunkBridge) chunk).bridge$setBlockNotifier(pos, uuid);
    }

    /**
     * Gets the {@link ServerLocation} of the spawn point.
     *
     * @return The location
     */
    public ServerLocation getSpawnLocation() {
        return ServerLocation.of(this, this.getProperties().getSpawnPosition());
    }
}
