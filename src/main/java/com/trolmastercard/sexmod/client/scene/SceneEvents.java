package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.network.SceneChatPayload;
import com.trolmastercard.sexmod.sound.SoundGroup;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * What every girl's animation keyframe events do: sounds play on every client that renders her;
 * dialogue is sent once, by the partner's client, and broadcast by the server.
 */
public final class SceneEvents {
    private SceneEvents() {
    }

    /** Events every girl's companion animations share; true if {@code event} was one. */
    static boolean handleShared(GirlEntity girl, String event) {
        if ("attackSound".equals(event)) {
            girl.level().playLocalSound(girl, SoundEvents.PLAYER_ATTACK_STRONG, girl.getSoundSource(), 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    static void play(GirlEntity girl, SoundGroup group) {
        play(girl, group.random(girl.getRandom()), 1.0F);
    }

    static void play(GirlEntity girl, SoundGroup group, float volume) {
        play(girl, group.random(girl.getRandom()), volume);
    }

    static void play(GirlEntity girl, Holder<SoundEvent> sound) {
        play(girl, sound, 1.0F);
    }

    static void play(GirlEntity girl, Holder<SoundEvent> sound, float volume) {
        girl.level().playLocalSound(girl, sound.value(), girl.getSoundSource(), volume, 1.0F);
    }

    /** "{@code <Girl> ...}" to everyone nearby, sent once by the partner's client. */
    static void say(GirlEntity girl, boolean partner, String line) {
        if (partner) ClientPacketDistributor.sendToServer(new SceneChatPayload(girl.getId(), girl.dialoguePrefix() + line, false));
    }

    /** "{@code <partner> ...}": the lines 1.12.2 had the player say. */
    static void sayAsPartner(GirlEntity girl, boolean partner, String line) {
        if (partner) ClientPacketDistributor.sendToServer(new SceneChatPayload(girl.getId(), girl.dialoguePrefix() + line, true));
    }
}
