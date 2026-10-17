package com.trolmastercard.sexmod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Finds where a girl stands beside a bed (1.12.2 {@code BaseGirlEntity.findNearestBed} +
 * {@code JennyEntity.goToSexBed}): the nearest bed block within 22 blocks (±3 vertically), then
 * the free side of it closest to her.
 */
public final class BedFinder {
    private static final int RADIUS = 22;
    private static final int HEIGHT = 3;
    /** Stand position (relative to the bed block) and the free neighbour it needs, per side; 1.12.2's tables. */
    private static final int[] YAWS = {0, 180, -90, 90};
    private static final Vec3[] STAND = {new Vec3(0.5, 0, -0.5), new Vec3(0.5, 0, 1.5), new Vec3(-0.5, 0, 0.5), new Vec3(1.5, 0, 0.5)};
    private static final BlockPos[] NEIGHBOUR = {new BlockPos(0, 0, -1), new BlockPos(0, 0, 1), new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0)};

    public enum Failure { NO_BED, OBSCURED }

    public record Plan(@Nullable Vec3 position, float yaw, @Nullable Failure failure) {
        public boolean ok() {
            return this.failure == null;
        }
    }

    private BedFinder() {
    }

    /** Where Ellie sits on a bed's end, per side (1.12.2 {@code EllieEntity.getRandomSitPose}). */
    private static final Vec3[] SEAT = {new Vec3(0.5, 0, -0.18), new Vec3(0.5, 0, 1.18), new Vec3(-0.18, 0, 0.5), new Vec3(1.18, 0, 0.5)};
    /** Per side: the bed's other half, which the open neighbour ({@link #NEIGHBOUR}) is opposite. */
    private static final BlockPos[] OTHER_HALF = {new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0)};

    /** Ellie's seat on the end of a bed ({@link #planBedEnd}). */
    public static Plan planSeat(Level level, BlockPos from) {
        return planBedEnd(level, from, SEAT);
    }

    /** Bia's spot at the end of a bed, where she lies down along it (1.12.2 {@code BiaEntity.getBedVector}). */
    public static Plan planBedEndStand(Level level, BlockPos from) {
        return planBedEnd(level, from, STAND);
    }

    /**
     * A spot at the end of a bed: on the nearest bed (within 22 blocks, ±3) whose end has open
     * space in front of it with the bed running away from it, the side nearest her. Fails with
     * {@link Failure#NO_BED} only.
     */
    private static Plan planBedEnd(Level level, BlockPos from, Vec3[] spots) {
        List<BlockPos> beds = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    BlockPos pos = from.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(BlockTags.BEDS)) beds.add(pos);
                }
            }
        }
        beds.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));
        for (BlockPos bed : beds) {
            int pick = -1;
            double pickDist = Double.MAX_VALUE;
            for (int i = 0; i < spots.length; i++) {
                if (!level.getBlockState(bed.offset(NEIGHBOUR[i])).isAir()) continue;
                if (!level.getBlockState(bed.offset(OTHER_HALF[i])).is(BlockTags.BEDS)) continue;
                double dist = Vec3.atLowerCornerOf(bed).add(spots[i]).distanceToSqr(Vec3.atLowerCornerOf(from));
                if (dist < pickDist) {
                    pickDist = dist;
                    pick = i;
                }
            }
            if (pick >= 0) return new Plan(Vec3.atLowerCornerOf(bed).add(spots[pick]), YAWS[pick], null);
        }
        return new Plan(null, 0, Failure.NO_BED);
    }

    public static Plan plan(Level level, BlockPos from) {
        BlockPos bed = null;
        double best = Double.MAX_VALUE;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    BlockPos pos = from.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).is(BlockTags.BEDS)) continue;
                    double dist = pos.distSqr(from);
                    if (dist < best) {
                        best = dist;
                        bed = pos;
                    }
                }
            }
        }
        if (bed == null) return new Plan(null, 0, Failure.NO_BED);
        int pick = -1;
        double pickDist = Double.MAX_VALUE;
        for (int i = 0; i < STAND.length; i++) {
            if (!level.getBlockState(bed.offset(NEIGHBOUR[i])).isAir()) continue;
            double dist = Vec3.atLowerCornerOf(bed).add(STAND[i]).distanceToSqr(Vec3.atCenterOf(from));
            if (dist < pickDist) {
                pickDist = dist;
                pick = i;
            }
        }
        if (pick < 0) return new Plan(null, 0, Failure.OBSCURED);
        return new Plan(Vec3.atLowerCornerOf(bed).add(STAND[pick]), YAWS[pick], null);
    }
}
