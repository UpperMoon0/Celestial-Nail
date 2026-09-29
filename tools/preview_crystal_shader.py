"""Render the actual crystal GLSL with the exported game mesh (not a game screenshot).
Requires numpy, Pillow, moderngl and glcontext; run render_nail_job.ps1 first.
"""
from pathlib import Path
import sys, math, struct
import numpy as np
from PIL import Image
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'build/shader-preview-deps'))
import moderngl
ctx = moderngl.create_standalone_context(require=330)
assets=ROOT/'common/src/main/resources/assets'
def shader(path):
    return path.read_text().replace('#moj_import <minecraft:dynamictransforms.glsl>',
        'uniform mat4 ModelViewMat;\nuniform vec4 ColorModulator;').replace(
        '#moj_import <minecraft:projection.glsl>', 'uniform mat4 ProjMat;')
programs=[]
for stem in ['minecraft/shaders/core/celestial_nail_sky','celestial_nail/shaders/core/sky']:
    programs.append(ctx.program(vertex_shader=shader(assets/(stem+'.vsh')),fragment_shader=shader(assets/(stem+'.fsh'))))
print('Both shader versions compiled:',ctx.info['GL_RENDERER'])
program=programs[0]
rows=np.loadtxt(ROOT/'build/nail-mesh.txt',dtype=np.float32)
verts=[]
for row in rows:
    mat,shade=int(row[0]),float(row[1]); points=row[2:].reshape(4,3)
    seed=(struct.unpack('I',np.float32(points[0,0]*np.float32(17)+points[0,1]*np.float32(31)+points[0,2]*np.float32(47)).tobytes())[0]&0x7fffffff)%251
    for i in [0,1,2,0,2,3]:
        u,v=[(0,0),(1,0),(1,1),(0,1)][i]
        if mat in (3,4,6): uv=(2+mat+u*.9,seed+v*.9)
        else: uv=((mat*16+.5+u*15)/128,(.5+v*15)/16)
        verts.append([*points[i],shade,shade,shade,1,*uv])
vertices=np.array(verts,dtype='f4'); crystal=vertices[:,7]>=2
vbo=ctx.buffer(vertices.tobytes()); vao=ctx.vertex_array(program,[(vbo,'3f 4f 2f','Position','Color','UV0')])
teximage=Image.open(assets/'celestial_nail/textures/entity/celestial_nail.png').convert('RGBA')
tex=ctx.texture(teximage.size,4,teximage.tobytes());tex.filter=(moderngl.NEAREST,moderngl.NEAREST);tex.use()
program['Sampler0']=0;program['ColorModulator'].value=(1,1,1,1)
size=(640,720);fbo=ctx.simple_framebuffer(size);fbo.use();ctx.enable(moderngl.DEPTH_TEST)
def render(age,center,extent,yaw):
    clock=round(age%960/960*65535)
    vertices[crystal,4]=(clock>>8)/255;vertices[crystal,5]=(clock&255)/255
    vbo.write(vertices.tobytes())
    a=math.radians(yaw);c,s=math.cos(a),math.sin(a)
    view=np.array([[c,0,s,0],[0,1,0,-center],[-s,0,c,-14],[0,0,0,1]],dtype='f4')
    h=extent;w=h*size[0]/size[1]
    proj=np.array([[1/w,0,0,0],[0,1/h,0,0],[0,0,-2/30,-1],[0,0,0,1]],dtype='f4')
    program['ModelViewMat'].write(view.T.tobytes());program['ProjMat'].write(proj.T.tobytes())
    fbo.clear(.025,.045,.075,1,depth=1);vao.render(moderngl.TRIANGLES)
    return Image.frombytes('RGB',size,fbo.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM)
out=ROOT/'build/crystal-glint-preview';out.mkdir(exist_ok=True)
render(110,3.81,4.05,18).save(out/'full-model.png')
frames=[render(i*5,6.48,1.3,18+7*math.sin(i/48*math.tau)) for i in range(48)]
frames[0].save(out/'crystal-shader.gif',save_all=True,append_images=frames[1:],duration=250,loop=0)
frames[22].save(out/'crown.png')
# Exercise animation and view response independently; do not count camera motion as animation.
reference=np.array(render(0,6.48,1.3,18),dtype=int)
assert np.max(np.abs(reference-np.array(render(160,6.48,1.3,18),dtype=int)))>20
assert np.mean(np.abs(reference-np.array(render(0,6.48,1.3,24),dtype=int)))>1
# The periodic clock must wrap without a discontinuity.
assert np.array_equal(reference,np.array(render(960,6.48,1.3,18),dtype=int))
print('Rendered animated shader preview:',out)
