#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec3 localPosition;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash3(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
vec4 effect(vec2 uv,vec4 color){
 if(uv.x > -3.5){
  vec2 p=vec2(uv.x+3.0,uv.y)*2.0-1.0;
  float r=length(p),a=atan(p.y,p.x);
  float ripple=sin(a*37.0+r*130.0)*0.0025+sin(a*73.0)*0.0015;
  float edge=exp(-pow((r-.90+ripple)/.010,2.0));
  float haze=exp(-pow((r-.88)/.036,2.0))*.16;
  float wisps=.55+.45*sin(a*19.0+r*210.0)*sin(a*31.0-r*70.0);
  return vec4(mix(vec3(.18,.65,1.0),vec3(.78,.96,1.0),edge),min(.65,(edge*.5+haze)*wisps)*color.a);
 }
 if(uv.x > -5.5){
  float y=uv.x+5.0,phase=uv.y;
  float core=exp(-pow((y-phase)/.008,2.0));
  float trail=exp(-pow((y-phase-.022)/.026,2.0))*.20;
  return vec4(.32,.84,1.0,min(.85,core*.7+trail)*color.a);
 }
 // Crack veins precede the breakup; dark space between veins remains transparent.
 float h=hash3(floor(localPosition*13.0));
 float veins=pow(1.0-abs(sin(localPosition.y*19.0+sin(localPosition.x*31.0)+localPosition.z*23.0)),24.0);
 return vec4(.35,.87,1.0,veins*smoothstep(h*.4,h*.4+.15,uv.y)*color.a);
}
void main(){
 vec4 c=texCoord0.x<0.0 ? effect(texCoord0,vertexColor) : texture(Sampler0,texCoord0)*vertexColor;
 c*=ColorModulator;if(c.a<0.002)discard;fragColor=c;
}
