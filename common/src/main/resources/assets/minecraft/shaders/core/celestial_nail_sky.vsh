#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 localPosition;
out vec3 viewPosition;
out vec4 vertexColor;
out vec2 texCoord0;
void main(){localPosition=Position;
 viewPosition=(ModelViewMat*vec4(Position,1.0)).xyz;gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);vertexColor=Color;texCoord0=UV0;}
