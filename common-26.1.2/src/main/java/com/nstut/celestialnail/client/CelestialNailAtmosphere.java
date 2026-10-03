package com.nstut.celestialnail.client;

import com.nstut.celestialnail.CataclysmTimeline;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.entity.CelestialNailEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Client-only atmosphere; bounded particles and no simulated debris entities or chunk tickets. */
public final class CelestialNailAtmosphere {
    private static final Map<UUID, Echo> ECHOES=new HashMap<>();
    private static final Random RANDOM=new Random(729);
    private static Object world;
    private static int budget;
    public static float darkness, flash, shake, muffle;
    private static double farPlane;
    private static final class Echo {
        CelestialNailEntity nail;
        Vec3 center;
        long impact=-1;
        boolean boom, crack, closing, crumbling;
        CelestialNailAtmosphereSound drone;
        final com.nstut.celestialnail.ImpactPresentationClock pulse=new com.nstut.celestialnail.ImpactPresentationClock();
        Echo(CelestialNailEntity nail) { this.nail=nail; center=nail.position(); }
    }
    private CelestialNailAtmosphere() {}
    public static void track(CelestialNailEntity nail) {
        Minecraft mc=Minecraft.getInstance();
        resetWorld(mc.level);
        CinematicSettings.load(mc.gameDirectory.toPath());
        CelestialNailPortalSound.tickEntity(nail);
        Echo echo=ECHOES.computeIfAbsent(nail.getUUID(), id->new Echo(nail));
        // A newly constructed client entity has idle defaults until its metadata arrives.
        // Only a fully identified Nail may arm the late-delivery grace.
        if(!nail.nailId().isEmpty()) echo.pulse.observe(nail.impactAge(0));
        echo.nail=nail; echo.center=nail.impactAge(0)>=0?nail.impactOrigin():nail.position();
        if (nail.impactAge(0)>=0) echo.impact=nail.level().getGameTime()-(long)nail.impactAge(0);
    }
    private static void resetWorld(Object current) {
        if(world==current) return;
        ECHOES.values().forEach(e->{if(e.drone!=null)e.drone.finish();});
        ProceduralCinematicPass.invalidate();
        ECHOES.clear();world=current; darkness=flash=shake=muffle=0;farPlane=0;
    }
    public static float visibility(double x,double z) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null) return 0;
        return CataclysmTimeline.horizontalFade(Math.hypot(x-mc.player.getX(),z-mc.player.getZ()),mc.options.getEffectiveRenderDistance()*16.0);
    }
    public static float farPlane(float original) { return (float)Math.max(original,farPlane); }
    public static void tick() {
        Minecraft mc=Minecraft.getInstance(); resetWorld(mc.level);
        if(mc.level==null || mc.player==null || mc.isPaused()) return;
        darkness=flash=shake=muffle=0; farPlane=0; budget=48;
        long now=mc.level.getGameTime();
        Iterator<Echo> it=ECHOES.values().iterator();
        while(it.hasNext()) {
            Echo e=it.next(); CelestialNailEntity n=e.nail;
            float impact=e.impact<0?-1:now-e.impact;
            if(n.isRemoved()&&(e.impact<0||e.crumbling||impact>CataclysmTimeline.AFTERMATH_TICKS)) {
                if(e.drone!=null)e.drone.finish(); it.remove();continue;
            }
            float visible=visibility(e.center.x,e.center.z);
            if(visible<=0) {if(e.drone!=null)e.drone.gain=0;continue;}
            double distance=mc.player.position().distanceTo(e.center);
            float near=visible*(float)Math.max(.08,1-distance/640);
            float age=n.summonAge(0),launch=n.launchAge(0),h=n.nailHeight();
            // Maintain depth coverage in every phase, including ticks 28..1200 after impact.
            // The buried tip can be much farther from the camera than the visible crown.
            var camera=mc.gameRenderer.getMainCamera().position();
            farPlane=Math.max(farPlane,CelestialNailVisuals.bodyFarPlane(
                    Math.hypot(n.getX()-camera.x,n.getZ()-camera.z),camera.y,n.getY(),h));
            if(impact>CataclysmTimeline.AFTERMATH_TICKS&&!n.isCrumbling()) {
                farPlane=Math.max(farPlane,Math.abs(n.getY()+h-mc.player.getY())+128);
                if(e.drone!=null){e.drone.finish();e.drone=null;}
                float linger=CinematicSettings.lingering ? com.nstut.celestialnail.CinematicTimeline.lingering(distance,impact) : 0;
                muffle=Math.max(muffle,linger*.35F);
                if(linger>.001F && now%240==Math.floorMod(n.getId(),240)) sound("crystal_creak",linger*.8F);
                if(linger>.001F && now%12==Math.floorMod(n.getId(),12))
                    particle(ParticleTypes.END_ROD,n.getX()+random(h*.08),n.getY()+RANDOM.nextDouble()*h,n.getZ()+random(h*.08),0,.008,0);
                continue;
            }
            if(n.isCrumbling()) {
                if(!e.crumbling){e.crumbling=true;sound("crystal_creak",near*.8F);}
                if(e.drone!=null){e.drone.finish();e.drone=null;}
                farPlane=Math.max(farPlane,Math.abs(n.getY()+h-mc.player.getY())+128);
                float crumble=n.crumbleAge(0);
                if(crumble>6 && crumble<42) for(int j=0;j<6;j++) {
                    double band=Math.min(1,Math.max(0,(crumble-6)/14));
                    double y=n.getY()+h*band+random(h*.075)-Math.pow(Math.max(0,crumble-20),2)*h*.0012;
                    particle(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),n.getX()+random(h*.1),y,n.getZ()+random(h*.1),random(.08),-.3,random(.08));
                    if(j<3)particle(ParticleTypes.CLOUD,n.getX()+random(h*.1),y,n.getZ()+random(h*.1),0,-.05,0);
                }
                continue;
            }
            if(impact<0) {
                farPlane=Math.max(farPlane,Math.min(32768,Math.abs(n.portalY()+h*1.5-mc.player.getY())+h+128));
                float presence=CelestialNailVisuals.smooth(age/40)*near;
                darkness=Math.max(darkness,presence*.24F);
                muffle=Math.max(muffle,presence*.65F);
                if(e.drone==null) { e.drone=new CelestialNailAtmosphereSound("presence",true);mc.getSoundManager().play(e.drone); }
                e.drone.gain=presence*(launch>=24?0:.65F);
                if(!e.crack&&age<30) {e.crack=true;sound("sky_crack",near*.75F);}
                if(!e.closing&&launch>=0&&launch<24) {e.closing=true;sound("portal_close",near);}
                if(launch>=24&&launch<30) muffle=Math.max(muffle,near*.95F);
                if(launch<0&&now%160==Math.floorMod(n.getId(),160)) sound("crystal_creak",near*.35F);
                if(launch<24) {
                    for(int j=0;j<3;j++) {
                        double a=RANDOM.nextDouble()*Math.PI*2,r=h*(.12+RANDOM.nextDouble()*.28);
                        particle(ParticleTypes.END_ROD,n.getX()+Math.cos(a)*r,n.getY()+RANDOM.nextDouble()*h,n.getZ()+Math.sin(a)*r,0,.04,0);
                    }
                    // Lift nearby ground dust into the air, rather than spawning falling-block entities.
                    if(age<170||now%3==0) for(int j=0;j<2;j++)
                        particle(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),mc.player.getX()+random(12),mc.player.getY()+random(2),mc.player.getZ()+random(12),0,.15+RANDOM.nextDouble()*.2,0);
                }
            } else {
                if(e.drone!=null){e.drone.finish();e.drone=null;}
                if(impact<28)farPlane=Math.max(farPlane,Math.abs(e.center.y+h*5-mc.player.getY())+128);
                float after=CataclysmTimeline.aftermath(impact),arrival=CataclysmTimeline.arrival(distance);
                darkness=Math.max(darkness,near*.26F*after);
                // The impact frame is sampled per render frame by the procedural compositor.
                shake=Math.max(shake,near*CataclysmTimeline.shake(impact-arrival)*CinematicSettings.shakeIntensity);
                muffle=Math.max(muffle,near*after*.4F);
                if(!e.boom&&impact>=arrival) { e.boom=true;if(impact<arrival+20)sound("impact_boom",near); }
                if(impact>arrival+80&&now%150==Math.floorMod(n.getId(),150)) sound("aftershock",near*.45F*after);
                if(impact<160) for(int j=0;j<6;j++) {
                    double r=Math.min(n.power(),impact*.7),a=RANDOM.nextDouble()*Math.PI*2;
                    particle(ParticleTypes.LARGE_SMOKE,e.center.x+Math.cos(a)*r,e.center.y+RANDOM.nextDouble()*Math.min(100,impact*.6),e.center.z+Math.sin(a)*r,random(.12),.4,random(.12));
                    if(impact<65) particle(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DEEPSLATE.defaultBlockState()),e.center.x+random(r),e.center.y+20+RANDOM.nextDouble()*40,e.center.z+random(r),random(.3),-.4,random(.3));
                }
                // Fixed branching seams on the cleared spherical bowl, rebuilt with particles as the world settles.
                if(impact>40 && after>.1 && now%3==0) for(int j=0;j<8;j++) {
                    double branch=j*Math.PI/4, along=((now/3+j*7)%40)/40.0, r=n.power()*along;
                    double a=branch+Math.sin(along*17+j)*.035;
                    double y=e.center.y-Math.sqrt(Math.max(0,n.power()*n.power()-r*r))+1.25;
                    particle(ParticleTypes.SOUL_FIRE_FLAME,e.center.x+Math.cos(a)*r,y,e.center.z+Math.sin(a)*r,0,.005,0);
                }
                if(after>.05&&distance<384) for(int j=0;j<3;j++) particle(ParticleTypes.ASH,mc.player.getX()+random(24),mc.player.getY()+6+RANDOM.nextDouble()*12,mc.player.getZ()+random(24),0,-.06,0);
                // An expanding ring travels at the same speed as the delayed sound and camera impulse.
                if(impact>=8&&impact<65) for(int j=0;j<16;j++) {
                    double a=(j+RANDOM.nextDouble())*Math.PI/8,r=CataclysmTimeline.shockRadius(impact);
                    particle(ParticleTypes.CLOUD,e.center.x+Math.cos(a)*r,e.center.y+1,e.center.z+Math.sin(a)*r,Math.cos(a)*.5,.02,Math.sin(a)*.5);
                }
            }
        }
    }
    /** Sample at camera setup so terrain work cannot consume the shake between ticks. */
    public static float cameraShake() {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || world!=mc.level || mc.isPaused()) return 0;
        float strength=0;
        long nanos=System.nanoTime();
        for(Echo e:ECHOES.values()) {
            var n=e.nail;
            if(n.isRemoved() || n.isCrumbling() || n.impactAge(0)<0) continue;
            double distance=mc.player.getEyePosition().distanceTo(e.center);
            float near=com.nstut.celestialnail.CinematicTimeline.proximity(distance,640)*visibility(e.center.x,e.center.z);
            float age=e.pulse.age(nanos,n.impactAge(0));
            strength=Math.max(strength,near*com.nstut.celestialnail.CinematicTimeline.cameraShake(age,distance));
        }
        return strength*(CinematicSettings.impactMode==0?0:CinematicSettings.impactMode==1?.35F:1)*CinematicSettings.shakeIntensity*mc.options.screenEffectScale().get().floatValue();
    }
    /** Independently samples synchronized entity clocks, including when the Nail is outside the view. */
    public static ProceduralCinematicPass.Frame cinematic(float partial, Vec3 camera, Vec3 forward) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || world!=mc.level)
            return new ProceduralCinematicPass.Frame(0,0,-1,0,0,0,0,0,0,0,-1,0,0,0,0);
        float flashStrength=0, atmosphere=0, best=0, pulseAge=-1;
        Vec3 impactCenter=Vec3.ZERO;
        Echo selected=null;
        float selectedAge=-1, selectedDust=0;
        double range=mc.options.getEffectiveRenderDistance()*16.0;
        float accessibility=mc.options.screenEffectScale().get().floatValue();
        for(Echo e:ECHOES.values()) {
            CelestialNailEntity n=e.nail;
            if(n.isRemoved() || n.isCrumbling()) continue;
            float age=n.impactAge(partial);
            if(age<0) continue;
            float visible=CataclysmTimeline.horizontalFade(Math.hypot(e.center.x-camera.x,e.center.z-camera.z),range);
            double distance=camera.distanceTo(e.center);
            float near=com.nstut.celestialnail.CinematicTimeline.proximity(distance,640)*visible;
            Vec3 direction=e.center.add(0,n.nailHeight()*.25,0).subtract(camera).normalize();
            float facing=(float)Math.max(0,forward.dot(direction));
            float presentation=e.pulse.age(System.nanoTime(),age);
            float pulse=com.nstut.celestialnail.ImpactSequence.strength(presentation,CinematicSettings.impactMode)
                    *near*(.65F+.35F*facing)*CinematicSettings.impactIntensity*accessibility*1.5F;
            if(pulse>flashStrength) {flashStrength=pulse;pulseAge=presentation;impactCenter=e.center.subtract(camera);}
            if(CinematicSettings.lingering) atmosphere=Math.max(atmosphere,
                    com.nstut.celestialnail.CinematicTimeline.lingering(distance,age)*visible*accessibility);
            float dust=CinematicSettings.dustSamples==0?0:com.nstut.celestialnail.CinematicTimeline.dust(age)*visible;
            float score=dust/(1+(float)distance/256);
            // A fixed-cost local veil; overlapping events use the strongest volume, never stack pulses.
            if(score>best) {best=score;selected=e;selectedAge=age;selectedDust=dust;}
        }
        if(selected==null) return new ProceduralCinematicPass.Frame(flashStrength,0,-1,0,0,0,0,0,atmosphere,0,pulseAge,CinematicSettings.impactMode,(float)impactCenter.x,(float)impactCenter.y,(float)impactCenter.z);
        Vec3 center=selected.center.subtract(camera);
        return new ProceduralCinematicPass.Frame(flashStrength,
                selectedDust,selectedAge,
                selected.nail.power(),selected.nail.nailHeight(),(float)center.x,(float)center.y,(float)center.z,
                atmosphere,CinematicSettings.dustSamples,pulseAge,CinematicSettings.impactMode,(float)impactCenter.x,(float)impactCenter.y,(float)impactCenter.z);
    }
    private static double random(double range) {return (RANDOM.nextDouble()*2-1)*range;}
    private static void particle(net.minecraft.core.particles.ParticleOptions type,double x,double y,double z,double vx,double vy,double vz) {
        if(budget--<=0)return;
        Minecraft.getInstance().level.addParticle(type,x,y,z,vx,vy,vz);
    }
    private static void sound(String name,float volume) {
        CelestialNailAtmosphereSound sound=new CelestialNailAtmosphereSound(name,false);sound.gain=volume;
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
