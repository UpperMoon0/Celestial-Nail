package com.nstut.celestialnail.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
/** Height-independent shader; horizontal fading is supplied per entity, terrain retains vanilla fog. */
public abstract class CelestialNailSkyRender extends RenderType {
 private static ShaderInstance shader;
 private CelestialNailSkyRender() {super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
 public static void reload(ResourceProvider provider) throws java.io.IOException {
  ShaderInstance next=new ShaderInstance(provider,"celestial_nail_sky",DefaultVertexFormat.POSITION_TEX_COLOR);
  if(shader!=null)shader.close();shader=next;
 }
 public static final RenderType TYPE=create("celestial_nail_sky",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,262144,false,true,
  CompositeState.builder().setShaderState(new ShaderStateShard(()->shader))
   .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("celestial_nail","textures/entity/celestial_nail.png"),false,false))
   .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false));
}
