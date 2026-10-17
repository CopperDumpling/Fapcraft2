package com.trolmastercard.sexmod.sound;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

/**
 * The numbered variants of one sound, e.g. {@code girls.jenny.ahh} =
 * {@code girls.jenny.ahh.ahh0 .. ahh9}. {@link #random} avoids playing the same
 * variant twice in a row (as 1.12.2's {@code SoundHandler.randomSound} did).
 */
public final class SoundGroup {
    private final String name;
    private final int[] numbers;
    private final List<Holder<SoundEvent>> variants;
    private final AtomicInteger last = new AtomicInteger(-1);

    SoundGroup(String name, int[] numbers, List<Holder<SoundEvent>> variants) {
        this.name = name;
        this.numbers = numbers;
        this.variants = variants;
    }

    /** The variant with this number (the {@code n} in {@code ahh<n>}), as 1.12.2's {@code AHH[n]}. */
    public Holder<SoundEvent> variant(int number) {
        for (int i = 0; i < this.numbers.length; i++) {
            if (this.numbers[i] == number) return this.variants.get(i);
        }
        throw new IllegalArgumentException(this.name + " has no variant " + number);
    }

    public String name() {
        return name;
    }

    public List<Holder<SoundEvent>> variants() {
        return variants;
    }

    public Holder<SoundEvent> random(RandomSource random) {
        int size = variants.size();
        if (size == 1) return variants.get(0);
        int index = random.nextInt(size - 1);
        int previous = last.get();
        if (previous >= 0 && index >= previous) index++; // never the previous variant
        last.set(index);
        return variants.get(index);
    }
}
