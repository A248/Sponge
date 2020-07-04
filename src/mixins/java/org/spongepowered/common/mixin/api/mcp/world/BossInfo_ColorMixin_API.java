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
package org.spongepowered.common.mixin.api.mcp.world;

import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.text.format.SpongeTextColor;
import org.spongepowered.plugin.PluginContainer;

@Mixin(BossInfo.Color.class)
public abstract class BossInfo_ColorMixin_API implements BossBarColor {

    @Shadow public abstract String shadow$getName();
    @Shadow public abstract TextFormatting shadow$getFormatting();

    private CatalogKey api$key;
    private SpongeTextColor api$color;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void api$setKeyAndColor(String enumName, int ordinal, String name, TextFormatting formatting, CallbackInfo ci) {
        final PluginContainer container = SpongeImplHooks.getActiveModContainer();
        this.api$key = CatalogKey.of(container, this.shadow$getName().toLowerCase());
        this.api$color = SpongeTextColor.of(this.shadow$getFormatting());
    }

    @Override
    public CatalogKey getKey() {
        return this.api$key;
    }

    @Override
    public TextColor getColor() {
        return this.api$color;
    }
}
