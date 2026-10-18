package com.trolmastercard.sexmod.client.render;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.render.slime.WildSlimeModel;
import com.trolmastercard.sexmod.client.render.slime.WildSlimeRenderer;
import com.trolmastercard.sexmod.client.render.luna.LunaHookRenderer;
import com.trolmastercard.sexmod.client.render.luna.LunaRodCast;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
import com.trolmastercard.sexmod.entity.ellie.EllieEntity;
import com.trolmastercard.sexmod.entity.luna.LunaEntity;
import com.trolmastercard.sexmod.entity.manglelie.ManglelieEntity;
import com.trolmastercard.sexmod.entity.slime.SlimeEntity;
import com.trolmastercard.sexmod.registry.ModEntities;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class ModRenderers {
    /** 1.12.2 {@code JennyEntity} constructor values. */
    private static final HeldWeaponLayer.WeaponPose JENNY_WEAPON = new HeldWeaponLayer.WeaponPose(140.0F, 50.0F, new Vector3f(0.0F, -0.03F, -0.2F), 140.0F);
    /** 1.12.2 {@code JennyNpcModel}. */
    private static final ArmorLayer.Bones JENNY_ARMOR = new ArmorLayer.Bones(
        new ArmorLayer.Part(List.of("armorHelmet"), List.of()),
        new ArmorLayer.Part(List.of("armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs"), List.of("boobsFlesh", "upperBodyL", "upperBodyR")),
        new ArmorLayer.Part(List.of("armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"),
            List.of("fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR")),
        new ArmorLayer.Part(List.of("armorShoesL", "armorShoesR"), List.of()));

    /** 1.12.2 {@code EllieEntity} constructor values. */
    private static final HeldWeaponLayer.WeaponPose ELLIE_WEAPON = new HeldWeaponLayer.WeaponPose(-85.0F, -175.0F, new Vector3f(-0.1F, 0.05F, 0.0F), -85.0F);
    /** 1.12.2 {@code EllieNpcModel}: as Jenny, plus her headband under a helmet and her hotpants and slip under leggings. */
    private static final ArmorLayer.Bones ELLIE_ARMOR = new ArmorLayer.Bones(
        new ArmorLayer.Part(List.of("armorHelmet"), List.of("headband")),
        new ArmorLayer.Part(List.of("armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs"), List.of("boobsFlesh", "upperBodyL", "upperBodyR")),
        new ArmorLayer.Part(List.of("armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"),
            List.of("fleshL", "fleshR", "vagina", "hotpants", "slip", "curvesL", "curvesR", "kneeL", "kneeR")),
        new ArmorLayer.Part(List.of("armorShoesL", "armorShoesR"), List.of()));

    /** 1.12.2 {@code BiaNpcModel}: her leaves under a helmet, bra and slip under armour. Her weapon grip is Jenny's. */
    private static final ArmorLayer.Bones BIA_ARMOR = new ArmorLayer.Bones(
        new ArmorLayer.Part(List.of("armorHelmet"), List.of("leaf7", "leaf8")),
        new ArmorLayer.Part(List.of("armorChest", "armorBoobs", "armorShoulderR", "armorShoulderL"), List.of("bra", "upperBodyR", "upperBodyL")),
        new ArmorLayer.Part(List.of("armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"),
            List.of("slip", "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR")),
        new ArmorLayer.Part(List.of("armorShoesL", "armorShoesR"), List.of()));

    /** 1.12.2 {@code SlimeNpcModel}: her blob under a helmet, her cloth under a chestplate. She never wears armour or holds weapons. */
    private static final ArmorLayer.Bones SLIME_ARMOR = new ArmorLayer.Bones(
        new ArmorLayer.Part(List.of("armorHelmet"), List.of("bigblob")),
        new ArmorLayer.Part(List.of("armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs"), List.of("boobsFlesh", "upperBodyL", "upperBodyR", "cloth")),
        new ArmorLayer.Part(List.of("armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"),
            List.of("fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR")),
        new ArmorLayer.Part(List.of("armorShoesL", "armorShoesR"), List.of()));

    /** 1.12.2 {@code LunaEntity} constructor values. */
    private static final HeldWeaponLayer.WeaponPose LUNA_WEAPON = new HeldWeaponLayer.WeaponPose(230.0F, 150.0F, new Vector3f(0.0F, -0.06F, 0.1F), 320.0F);
    /** 1.12.2 {@code CatNpcModel}: her cloth under a chestplate or leggings. */
    private static final ArmorLayer.Bones LUNA_ARMOR = new ArmorLayer.Bones(
        new ArmorLayer.Part(List.of("armorHelmet"), List.of()),
        new ArmorLayer.Part(List.of("armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs"), List.of("boobsFlesh", "cloth")),
        new ArmorLayer.Part(List.of("armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"),
            List.of("fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR", "cloth")),
        new ArmorLayer.Part(List.of("armorShoesL", "armorShoesR"), List.of()));

    private ModRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JENNY.get(), ctx -> new GirlRenderer<>(ctx, new GirlModel<>("jenny"), JENNY_WEAPON, JENNY_ARMOR));
        event.registerEntityRenderer(ModEntities.ELLIE.get(), ctx -> new GirlRenderer<EllieEntity>(ctx, new GirlModel<>("ellie", "dressed", "nude"), ELLIE_WEAPON, ELLIE_ARMOR)
            .withRenderLayer(EllieHeadLayer::new));
        event.registerEntityRenderer(ModEntities.BIA.get(), ctx -> new GirlRenderer<>(ctx, new GirlModel<>("bia"), JENNY_WEAPON, BIA_ARMOR));
        // 1.12.2 dressed the slime in her "armored" model (outfit 1); "dressed" was never used
        event.registerEntityRenderer(ModEntities.SLIME.get(), ctx -> new GirlRenderer<SlimeEntity>(ctx, new GirlModel<>("slime", "armored", "nude"), JENNY_WEAPON, SLIME_ARMOR)
            .withRenderLayer(SlimeLayer::new));
        event.registerEntityRenderer(ModEntities.WILD_SLIME.get(), WildSlimeRenderer::new);
        // one model for every outfit
        event.registerEntityRenderer(ModEntities.LUNA.get(), ctx -> new GirlRenderer<LunaEntity>(ctx, new GirlModel<>("cat", "cat", "cat"), LUNA_WEAPON, LUNA_ARMOR)
            .withRenderLayer(renderer -> new LunaLayer(ctx, renderer)));
        event.registerEntityRenderer(ModEntities.LUNA_HOOK.get(), LunaHookRenderer::new);
        // one model for every outfit; she never wears armour or holds weapons
        event.registerEntityRenderer(ModEntities.BEE.get(), ctx -> new GirlRenderer<BeeEntity>(ctx,
            new GirlModel<BeeEntity>("bee", "bee", "bee").withHeadRestPitch(1.0F), JENNY_WEAPON, ArmorLayer.Bones.NONE).withRenderLayer(BeeLayer::new));
        // one model for every outfit; she never wears armour or holds weapons
        event.registerEntityRenderer(ModEntities.MANGLELIE.get(), ctx -> new GirlRenderer<ManglelieEntity>(ctx,
            new GirlModel<>("manglelie", "manglelie", "manglelie"), JENNY_WEAPON, ArmorLayer.Bones.NONE).withRenderLayer(ManglelieLayer::new));
    }

    @SubscribeEvent
    static void onRegisterItemProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(Fapcraft.id("luna_cast"), LunaRodCast.MAP_CODEC);
    }

    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WildSlimeModel.LAYER, WildSlimeModel::createLayer);
    }
}
