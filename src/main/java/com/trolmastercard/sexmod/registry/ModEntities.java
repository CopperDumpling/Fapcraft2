package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.bia.BiaEntity;
import com.trolmastercard.sexmod.entity.ellie.EllieEntity;
import com.trolmastercard.sexmod.entity.jenny.JennyEntity;
import com.trolmastercard.sexmod.entity.slime.SlimeEntity;
import com.trolmastercard.sexmod.entity.slime.WildSlimeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types: girls, player variants, scene entities, eggs/projectiles (Phase 2 onward). */
public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Fapcraft.MODID);

    // 1.12.2: 0.49 x 1.95, eye height 1.64, tracked within 50 blocks
    public static final DeferredHolder<EntityType<?>, EntityType<JennyEntity>> JENNY = ENTITIES.registerEntityType(
        "jenny", JennyEntity::new, MobCategory.CREATURE, b -> b.sized(0.49F, 1.95F).eyeHeight(1.64F).clientTrackingRange(4));
    // 1.12.2: the default 0.6 x 1.8, eye height 1.9 (1.53 crouched, see EllieEntity)
    public static final DeferredHolder<EntityType<?>, EntityType<EllieEntity>> ELLIE = ENTITIES.registerEntityType(
        "ellie", EllieEntity::new, MobCategory.CREATURE, b -> b.sized(0.6F, 1.8F).eyeHeight(1.9F).clientTrackingRange(4));
    // 1.12.2: 0.49 x 1.65, the default eye height (0.85 of her height)
    public static final DeferredHolder<EntityType<?>, EntityType<BiaEntity>> BIA = ENTITIES.registerEntityType(
        "bia", BiaEntity::new, MobCategory.CREATURE, b -> b.sized(0.49F, 1.65F).clientTrackingRange(4));
    // 1.12.2: the default 0.6 x 1.8; spawns in swamps (biome modifier slime_spawns)
    public static final DeferredHolder<EntityType<?>, EntityType<SlimeEntity>> SLIME = ENTITIES.registerEntityType(
        "slime", SlimeEntity::new, MobCategory.CREATURE, b -> b.sized(0.6F, 1.8F).clientTrackingRange(4));
    // 1.12.2 friendly_slime: a vanilla-sized slime cube (0.51 per size)
    public static final DeferredHolder<EntityType<?>, EntityType<WildSlimeEntity>> WILD_SLIME = ENTITIES.registerEntityType(
        "friendly_slime", WildSlimeEntity::new, MobCategory.CREATURE, b -> b.sized(0.52F, 0.52F).eyeHeight(0.325F).clientTrackingRange(10));

    private ModEntities() {
    }

    @EventBusSubscriber(modid = Fapcraft.MODID)
    static final class Attributes {
        @SubscribeEvent
        static void onCreateAttributes(EntityAttributeCreationEvent event) {
            event.put(JENNY.get(), JennyEntity.createAttributes().build());
            event.put(ELLIE.get(), EllieEntity.createAttributes().build());
            event.put(BIA.get(), BiaEntity.createAttributes().build());
            event.put(SLIME.get(), SlimeEntity.createAttributes().build());
            event.put(WILD_SLIME.get(), Monster.createMonsterAttributes().build());
        }

        @SubscribeEvent
        static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            event.register(SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
