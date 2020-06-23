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
package org.spongepowered.common.world.teleport;

import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.category.TeleportHelperCategory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class ConfigTeleportHelperFilter implements TeleportHelperFilter {

    // We try to cache this in case of big mod blacklists, we don't want to parse this
    // all the time.
    @Nullable private static List<BlockType> floorBlockTypes = null;
    @Nullable private static List<BlockState> floorBlockStates = null;
    @Nullable private static List<BlockType> bodyBlockTypes = null;
    @Nullable private static List<BlockState> bodyBlockStates = null;

    public static void invalidateCache() {
        floorBlockTypes = null;
        floorBlockStates = null;
        bodyBlockStates = null;
        bodyBlockTypes = null;
    }

    private static void updateCacheIfNecessary() {
        if (floorBlockTypes == null) {
            TeleportHelperCategory teleportHelperCat = SpongeImpl.getGlobalConfigAdapter().getConfig().getTeleportHelper();
            floorBlockTypes = teleportHelperCat.getUnsafeFloorBlockIds().stream()
                    .map(x -> CatalogKey.resolve(x.toLowerCase(Locale.ENGLISH)))
                    .map(x -> Sponge.getRegistry().getCatalogRegistry().get(BlockType.class, x).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            floorBlockStates = teleportHelperCat.getUnsafeFloorBlockIds().stream()
                    .map(x -> CatalogKey.resolve(x.toLowerCase(Locale.ENGLISH)))
                    .map(x -> Sponge.getRegistry().getCatalogRegistry().get(BlockState.class, x).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            bodyBlockTypes = teleportHelperCat.getUnsafeBodyBlockIds().stream()
                    .map(x -> CatalogKey.resolve(x.toLowerCase(Locale.ENGLISH)))
                    .map(x -> Sponge.getRegistry().getCatalogRegistry().get(BlockType.class, x).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            bodyBlockStates = teleportHelperCat.getUnsafeBodyBlockIds().stream()
                    .map(x -> CatalogKey.resolve(x.toLowerCase(Locale.ENGLISH)))
                    .map(x -> Sponge.getRegistry().getCatalogRegistry().get(BlockState.class, x).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private final CatalogKey key;

    public ConfigTeleportHelperFilter() {
        this.key = CatalogKey.sponge("config");
    }

    @Override
    public CatalogKey getKey() {
        return this.key;
    }

    @Override
    public boolean isSafeFloorMaterial(BlockState blockState) {
        updateCacheIfNecessary();
        return !floorBlockStates.contains(blockState) && !floorBlockTypes.contains(blockState.getType());
    }

    @Override
    public boolean isSafeBodyMaterial(BlockState blockState) {
        updateCacheIfNecessary();
        return !bodyBlockStates.contains(blockState) && !bodyBlockTypes.contains(blockState.getType());
    }
}