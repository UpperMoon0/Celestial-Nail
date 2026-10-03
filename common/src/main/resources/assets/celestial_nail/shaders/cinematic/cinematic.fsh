#version 150
uniform sampler2D Scene;
uniform sampler2D Depth;
uniform mat4 InverseViewProjection;
uniform vec2 PixelSize;
uniform vec3 Center; // Impact origin relative to the camera, avoiding large-coordinate precision loss.
uniform vec4 Event; // age, crater radius, nail height, dust envelope
uniform vec2 Grade; // impact pulse, lingering atmosphere
uniform int Samples;
uniform vec4 Pulse; // presentation age, authored stage, projected impact focus
uniform vec4 DustFront; // front radius, width, curtain height, gravity drop
uniform vec4 DustPlume; // plume radius, rise, horizontal bounds, upper bound
in vec2 uv;
out vec4 fragColor;

float hash(vec3 p) {
    p=fract(p*.1031);
    p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}
float noise(vec3 p) {
    vec3 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float billow(vec3 p) { return noise(p)*.66 + noise(p*2.03+13.7)*.34; }
vec3 unproject(float z) {
    vec4 p=InverseViewProjection*vec4(uv*2.0-1.0,z*2.0-1.0,1.0);
    return p.xyz/p.w;
}
float density(vec3 p) {
    float age=Event.x;
    float radial=length(p.xz);
    float front=DustFront.x, width=DustFront.y, curtainHeight=DustFront.z, drop=DustFront.w;
    float shell=exp(-pow((radial-front)/width,2.0));
    float curtain=shell*exp(-pow((p.y-curtainHeight*.32)/(curtainHeight*.7),2.0));
    float wake=(1.0-smoothstep(front*.72,front+width,radial));
    wake*=exp(-pow(p.y/max(5.0,curtainHeight*.45),2.0))*.7;
    float plume=exp(-pow(radial/DustPlume.x,2.0));
    plume*=exp(-pow((p.y-DustPlume.y*.42)/max(5.0,DustPlume.y*.65),2.0))*.8;
    float upper=(curtain+wake+plume)*smoothstep(-3.0,1.0,p.y);
    // Heavy dust spills over ledges instead of stopping at the impact plane.
    // The captured world depth clips the volume against cliffs and lower ground.
    float spill=shell*smoothstep(-drop-width,-drop+width,p.y);
    spill*=1.0-smoothstep(0.0,curtainHeight*.3,p.y);
    spill*=smoothstep(0.0,12.0,drop)*.85;
    float mass=upper+spill;
    if(mass<.002) return 0.0; // Skip all procedural noise in empty ray segments.
    vec2 outward=p.xz/max(radial,.001);
    vec3 material=p-vec3(outward.x,0.0,outward.y)*min(age*1.25,front*.7);
    material.y+=p.y<0.0?drop*.65:-age*.16;
    float n=billow(material*.055);
    return mass*(.32+.68*smoothstep(.18,.7,n));
}
void main() {
    vec4 original=texture(Scene,uv);
    vec3 rgb=original.rgb;
    if (Grade.y>.001) {
        float l=dot(rgb,vec3(.2126,.7152,.0722));
        float edge=smoothstep(.25,.8,length((uv-.5)*vec2(1.0,.8)));
        rgb=mix(rgb,vec3(l)*vec3(.79,.92,1.06),Grade.y);
        rgb*=1.0-edge*Grade.y*.38;
    }
    if (Event.w>.001 && Samples>0) {
        float depth=texture(Depth,uv).r;
        vec3 end=unproject(min(depth,.99999));
        vec3 ray=normalize(end);
        float maxDistance=depth<.99999?length(end):4096.0;
        vec3 low=Center+vec3(-DustPlume.z,-DustFront.w-DustFront.y,-DustPlume.z);
        vec3 high=Center+vec3(DustPlume.z,DustPlume.w,DustPlume.z);
        vec3 safeRay=vec3(abs(ray.x)<.00001?.00001:ray.x,abs(ray.y)<.00001?.00001:ray.y,abs(ray.z)<.00001?.00001:ray.z);
        vec3 ta=(low/safeRay), tb=(high/safeRay);
        vec3 near=min(ta,tb), far=max(ta,tb);
        float start=max(0.0,max(near.x,max(near.y,near.z)));
        float stop=min(maxDistance,min(far.x,min(far.y,far.z)));
        if (stop>start) {
            float stepSize=max(0.0,stop-start)/float(Samples);
            float opacity=0.0;
            // World-stable jitter; no fresh per-frame random noise or temporal history.
            float jitter=hash(floor((ray*start-Center)*2.0));
            for(int i=0;i<24;i++) {
                if(i>=Samples || opacity>.96 || stepSize<=0.0) break;
                vec3 p=ray*(start+(float(i)+jitter)*stepSize)-Center;
                float extinction=density(p)*Event.w*.11;
                opacity+=(1.0-opacity)*(1.0-exp(-extinction*stepSize));
            }
            vec3 dustColor=mix(vec3(.22,.25,.29),vec3(.52,.58,.62),clamp(ray.y*.5+.5,0.0,1.0));
            rgb=mix(rgb,dustColor,min(.96,opacity));
        }
    }
    if (Grade.x>.001) {
        float amount=clamp(Grade.x,0.0,1.0);
        float l=dot(original.rgb,vec3(.2126,.7152,.0722));
        int stage=int(Pulse.y+.5);
        vec3 ink=vec3(.003,.006,.015), snow=vec3(.96,.99,1.0);
        vec3 impact;
        if(stage==5 || stage==6) {
            // Recovery and Reduced are a continuous grade, with no threshold flashes.
            impact=mix(rgb,vec3(dot(rgb,vec3(.2126,.7152,.0722)))*vec3(.66,.84,1.09),.65);
            float vignette=smoothstep(.22,.8,length((uv-.5)*vec2(1.0,.8)));
            impact*=1.0-vignette*.32;
        } else {
            float nx=dot(texture(Scene,uv+vec2(PixelSize.x,0)).rgb,vec3(.2126,.7152,.0722));
            float ny=dot(texture(Scene,uv+vec2(0,PixelSize.y)).rgb,vec3(.2126,.7152,.0722));
            float edge=clamp((abs(l-nx)+abs(l-ny))*7.0,0.0,1.0);
            float tone=smoothstep(.22,.39,l);
            vec2 delta=uv-Pulse.zw;
            delta.x*=PixelSize.y/PixelSize.x;
            float r=length(delta), angle=atan(delta.y,delta.x);
            float seam=abs(sin(angle*12.0+sin(r*31.0)*.12));
            float fractures=(1.0-smoothstep(.006,.02,seam))*smoothstep(.025,.08,r)
                    *(1.0-smoothstep(.3,1.0,r));
            if(stage==0) impact=mix(ink,snow,smoothstep(.025,.22,edge));
            else if(stage==1) impact=mix(ink,snow,tone);
            else if(stage==2) impact=mix(snow,ink,tone);
            else if(stage==3) impact=mix(ink,vec3(1.0,.92,.72),tone);
            else impact=mix(ink,snow,edge);
            if(stage>=2) impact=mix(impact,stage==2?vec3(.68,.84,1.0):vec3(1.0,.92,.75),fractures*.85);
            // A brief pressure ripple reinforces the flash without a persistent blur pass.
            float ring=exp(-pow((r-(.08+Pulse.x*.045))/.035,2.0));
            impact=mix(impact,snow,ring*.12);
        }
        rgb=mix(rgb,impact,amount);
    }
    fragColor=vec4(rgb,original.a);
}
