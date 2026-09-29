"""GPU shader preview and self-contained animated HTML viewer; no game launch required.
Requires numpy, Pillow, moderngl and glcontext. Export with render_nail_job.ps1 first.
"""
from pathlib import Path
import sys, math, struct, json, base64
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
meshfile=ROOT/'build/nail-mesh.txt'
header=meshfile.read_text().splitlines()[0]
if not header.startswith('# bodyFaces='):
    raise SystemExit('Re-export the mesh with the current tools/render_nail_job.ps1 first.')
meta=dict(item.split('=') for item in header[2:].split());height=float(meta['height']);body_count=int(meta['bodyFaces'])
rows=np.loadtxt(meshfile,dtype=np.float32)
verts=[];pulseverts=[]
for face_index,row in enumerate(rows):
    mat,shade=int(row[0]),float(row[1]);points=row[2:].reshape(4,3)
    seed=(struct.unpack('I',np.float32(points[0,0]*np.float32(17)+points[0,1]*np.float32(31)+points[0,2]*np.float32(47)).tobytes())[0]&0x7fffffff)%251
    normal=np.cross(points[1]-points[0],points[2]-points[0]);normal/=max(np.linalg.norm(normal),1e-12)
    for i in [0,1,2,0,2,3]:
        u,v=[(0,0),(1,0),(1,1),(0,1)][i]
        if mat in (3,4,6):uv=(2+mat+u*.9,seed+v*.9)
        else:uv=((mat*16+.5+u*15)/128,(.5+v*15)/16)
        verts.append([*points[i],shade,shade,shade,1,*uv])
        if face_index<body_count and mat>=3:
            p=points[i]+normal*.002
            pulseverts.append([*p,u,v,seed/255,1,-5+p[1]/height,0])
vertices=np.array(verts,dtype='f4');crystal=vertices[:,7]>=2
pulse_vertices=np.array(pulseverts,dtype='f4')
# Match the game's normalized byte color precision.
vertices[:,3:7]=np.floor(vertices[:,3:7]*255)/255
pulse_vertices[:,3:7]=np.floor(pulse_vertices[:,3:7]*255)/255
vbo=ctx.buffer(vertices.tobytes());vao=ctx.vertex_array(program,[(vbo,'3f 4f 2f','Position','Color','UV0')])
pbo=ctx.buffer(pulse_vertices.tobytes());pvao=ctx.vertex_array(program,[(pbo,'3f 4f 2f','Position','Color','UV0')])
teximage=Image.open(assets/'celestial_nail/textures/entity/celestial_nail.png').convert('RGBA')
tex=ctx.texture(teximage.size,4,teximage.tobytes());tex.filter=(moderngl.NEAREST,moderngl.NEAREST);tex.use()
program['Sampler0']=0;program['ColorModulator'].value=(1,1,1,1)
size=(640,720);fbo=ctx.simple_framebuffer(size);fbo.use();ctx.enable(moderngl.DEPTH_TEST)
ctx.blend_func=moderngl.SRC_ALPHA,moderngl.ONE_MINUS_SRC_ALPHA

def render(age,center,extent,yaw,pulse=False):
    clock=round(age%960/960*65535)
    vertices[crystal,4]=(clock>>8)/255;vertices[crystal,5]=(clock&255)/255
    vbo.write(vertices.tobytes())
    a=math.radians(yaw);c,s=math.cos(a),math.sin(a)
    view=np.array([[c,0,s,0],[0,1,0,-center],[-s,0,c,-14],[0,0,0,1]],dtype='f4')
    w=extent*size[0]/size[1]
    proj=np.array([[1/w,0,0,0],[0,1/extent,0,0],[0,0,-2/30,-1],[0,0,0,1]],dtype='f4')
    program['ModelViewMat'].write(view.T.tobytes());program['ProjMat'].write(proj.T.tobytes())
    fbo.clear(.025,.045,.075,1,depth=1);ctx.disable(moderngl.BLEND);vao.render(moderngl.TRIANGLES)
    if pulse:
        pulse_vertices[:,8]=1-(age%100)/100;pbo.write(pulse_vertices.tobytes())
        ctx.enable(moderngl.BLEND);fbo.depth_mask=False;pvao.render(moderngl.TRIANGLES);fbo.depth_mask=True
    return Image.frombytes('RGB',size,fbo.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM)

out=ROOT/'build/crystal-glint-preview';out.mkdir(exist_ok=True)
render(110,3.81,4.05,198,True).save(out/'full-model.png')
frames=[render(i*5,6.48,1.3,198+7*math.sin(i/48*math.tau)) for i in range(48)]
frames[0].save(out/'crystal-shader.gif',save_all=True,append_images=frames[1:],duration=250,loop=0)
frames[22].save(out/'crown.png')
frames=[render(i*2,3.81,4.05,198,True) for i in range(50)]
frames[0].save(out/'pulse-shader.gif',save_all=True,append_images=frames[1:],duration=100,loop=0)
# Animation and view response must each be visible. The clock must loop seamlessly.
reference=np.array(render(0,6.48,1.3,198),dtype=int)
assert np.max(np.abs(reference-np.array(render(160,6.48,1.3,198),dtype=int)))>20
assert np.mean(np.abs(reference-np.array(render(0,6.48,1.3,204),dtype=int)))>1
assert np.array_equal(reference,np.array(render(960,6.48,1.3,198),dtype=int))
assert np.max(np.abs(np.array(render(17,6.48,1.3,198,True),dtype=int)-np.array(render(17,6.48,1.3,198,False),dtype=int)))>5

# Same fragment shader in WebGL2; only matrix imports/version and the vertex clock
# transport change. The viewer derives time per frame instead of rebuilding every vertex.
vs=shader(assets/'minecraft/shaders/core/celestial_nail_sky.vsh').replace('#version 150','#version 300 es\nprecision highp float;\nuniform float PreviewAge;')
vs=vs.replace('texCoord0=UV0;', '''texCoord0=UV0;
 if(UV0.x>=2.0){float clock=floor(mod(PreviewAge,960.0)/960.0*65535.0+.5);vertexColor.gb=vec2(floor(clock/256.0),mod(clock,256.0))/255.0;}
 if(UV0.x < -3.5 && UV0.x > -5.5)texCoord0.y=1.0-mod(PreviewAge,100.0)/100.0;''')
fs=shader(assets/'minecraft/shaders/core/celestial_nail_sky.fsh').replace('#version 150','#version 300 es\nprecision highp float;')
b64=lambda data:base64.b64encode(data).decode('ascii')
data={'vertex':vs,'fragment':fs,'model':b64(vertices.tobytes()),'pulse':b64(pulse_vertices.tobytes()),'atlas':b64(teximage.tobytes())}
html=(ROOT/'tools/nail_shader_preview.html').read_text(encoding='utf-8-sig').replace('__PREVIEW_DATA__',json.dumps(data))
(out/'preview.html').write_text(html,encoding='utf-8')
print('Animation, camera, pulse and loop checks passed. Offline viewer:',out/'preview.html')
