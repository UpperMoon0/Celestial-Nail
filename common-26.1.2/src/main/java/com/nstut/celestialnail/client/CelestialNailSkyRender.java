package com.nstut.celestialnail.client;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import com.nstut.celestialnail.client.mixin.RenderTypeInvoker;
public final class CelestialNailSkyRender {
 private static final RenderPipeline PIPELINE=RenderPipeline.builder()
  .withLocation("celestial_nail:pipeline/sky").withVertexShader("celestial_nail:core/sky").withFragmentShader("celestial_nail:core/sky")
  .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER).withUniform("Projection",UniformType.UNIFORM_BUFFER).withSampler("Sampler0")
  .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS).withCull(false)
  .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withDepthStencilState(DepthStencilState.DEFAULT).build();
 public static final RenderType TYPE=RenderTypeInvoker.celestial$create("celestial_nail_sky",RenderSetup.builder(PIPELINE)
  .withTexture("Sampler0",Identifier.fromNamespaceAndPath("celestial_nail","textures/entity/celestial_nail.png"))
  .sortOnUpload().bufferSize(262144).createRenderSetup());
 private CelestialNailSkyRender() {}
}
