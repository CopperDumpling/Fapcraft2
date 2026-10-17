package com.trolmastercard.sexmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.trolmastercard.sexmod.Fapcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Where Galaths and Manglelies have spawned, world-wide (1.12.2 {@code BeeWorldData},
 * "galath_spawn_list"): neither spawns within 700 blocks of any earlier spawn of either.
 */
public class NetherSpawns extends SavedData {
    /** Minimum distance between two of these spawns. */
    public static final double SPACING = 700.0;

    private static final Codec<NetherSpawns> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.listOf().fieldOf("galaths").forGetter(s -> s.galaths),
            BlockPos.CODEC.listOf().fieldOf("manglelies").forGetter(s -> s.manglelies))
        .apply(i, NetherSpawns::new));
    public static final SavedDataType<NetherSpawns> TYPE = new SavedDataType<>(Fapcraft.id("galath_spawn_list"), NetherSpawns::new, CODEC);

    private final List<BlockPos> galaths;
    private final List<BlockPos> manglelies;

    public NetherSpawns() {
        this(List.of(), List.of());
    }

    private NetherSpawns(List<BlockPos> galaths, List<BlockPos> manglelies) {
        this.galaths = new ArrayList<>(galaths);
        this.manglelies = new ArrayList<>(manglelies);
    }

    public static NetherSpawns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Whether a spawn at {@code pos} is at least {@link #SPACING} from every earlier one. */
    public boolean isFree(BlockPos pos) {
        double spacingSqr = SPACING * SPACING;
        for (List<BlockPos> list : List.of(this.galaths, this.manglelies)) {
            for (BlockPos taken : list) {
                if (taken.distSqr(pos) < spacingSqr) return false;
            }
        }
        return true;
    }

    public void addManglelie(BlockPos pos) {
        this.manglelies.add(pos.immutable());
        setDirty();
    }
}
