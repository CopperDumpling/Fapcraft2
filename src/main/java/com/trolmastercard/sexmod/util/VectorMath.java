package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;

/**
 * Rotation helpers from 1.12.2's {@code VectorMath}/{@code TrigMath} that have no vanilla
 * equivalent; scene and renderer offsets depend on their exact conventions.
 * <p>
 * Everything else in those classes maps to vanilla — use it instead:
 * <ul>
 *   <li>{@code scale / dotProduct / crossProduct} → {@link Vec3#scale}, {@link Vec3#dot}, {@link Vec3#cross}</li>
 *   <li>{@code TrigMath.wrapDegrees / toRadians} (both actually converted degrees → radians) → {@link Math#toRadians}</li>
 *   <li>{@code TrigMath.toDegrees / sinDegrees} (the latter actually converted radians → degrees) → {@link Math#toDegrees}</li>
 *   <li>{@code NormalizeAngle / normalizedAngle360} → {@code Mth.positiveModulo(angle, 360)}</li>
 *   <li>{@code calculatePitchAngle(from, to)} → {@code Math.atan2(dz, Math.sqrt(dx*dx + dy*dy))} inline</li>
 * </ul>
 */
public final class VectorMath {
    private VectorMath() {
    }

    /**
     * 1.12.2 {@code rotateByYawPitch}: rotates around X by {@code xAngle} degrees, then around Y
     * using the Minecraft yaw convention (+90° shift on the x/z plane).
     * <p>
     * Note the original parameter names were swapped ({@code yaw} was the X rotation); the
     * arguments keep the original order so ported call sites stay correct.
     */
    public static Vec3 rotateXThenYaw(Vec3 v, float xAngle, float yaw) {
        double xr = Math.toRadians(xAngle);
        double y = v.y * Math.cos(xr) - v.z * Math.sin(xr);
        double z = v.y * Math.sin(xr) + v.z * Math.cos(xr);
        double yawShifted = Math.toRadians(yaw + 90.0F);
        double yr = Math.toRadians(yaw);
        return new Vec3(
            -Math.sin(yawShifted) * v.x - Math.sin(yr) * z,
            y,
            Math.cos(yawShifted) * v.x + Math.cos(yr) * z);
    }

    /** 1.12.2 {@code rotateByYaw}: {@link #rotateXThenYaw} with no X rotation. */
    public static Vec3 rotateByYaw(Vec3 v, float yaw) {
        return rotateXThenYaw(v, 0.0F, yaw);
    }

    /**
     * 1.12.2 {@code rotateByEuler}: rotates around X, then Y, then Z, by angles in degrees.
     * (The original converted to radians and then truncated sin/cos to float precision;
     * that rounding is kept so results match.)
     */
    public static Vec3 rotateXYZ(Vec3 v, float xDeg, float yDeg, float zDeg) {
        double sx = (float) Math.sin(Math.toRadians(xDeg));
        double cx = (float) Math.cos(Math.toRadians(xDeg));
        double sy = (float) Math.sin(Math.toRadians(yDeg));
        double cy = (float) Math.cos(Math.toRadians(yDeg));
        double sz = (float) Math.sin(Math.toRadians(zDeg));
        double cz = (float) Math.cos(Math.toRadians(zDeg));
        double y = v.y * cx - v.z * sx;
        double z = v.y * sx + v.z * cx;
        double x = v.x * cy + z * sy;
        z = -v.x * sy + z * cy;
        return new Vec3(x * cz - y * sz, x * sz + y * cz, z);
    }

    /** Negates x and z (mirror through the vertical axis). */
    public static Vec3 mirrorXZ(Vec3 v) {
        return new Vec3(-v.x, v.y, -v.z);
    }

    /** Negates x and y. */
    public static Vec3 mirrorXY(Vec3 v) {
        return new Vec3(-v.x, -v.y, v.z);
    }

    /** Negates y and z. */
    public static Vec3 mirrorYZ(Vec3 v) {
        return new Vec3(v.x, -v.y, -v.z);
    }

    /**
     * How far {@code value} lies between {@code start} and {@code end}, measured on the X axis
     * only (as in 1.12.2). NaN when {@code start.x == end.x}.
     */
    public static double linearFactorX(Vec3 start, Vec3 end, Vec3 value) {
        return (value.x - start.x) / (end.x - start.x);
    }
}
