package com.nstut.celestialnail.compattest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.nstut.celestialnail.client.CelestialNailAtmosphere;
import com.nstut.celestialnail.compat.SodiumExtrasTestHooks;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** A packaged client, integrated-server tracking, actual camera culling and final framebuffer RGB. */
public final class SodiumExtrasClientFixture implements SodiumExtrasTestHooks.Driver {
 private static final long CLOCK=100000;
 private static final int GROUND_RADIUS=192;
 private record Scene(String name,double x,double y,double z,boolean offscreen,boolean suppress) {}
 private static final List<Scene> SCENES=List.of(
  new Scene("horizontal-cutoff",0,180,-150,false,false),
  new Scene("vertical-cutoff",0,115,-40,false,false),
  new Scene("angle-left",-100,180,-100,false,false),
  new Scene("angle-right",100,180,-100,false,false),
  new Scene("offscreen-control",0,180,-150,true,false),
  new Scene("missing-draw-control",0,180,-150,false,true),
  new Scene("grounded-horizontal-cutoff",0,260,-150,false,false),
  new Scene("grounded-vertical-cutoff",0,285,-40,false,false),
  new Scene("grounded-angle-left",-100,260,-100,false,false),
  new Scene("grounded-angle-right",100,260,-100,false,false),
  new Scene("grounded-offscreen-control",0,260,-150,true,false),
  new Scene("grounded-missing-draw-control",0,260,-150,false,true),
  new Scene("grounded-terrain-occluded-control",0,360,-150,false,false));
 private final Minecraft mc=Minecraft.getInstance();
 private final Map<String,Object> report=new LinkedHashMap<>();
 private final List<Map<String,Object>> cases=new ArrayList<>();
 private final Path output=mc.gameDirectory.toPath().resolve("evidence");
 private final long started=System.nanoTime();
 private boolean requested,done,groundedAssertionsRun,groundReady;
 private int index,stage,frames,entityId;
 private Scene scene;
 private float yaw,pitch;
 private CompletableFuture<Void> setup;
 private NativeImage hiddenA,hiddenB;
 private long drawStart;

