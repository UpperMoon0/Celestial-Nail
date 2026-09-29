#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main(){vec4 c=texture(Sampler0,texCoord0)*vertexColor*ColorModulator;if(c.a<0.002)discard;fragColor=c;}
