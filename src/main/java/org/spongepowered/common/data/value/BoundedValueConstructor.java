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
package org.spongepowered.common.data.value;

import org.spongepowered.api.data.value.BoundedValue;
import org.spongepowered.common.data.copy.CopyHelper;
import org.spongepowered.common.data.key.SpongeBoundedKey;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class BoundedValueConstructor<V extends BoundedValue<E>, E> implements ValueConstructor<V, E> {

    private final SpongeBoundedKey<V, E> key;

    BoundedValueConstructor(SpongeBoundedKey<V, E> key) {
        this.key = key;
    }

    @Override
    public V getMutable(E element) {
        return this.getMutable(element, this.key.getMinimum(), this.key.getMaximum());
    }

    public V getMutable(E element, E minimum, E maximum) {
        return this.getMutable(element, CopyHelper.createSupplier(minimum), CopyHelper.createSupplier(maximum));
    }

    public V getMutable(E element, Supplier<E> minimum, Supplier<E> maximum) {
        return (V) new MutableSpongeBoundedValue<>(this.key, element, minimum, maximum);
    }

    public V getImmutable(E element, E minimum, E maximum) {
        return this.getImmutable(element, CopyHelper.createSupplier(minimum), CopyHelper.createSupplier(maximum));
    }

    public V getImmutable(E element, Supplier<E> minimum, Supplier<E> maximum) {
        return this.getRawImmutable(CopyHelper.copy(element), minimum, maximum);
    }

    @Override
    public V getRawImmutable(E element) {
        return this.getRawImmutable(element, this.key.getMinimum(), this.key.getMaximum());
    }

    public V getRawImmutable(E element, Supplier<E> minimum, Supplier<E> maximum) {
        return (V) new ImmutableSpongeBoundedValue<>(this.key, element, minimum, maximum);
    }
}