 public SodiumExtrasClientFixture() {
  if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().startsWith(
     Path.of(System.getProperty("celestial_nail.compatRunRoot")).toAbsolutePath().normalize()))
   throw new IllegalStateException("Compatibility fixture requires an isolated game directory");
  try {Files.createDirectories(output);} catch(Exception e) {throw new IllegalStateException(e);}
 }
 @Override public void tick() {
  if(done) return;
  if(System.nanoTime()-started>600L*1000000000L) {finish(new AssertionError("Client timeout"));return;}
  try {
   if(!requested && mc.getOverlay()==null) {
    requested=true;
    SodiumExtrasAssertions.run(mc,report);
    mc.options.onboardAccessibility=false;
    mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
    mc.options.bobView().set(false);mc.options.enableVsync().set(false);
    mc.options.framerateLimit().set(60);mc.options.fov().set(70);
    mc.options.renderDistance().set(12);mc.options.simulationDistance().set(6);
    mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);
    String name="compat-fixture-"+UUID.randomUUID();
    var settings=new LevelSettings(name,GameType.SPECTATOR,false,Difficulty.PEACEFUL,true,
      new GameRules(),WorldDataConfiguration.DEFAULT);
    CompatWorldStartup.create(mc,name,settings);
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null) return;
   if(mc.screen!=null) mc.setScreen(null);
   if(setup==null) startScene();
  } catch(Throwable e) {finish(e);}
 }
 private boolean grounded() { return scene.name.startsWith("grounded-"); }
 private void startScene() {
  if(index==SCENES.size()) {finish(null);return;}
  scene=SCENES.get(index);LogUtils.getLogger().info("[NailCompatTest] Scene {}",scene.name);stage=0;frames=0;SodiumExtrasTestHooks.hideNail=true;
  yaw=(float)Math.toDegrees(-Math.atan2(-scene.x,-scene.z))+(scene.offscreen?180:0);
  pitch=(float)Math.toDegrees(Math.atan2(scene.y-(grounded()?259:236),Math.hypot(scene.x,scene.z)));
  var server=mc.getSingleplayerServer();var playerId=mc.player.getUUID();
  setup=CompletableFuture.runAsync(()->{
   var level=server.overworld();var player=server.getPlayerList().getPlayer(playerId);
   if(player==null) throw new IllegalStateException("Missing server player");
   var old=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(e instanceof CelestialNailEntity)old.add(e);});old.forEach(Entity::discard);
   level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
   level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
   level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
   level.setDayTime(6000);server.getWorldData().overworldData().setGameTime(CLOCK);
   level.setWeatherParameters(0,100000,false,false);
   player.setGameMode(GameType.SPECTATOR);
   player.teleportTo(level,scene.x,scene.y-player.getEyeHeight(),scene.z,yaw,pitch);
   var nail=SodiumExtrasAssertions.nailType().create(level);
   if(nail==null) throw new IllegalStateException("Nail factory failed");
   nail.setPos(0,200,0);nail.configure("compat_fixture_"+index,grounded()?32:4);nail.beginSummoning(1);
   var tag=new CompoundTag();nail.saveWithoutId(tag);
   tag.putLong("SummonTime",CLOCK-(grounded()?3000:300));tag.putLong("LaunchTime",-1);tag.putLong("ImpactTime",-1);tag.putLong("CrumbleTime",-1);
   if(grounded()) {
    tag.putByte("Phase",(byte)3);tag.putLong("LaunchTime",CLOCK-2000);
    tag.putLong("ImpactTime",CLOCK-1000);tag.putFloat("ImpactYExact",244.96F);
    tag.putBoolean("BlastCleared",true);
    // Cover every camera-to-body sightline, not just the Nail's footprint.
    // The final control raises the ground above all Nail geometry while keeping draws enabled.
    boolean buried=scene.name.equals("grounded-terrain-occluded-control");
    if(!groundReady||buried) {
     for(int x=-GROUND_RADIUS;x<=GROUND_RADIUS;x++)for(int z=-GROUND_RADIUS;z<=GROUND_RADIUS;z++)
      level.setBlock(new net.minecraft.core.BlockPos(x,buried?319:244,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
     groundReady=true;
    }
   }
   nail.load(tag);nail.setCustomNameVisible(false);level.addFreshEntity(nail);entityId=nail.getId();
  },server);
 }
 @Override public void beforeFrame() {
  if(done||setup==null||!setup.isDone()||mc.level==null||mc.player==null) return;
  if(setup.isCompletedExceptionally()) {finish(new AssertionError("Server setup failed"));return;}
  mc.level.setGameTime(CLOCK);
  mc.player.setPos(scene.x,scene.y-mc.player.getEyeHeight(),scene.z);
  mc.player.xo=mc.player.xOld=mc.player.getX();mc.player.yo=mc.player.yOld=mc.player.getY();mc.player.zo=mc.player.zOld=mc.player.getZ();
  mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
  CelestialNailAtmosphere.shake=0;CelestialNailAtmosphere.flash=0;mc.particleEngine.setLevel(mc.level);
 }
 @Override public void afterFrame() {
  if(done||setup==null||!setup.isDone()||mc.level==null||mc.player==null) return;
  try {
   if(mc.level.getEntity(entityId)==null) {if(++frames>600)throw new AssertionError("Nail never tracked");return;}
   if(grounded()&&!groundedAssertionsRun) {
    SodiumExtrasAssertions.grounded(mc,(CelestialNailEntity)mc.level.getEntity(entityId),report);
    groundedAssertionsRun=true;
   }
   if(++frames<(stage==0?120:45)) return;frames=0;
   if(stage==0) {hiddenA=Screenshot.takeScreenshot(mc.getMainRenderTarget());stage=1;}
   else if(stage==1) {
    hiddenB=Screenshot.takeScreenshot(mc.getMainRenderTarget());
    // The large ground plane can still be uploading meshes after initial tracking.
    // Wait for a stable hidden reference rather than letting terrain noise mask the monument.
    if(grounded()&&changed(hiddenA,hiddenB)>20) {
     hiddenA.close();hiddenA=hiddenB;hiddenB=null;return;
    }
    SodiumExtrasTestHooks.hideNail=scene.suppress;drawStart=SodiumExtrasTestHooks.renders;stage=2;
   } else {
    try(var visible=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {evaluate(visible);}
    hiddenA.close();hiddenB.close();hiddenA=hiddenB=null;index++;setup=null;
   }
  } catch(Throwable e) {finish(e);}
 }
 private static int changed(NativeImage a,NativeImage b) {
  if(a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight())throw new AssertionError("Window resized");
  return changed(a,b,a.getWidth()/3,a.getHeight()/6,a.getWidth()*2/3,a.getHeight()*5/6);
 }
 private static int changed(NativeImage a,NativeImage b,int left,int top,int right,int bottom) {
  int count=0;
  for(int y=top;y<bottom;y++) for(int x=left;x<right;x++) {
   int av=a.getPixelRGBA(x,y),bv=b.getPixelRGBA(x,y),delta=0;
   for(int c=0;c<3;c++)delta=Math.max(delta,Math.abs(((av>>(8*c))&255)-((bv>>(8*c))&255)));
   if(delta>8)count++;
  }
  return count;
 }
 // Project a narrow strip around the buried body's axis, well below the surface.
 // All visible grounded cameras look at that axis; its x coordinate is image center.
 private int[] buriedProbe(NativeImage image) {
  double p=Math.toRadians(pitch), distance=Math.hypot(scene.x,scene.z);
  double focal=image.getHeight()/(2*Math.tan(Math.toRadians(mc.options.fov().get())/2));
  int[] ys=new int[2];int i=0;
  for(double worldY:new double[]{205,225}) {
   double dy=worldY-scene.y, depth=distance*Math.cos(p)-dy*Math.sin(p);
   ys[i++]=(int)Math.round(image.getHeight()/2.0-focal*(distance*Math.sin(p)+dy*Math.cos(p))/depth);
  }
  int top=Math.max(0,Math.min(ys[0],ys[1])),bottom=Math.min(image.getHeight(),Math.max(ys[0],ys[1]));
  return new int[]{image.getWidth()/2-3,top,image.getWidth()/2+4,bottom};
 }
 private void evaluate(NativeImage visible) throws Exception {
  int noise=changed(hiddenA,hiddenB),pixels=changed(hiddenB,visible);
  boolean pixelVisible=pixels>Math.max(80,noise*4+40),expected=!scene.name.endsWith("-control");
  long renders=SodiumExtrasTestHooks.renders-drawStart;
  var entity=mc.level.getEntity(entityId);
  boolean tracked=entity instanceof CelestialNailEntity;
  float age=tracked?((CelestialNailEntity)entity).summonAge(0):-1;
  boolean passed=tracked&&Math.abs(age-(grounded()?3000:300))<.01&&((CelestialNailEntity)entity).isEmbedded()==grounded()&&pixelVisible==expected&&(expected?renders>0:(scene.offscreen&&grounded()||scene.name.equals("grounded-terrain-occluded-control"))||renders==0);
  var result=new LinkedHashMap<String,Object>();
  if(grounded()&&expected) {
   int[] probe=buriedProbe(visible);
   int probePixels=(probe[2]-probe[0])*Math.max(0,probe[3]-probe[1]);
   int buriedNoise=changed(hiddenA,hiddenB,probe[0],probe[1],probe[2],probe[3]);
   int buriedPixels=changed(hiddenB,visible,probe[0],probe[1],probe[2],probe[3]);
   boolean buriedOccluded=probePixels>=100&&buriedPixels<=Math.max(4,buriedNoise*4);
   result.put("buriedProbePixels",probePixels);result.put("buriedChangedPixels",buriedPixels);
   result.put("buriedNoisePixels",buriedNoise);result.put("buriedOccluded",buriedOccluded);
   passed&=buriedOccluded;
  }
  if(scene.name.equals("grounded-terrain-occluded-control"))passed&=renders>0;
  if(grounded())result.put("groundRadius",GROUND_RADIUS);
  result.put("case",scene.name);result.put("tracked",tracked);result.put("summonAge",age);result.put("embedded",tracked&&((CelestialNailEntity)entity).isEmbedded());
  result.put("expectedVisible",expected);result.put("visiblePixels",pixelVisible);result.put("changedPixels",pixels);
  result.put("noisePixels",noise);result.put("renders",renders);result.put("passed",passed);
  result.put("camera",List.of(scene.x,scene.y,scene.z,yaw,pitch));cases.add(result);
  hiddenB.writeToFile(output.resolve(scene.name+"-hidden.png"));visible.writeToFile(output.resolve(scene.name+"-visible.png"));
  if(!passed)throw new AssertionError("Image/camera regression: "+result);
 }
 private void finish(Throwable error) {
  if(done)return;done=true;
  report.put("cases",cases);report.put("expectedCases",SCENES.size());report.put("complete",error==null&&cases.size()==SCENES.size());
  report.put("passed",error==null&&cases.size()==SCENES.size());
  if(error!=null){report.put("failure",error.toString());LogUtils.getLogger().error("[NailCompatTest] Failed",error);}
  try {Files.writeString(mc.gameDirectory.toPath().resolve("sodium-extras-compat-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
  catch(Exception e){throw new IllegalStateException(e);}
  mc.stop();
 }
}
