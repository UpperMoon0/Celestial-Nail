#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec3 localPosition;
in vec3 viewPosition;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash3(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
// Crystal UVs: x = 2 + atlas tile + .9 * face U; y = seed + .9 * face V.
// Color R retains facet shade; GB carry a 16-bit looping clock; A retains distance/crumble fade.
vec4 crystal(vec2 encoded, vec4 data) {
 float material=floor(encoded.x)-2.0;
 vec2 uv=fract(encoded)/.9;
 float seed=hash3(vec3(floor(encoded.y),material,7.0));
 float phase=(data.g*65280.0+data.b*255.0)/65535.0*6.2831853;
 vec3 eye=normalize(-viewPosition);
 vec3 normal=normalize(cross(dFdx(viewPosition),dFdy(viewPosition)));
 float facing=abs(dot(normal,eye));
 float rim=pow(1.0-facing,3.0);
 vec2 inner=uv+(eye.xy*.055)*(.4+.6*(1.0-facing));
 float flow=sin(inner.y*9.0+sin(inner.x*7.0+phase)*.65-phase*2.0+seed*6.28);
 float caustic=pow(.5+.5*sin(inner.x*14.0+inner.y*8.0+phase*2.0+seed*9.0),12.0);
 vec3 base=texture(Sampler0,vec2((material*16.0+.5+uv.x*15.0)/128.0,(.5+uv.y*15.0)/16.0)).rgb;
 vec3 rgb=base*data.r*(.77+.10*flow)+vec3(.02,.12,.19)*caustic+vec3(.08,.20,.24)*rim;
 // A broad, restrained reflection slides with the camera, beneath the crisp star.
 float sheen=exp(-pow((inner.x*.7+inner.y-.72-.18*sin(phase+seed*6.28)-eye.x*.18)/.10,2.0));
 rgb+=vec3(.12,.21,.24)*sheen*(.25+.75*facing);
 vec2 center=vec2(.28+.40*seed,.38+.20*sin(seed*37.0));
 center+=vec2(sin(phase+seed*6.28),cos(phase*2.0+seed*6.28))*.035+eye.xy*.035;
 vec2 d=abs(uv-center);
 float aa=max(fwidth(uv.x),fwidth(uv.y));
 float starShape=d.x/.036+d.y/.15;
 float star=1.0-smoothstep(.65,1.0+aa*22.0,starShape);
 float core=exp(-dot(d,d)/.00024);
 float twinkle=pow(.5+.5*sin(phase*3.0+seed*31.0),8.0);
 float sparse=smoothstep(.66,.82,seed);
 // Fade subpixel sparkles instead of letting distant facets flicker.
 float resolved=1.0-smoothstep(.015,.09,aa);
 rgb=mix(rgb,vec3(.86,.98,1.0),clamp((star+core*.35)*twinkle*sparse*resolved*.90,0.0,1.0));
 return vec4(rgb,data.a);
}

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
  float height=uv.x+5.0, phase=uv.y;
  vec2 facet=color.rg;
  float seed=hash3(vec3(color.b*255.0,7.0,19.0));
  // Offset arrival on each facet; no continuous horizontal ring or hard leading edge.
  float distance=height-phase+(seed-.5)*.075+.009*sin(facet.x*6.28+seed*13.0);
  float front=exp(-pow(distance/.023,2.0));
  float wake=exp(-max(distance,0.0)/.095)*smoothstep(0.0,.028,distance);
  float threads=.5+.5*sin(facet.x*17.0+sin(facet.y*8.0+seed*12.0)*1.1+seed*24.0);
  float silk=pow(threads,10.0);
  float broad=pow(.5+.5*sin(facet.x*8.0-facet.y*3.0+seed*18.0),3.0);
  float envelope=smoothstep(0.0,.06,phase)*(1.0-smoothstep(.94,1.0,phase));
  float energy=(front*(.09+.30*silk+.10*broad)+wake*(.025+.10*silk))*envelope;
  vec3 normal=normalize(cross(dFdx(viewPosition),dFdy(viewPosition)));
  float facing=abs(dot(normal,normalize(-viewPosition)));
  energy*=.65+.35*facing;
  vec3 tint=mix(vec3(.055,.32,.85),vec3(.48,.91,1.0),front*.8+silk*.2);
  return vec4(tint,min(.48,energy)*color.a);
 }
 return vec4(0.0);
}
void main(){
 vec4 c=texCoord0.x>=2.0 ? crystal(texCoord0,vertexColor) : texCoord0.x<0.0 ? effect(texCoord0,vertexColor) : texture(Sampler0,texCoord0)*vertexColor;
 c*=ColorModulator;if(c.a<0.002)discard;fragColor=c;
}
