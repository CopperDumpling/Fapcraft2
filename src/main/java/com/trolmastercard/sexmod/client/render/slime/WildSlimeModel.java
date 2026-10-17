package com.trolmastercard.sexmod.client.render.slime;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;

/**
 * The wild slime (1.12.2 {@code GoblinModel}, which only the wild slime used): a tiny slime's
 * body, eyes and mouth with a four-piece tail, on the vanilla slime texture. Static.
 */
public class WildSlimeModel extends EntityModel<SlimeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Fapcraft.id("friendly_slime"), "main");

    public WildSlimeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-3.0F, 17.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(1.3F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 4).mirror().addBox(-3.3F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-1.0F, 21.0F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(-0.5F, 0.0F, 0.1F));
        tail.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(10, 11).addBox(-2.5F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, 20.7406F, 4.0504F, 1.0908F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(10, 11).addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, 19.9214F, 3.4768F, 0.6109F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(10, 11).addBox(-4.0F, 0.0F, 0.075F, 5.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, 19.0074F, 3.0643F, 0.3491F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(10, 11).addBox(-3.0F, -1.0F, -0.5F, 7.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 17.925F, 3.5F, 0.1309F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
