package com.trolmastercard.sexmod.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.GirlSlot;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * Her armour (1.12.2 {@code GirlModel.applyHeldItems} + {@code GirlRenderer.calculateBoneArmorColor}).
 * The dressed models carry armour bones for each body part. A worn piece shows its bones and
 * hides the clothing or skin bones beneath it; an empty slot hides its armour bones.
 * <p>
 * Every material uses the same bones, textured from columns of the girl's texture 72 pixels
 * apart (of 4096): diamond (and anything else) at 0, gold at 1, iron and chainmail at 2,
 * leather at 4, tinted with its dye.
 */
public class ArmorLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    private static final DataTicket<Worn> WORN = DataTicket.create("sexmod_worn_armor", Worn.class);
    private static final float COLUMN = 72.0F / 4096.0F;

    /**
     * A girl model's armour bones per body part, and the bones each one covers (1.12.2
     * {@code IGirlModelInfo}: {@code HeadArmor}/{@code Attachments}, {@code TopArmor}/{@code Top}, ...).
     */
    public record Bones(Part head, Part top, Part bottom, Part shoes) {
        public static final Bones NONE = new Bones(Part.NONE, Part.NONE, Part.NONE, Part.NONE);
    }

    public record Part(List<String> armor, List<String> covered) {
        public static final Part NONE = new Part(List.of(), List.of());
    }

    /** How a worn piece looks: which texture column, and its tint. */
    private record Look(float uOffset, int tint) {
    }

    /** What she wears on each part; null where the slot is empty. */
    private record Worn(@Nullable Look head, @Nullable Look top, @Nullable Look bottom, @Nullable Look shoes) {
    }

    private final Bones bones;

    public ArmorLayer(GeoRenderer<T, Void, LivingEntityRenderState> renderer, Bones bones) {
        super(renderer);
        this.bones = bones;
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        // the nude model has no armour bones; 1.12.2 showed none while she was undressed
        if (!girl.isDressed()) return;
        renderState.addGeckolibData(WORN, new Worn(
            look(girl.getShownEquipment(GirlSlot.HELMET)),
            look(girl.getShownEquipment(GirlSlot.CHEST_PLATE)),
            look(girl.getShownEquipment(GirlSlot.PANTS)),
            look(girl.getShownEquipment(GirlSlot.SHOES))));
    }

    private static @Nullable Look look(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        ResourceKey<EquipmentAsset> asset = equippable == null ? null : equippable.assetId().orElse(null);
        if (asset == EquipmentAssets.GOLD) return new Look(COLUMN, -1);
        if (asset == EquipmentAssets.IRON || asset == EquipmentAssets.CHAINMAIL) return new Look(2 * COLUMN, -1);
        if (asset == EquipmentAssets.LEATHER) return new Look(4 * COLUMN, DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR));
        return new Look(0.0F, -1);
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        Worn worn = pass.renderState().getGeckolibData(WORN);
        // the main pass never draws armour (this layer does), nor what a worn piece covers
        pass.addBoneUpdater((info, snapshots) -> {
            hide(info, snapshots, this.bones.head(), worn != null && worn.head() != null);
            hide(info, snapshots, this.bones.top(), worn != null && worn.top() != null);
            hide(info, snapshots, this.bones.bottom(), worn != null && worn.bottom() != null);
            hide(info, snapshots, this.bones.shoes(), worn != null && worn.shoes() != null);
        });
    }

    private static void hide(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots snapshots, Part part, boolean worn) {
        for (String name : part.armor()) {
            info.model().getBone(name).ifPresent(root -> GeoCubes.forEachBone(root, bone -> snapshots.get(bone).skipRender(true)));
        }
        if (!worn) return;
        for (String name : part.covered()) {
            info.model().getBone(name).ifPresent(root -> GeoCubes.forEachBone(root, bone -> snapshots.get(bone).skipRender(true)));
        }
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        Worn worn = pass.renderState().getGeckolibData(WORN);
        if (!pass.willRender() || worn == null) return;
        addPart(pass, consumer, this.bones.head(), worn.head());
        addPart(pass, consumer, this.bones.top(), worn.top());
        addPart(pass, consumer, this.bones.bottom(), worn.bottom());
        addPart(pass, consumer, this.bones.shoes(), worn.shoes());
    }

    private void addPart(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer, Part part, @Nullable Look look) {
        if (look == null) return;
        for (String name : part.armor()) {
            pass.model().getBone(name).ifPresent(root -> GeoCubes.forEachBone(root, bone -> {
                if (GeoCubes.hasCubes(bone)) consumer.accept(bone, (info, b, tasks) -> renderBone(info, b, tasks, look));
            }));
        }
    }

    private void renderBone(RenderPassInfo<LivingEntityRenderState> pass, GeoBone bone, SubmitNodeCollector renderTasks, Look look) {
        Identifier texture = this.renderer.getTextureLocation(pass.renderState());
        int light = pass.packedLight();
        int overlay = pass.packedOverlay();
        int color = ARGB.multiply(pass.renderColor(), look.tint());
        renderTasks.submitCustomGeometry(pass.poseStack(), RenderTypes.entityCutoutCull(texture), (pose, buffer) -> {
            PoseStack poseStack = pass.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            GeoCubes.renderBone(bone, poseStack, buffer, light, overlay, color, look.uOffset());
            poseStack.popPose();
        });
    }
}
