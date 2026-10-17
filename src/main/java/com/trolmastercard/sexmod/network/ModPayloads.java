package com.trolmastercard.sexmod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers every custom payload (the 1.12.2 mod had 49 packets; ported as features need them). */
public final class ModPayloads {
    /** Bump whenever a payload's wire format changes. */
    public static final String PROTOCOL = "1";

    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(ChooseActionPayload.TYPE, ChooseActionPayload.CODEC, ServerSceneHandlers::chooseAction);
        registrar.playToServer(SceneStepPayload.TYPE, SceneStepPayload.CODEC, ServerSceneHandlers::step);
        registrar.playToServer(ScenePosePayload.TYPE, ScenePosePayload.CODEC, ServerSceneHandlers::pose);
        registrar.playToServer(SceneChatPayload.TYPE, SceneChatPayload.CODEC, ServerSceneHandlers::chat);
        registrar.playToServer(LeaveScenePayload.TYPE, LeaveScenePayload.CODEC, ServerSceneHandlers::leave);
    }
}
