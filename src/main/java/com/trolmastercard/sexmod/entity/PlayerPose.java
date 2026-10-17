package com.trolmastercard.sexmod.entity;

/**
 * Where a scene puts its partner, relative to where the partner stood when the first pose was
 * applied, in the scene's frame (1.12.2 {@code positionPlayerRelative}).
 *
 * @param right   blocks to the right (1.12.2 {@code dx})
 * @param up      blocks up
 * @param forward blocks forward
 * @param yaw     degrees added to the scene yaw
 * @param pitch   absolute pitch
 */
public record PlayerPose(double right, double up, double forward, float yaw, float pitch) {
}
