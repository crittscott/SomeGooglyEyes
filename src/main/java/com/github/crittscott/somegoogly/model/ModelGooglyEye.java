package com.github.crittscott.somegoogly.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

public class ModelGooglyEye extends Model {
    private final ModelPart cornea1;
    private final ModelPart cornea2;
    private final ModelPart cornea3;
    private final ModelPart cornea4;
    private final ModelPart cornea5;
    private final ModelPart cornea6;
    private final ModelPart[] iris = new ModelPart[3];

    public ModelGooglyEye(ModelPart root) {
        super(RenderType::entityCutout);

        this.cornea1 = root.getChild("cornea1");
        this.cornea2 = root.getChild("cornea2");
        this.cornea3 = root.getChild("cornea3");
        this.cornea4 = root.getChild("cornea4");
        this.cornea5 = root.getChild("cornea5");
        this.cornea6 = root.getChild("cornea6");

        this.iris[0] = root.getChild("iris1");
        this.iris[1] = root.getChild("iris2");
        this.iris[2] = root.getChild("iris3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("cornea1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -1.865F, -1.03F, 1, 3.73F, 1), PartPose.ZERO);

        partdefinition.addOrReplaceChild("cornea2", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -1.865F, -1.0F, 1, 3.73F, 1),
                PartPose.rotation(0.0F, 0.0F, 0.5235987755982988F));

        partdefinition.addOrReplaceChild("cornea3", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -1.865F, -1.02F, 1, 3.73F, 1),
                PartPose.rotation(0.0F, 0.0F, 1.0471975511965976F));

        partdefinition.addOrReplaceChild("cornea4", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -1.865F, -0.99F, 1, 3.73F, 1),
                PartPose.rotation(0.0F, 0.0F, 1.5707963267948966F));

        partdefinition.addOrReplaceChild("cornea5", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -1.865F, -1.01F, 1, 3.73F, 1),
                PartPose.rotation(0.0F, 0.0F, 2.0943951023931953F));

        partdefinition.addOrReplaceChild("cornea6", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -1.865F, -0.98F, 1, 3.73F, 1),
                PartPose.rotation(0.0F, 0.0F, 2.6179938779914944F));

        partdefinition.addOrReplaceChild("iris1", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -0.8665F, -1.51F, 1, 1.733F, 1),
                PartPose.rotation(0.0F, 0.0F, 2.0943951023931953F));

        partdefinition.addOrReplaceChild("iris2", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -0.8665F, -1.5F, 1, 1.733F, 1),
                PartPose.rotation(0.0F, 0.0F, 1.0471975511965976F));

        partdefinition.addOrReplaceChild("iris3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -0.8665F, -1.49F, 1, 1.733F, 1), PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Empty - we render parts manually
    }

    public void renderCornea(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        cornea1.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        cornea2.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        cornea3.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        cornea4.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        cornea5.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        cornea6.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderIris(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        for (int i = 0; i < iris.length; i++) {
            iris[i].render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    public void moveIris(float x, float y, float pupilSize) {
        float shiftFactor = (1.45F - pupilSize * 0.525F) / pupilSize;
        float rotX = -x * shiftFactor;
        float rotY = -y * shiftFactor * (float)Math.cos(Math.toRadians(x * 90F));

        for (int i = 0; i < iris.length; i++) {
            iris[i].x = rotX;
            iris[i].y = rotY;
        }
    }
}