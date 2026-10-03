package com.nstut.celestialnail;

/** Authored 750 ms sequence; ages are render-time ticks, never random flashes. */
public final class ImpactSequence {
    public static final int OFF=0, REDUCED=1, FULL=2;
    public static final float DURATION=15;
    private ImpactSequence() {}
    public static int stage(float age,int mode) {
        if(age<0 || age>=DURATION || mode==OFF) return -1;
        if(mode==REDUCED) return 6;
        if(age<1.2F) return 0;
        if(age<2.8F) return 1;
        if(age<4) return 5;
        if(age<5.6F) return 2;
        if(age<7) return 5;
        if(age<8.2F) return 3;
        if(age<9.4F) return 5;
        if(age<10.6F) return 4;
        return 5;
    }
    public static float strength(float age,int mode) {
        int stage=stage(age,mode);
        if(stage<0) return 0;
        if(mode==REDUCED) return .28F*(1-CelestialNailVisuals.smooth(age/DURATION));
        if(stage==0) return 1;
        if(stage==1 || stage==2) return .98F;
        if(stage==3) return .9F;
        if(stage==4) return .8F;
        if(age<10.6F) return .08F;
        return .55F*(1-CelestialNailVisuals.smooth((age-10.6F)/4.4F));
    }
    public static int mode(String name) {
        if("off".equalsIgnoreCase(name)) return OFF;
        if("reduced".equalsIgnoreCase(name)) return REDUCED;
        return FULL;
    }
}
