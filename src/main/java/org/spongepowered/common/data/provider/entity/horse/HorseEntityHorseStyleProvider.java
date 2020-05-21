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
package org.spongepowered.common.data.provider.entity.horse;

import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.util.registry.Registry;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HorseStyle;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.provider.GenericMutableDataProvider;
import org.spongepowered.common.data.type.SpongeHorseStyle;
import org.spongepowered.common.registry.MappedRegistry;

import java.util.Optional;

public class HorseEntityHorseStyleProvider extends GenericMutableDataProvider<HorseEntity, HorseStyle> {

    public HorseEntityHorseStyleProvider() {
        super(Keys.HORSE_STYLE);
    }

    @Override
    protected Optional<HorseStyle> getFrom(HorseEntity dataHolder) {
        final Registry<HorseStyle> registry = SpongeImpl.getRegistry().getCatalogRegistry().getRegistry(HorseStyle.class);
        return Optional.of(((MappedRegistry<HorseStyle, Integer>)registry).getReverseMapping(getHorseStyle(dataHolder)));
    }

    @Override
    protected boolean set(HorseEntity dataHolder, HorseStyle value) {
        final int color = HorseEntityHorseColorProvider.getHorseColor(dataHolder);
        dataHolder.setHorseVariant((color | ((SpongeHorseStyle) value).getBitMask()));
        return true;
    }

    public static int getHorseStyle(HorseEntity dataHolder) {
        return (dataHolder.getHorseVariant() & 0xFF00) >> 8;
    }
}
