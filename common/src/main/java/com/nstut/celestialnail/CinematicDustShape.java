package com.nstut.celestialnail;

/** Per-frame volume geometry, shared by density and ray bounds without per-sample recomputation. */
public record CinematicDustShape(float front, float width, float curtainHeight, float drop,
                                 float plumeRadius, float rise, float span, float top) {
    public static CinematicDustShape at(float age, float radius, float height) {
        age=Math.max(0,age); radius=Math.max(4,radius); height=Math.max(8,height);
        float front=Math.min(radius*4,4+age*2.2F);
        float width=Math.max(6,front*.28F);
        float curtain=Math.max(8,Math.min(height*.85F,age*.9F));
        float falling=Math.max(0,age-8);
        float drop=Math.min(384,Math.min(radius*6+height*.5F,falling*falling*.018F));
        float plume=Math.max(6,radius*.65F+age*.14F);
        float rise=Math.min(height*1.5F,age*.85F);
        return new CinematicDustShape(front,width,curtain,drop,plume,rise,
                Math.max(front+width*2,plume*2),Math.max(curtain*1.8F,rise*1.5F)+5);
    }
}
