"""Render an offline video from Java's actual fracture meshes and rigid transforms.
Run tools/render_nail_shader_job.ps1 first. Requires ffmpeg on PATH.
"""
from pathlib import Path
import os,sys,subprocess,struct,runpy,math
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
jhome=Path(os.environ.get('JAVA_HOME',str(Path.home()/'.gradle/jdks/jetbrains_s_r_o_-21-amd64-windows.2')))
java=jhome/'bin/java.exe';javac=jhome/'bin/javac.exe'
sources=[root/'common/src/main/java/com/nstut/celestialnail/CelestialNailVisuals.java',root/'common/src/main/java/com/nstut/celestialnail/client/CelestialNailMesh.java',root/'common/src/main/java/com/nstut/celestialnail/client/CelestialNailFracture.java',root/'tools/ExportNailFracture.java']
subprocess.run([str(javac),'-d',str(root/'build/preview-model'),*map(str,sources)],check=True)
binary=root/'build/fracture-frames.bin'
subprocess.run([str(java),'-cp',str(root/'build/preview-model'),'ExportNailFracture',str(binary)],check=True)
env=runpy.run_path(str(root/'tools/preview_crystal_shader.py'))
ctx,program,moderngl=env['ctx'],env['program'],env['moderngl']
raw=binary.read_bytes();frames,faces=struct.unpack('>ii',raw[:8]);data=np.frombuffer(raw[8:],dtype='>f4').reshape(frames,1+faces*15)
base=data[0,1:].reshape(faces,15);materials=base[:,0].astype(int);shade=base[:,1]
uv=np.array([(0,0),(1,0),(1,1),(0,0),(1,1),(0,1)],dtype='f4')
seed=np.mod((np.array(base[:,3]*np.float32(17)+base[:,4]*np.float32(31)+base[:,5]*np.float32(47),dtype='f4').view('u4')&0x7fffffff),251)
vertex=np.zeros((faces,6,9),dtype='f4');vertex[:,:,3:6]=shade[:,None,None];vertex[:,:,6]=1
vertex[:,:,7]=(materials[:,None]*16+.5+uv[None,:,0]*15)/128;vertex[:,:,8]=(.5+uv[None,:,1]*15)/16
crystal=np.isin(materials,[3,4,6]);vertex[crystal,:,7]=2+materials[crystal,None]+uv[None,:,0]*.9;vertex[crystal,:,8]=seed[crystal,None]+uv[None,:,1]*.9
vbo=ctx.buffer(vertex.tobytes());vao=ctx.vertex_array(program,[(vbo,'3f 4f 2f','Position','Color','UV0')])
size=(640,800);fbo=ctx.simple_framebuffer(size);fbo.use();ctx.enable(moderngl.DEPTH_TEST|moderngl.BLEND)
a=math.radians(198);c,s=math.cos(a),math.sin(a);extent=6.5;center=1.35;w=extent*size[0]/size[1]
view=np.array([[c,0,s,0],[0,1,0,-center],[-s,0,c,-18],[0,0,0,1]],dtype='f4')
proj=np.array([[1/w,0,0,0],[0,1/extent,0,0],[0,0,-2/40,-1],[0,0,0,1]],dtype='f4');program['ModelViewMat'].write(view.T.tobytes());program['ProjMat'].write(proj.T.tobytes())
out=root/'build/crystal-glint-preview';samples=[]
video=subprocess.Popen(['ffmpeg','-y','-loglevel','error','-f','rawvideo','-pixel_format','rgb24','-video_size','640x800','-framerate','30','-i','-','-an','-c:v','libx264','-pix_fmt','yuv420p','-movflags','+faststart',str(out/'crumble.mp4')],stdin=subprocess.PIPE)
try:
 for i,frame in enumerate(data):
  age=float(frame[0]);f=frame[1:].reshape(faces,15);points=f[:,3:].reshape(faces,4,3)
  vertex[:,:,:3]=points[:,[0,1,2,0,2,3]];vertex[:,:,6]=f[:,2,None]
  clock=round(age/960*65535);vertex[crystal,:,4]=(clock>>8)/255;vertex[crystal,:,5]=(clock&255)/255
  depth=-s*points[:,:,0].mean(axis=1)+c*points[:,:,2].mean(axis=1)
  vbo.write(vertex[np.argsort(depth)].tobytes());fbo.clear(.025,.045,.075,1,depth=1);vao.render(moderngl.TRIANGLES)
  im=Image.frombytes('RGB',size,fbo.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM)
  draw=ImageDraw.Draw(im);draw.text((20,18),f'CELESTIAL NAIL / REMOVAL     {age/20:.2f}s',fill=(165,205,230));video.stdin.write(im.tobytes())
  if i in [0,18,30,42,54,66]:samples.append(im)
finally:video.stdin.close()
assert video.wait()==0
sheet=Image.new('RGB',(640*3,800*2))
for i,im in enumerate(samples):sheet.paste(im,((i%3)*640,(i//3)*800))
sheet.save(out/'crumble-contact.png')
(out/'crumble.html').write_text('<!doctype html><meta charset="utf-8"><title>Nail removal preview</title><body style="background:#07101c;color:#dfeafa;font:16px system-ui;text-align:center"><h2>Celestial Nail — removal</h2><p>Production fracture geometry • offline render • no terrain damage</p><video controls autoplay loop muted style="max-height:80vh;max-width:95vw" src="crumble.mp4"></video><p><a style="color:#8cdfff" href="preview.html">Crystal shader viewer</a></p>',encoding='utf-8')
print(f'Rendered {frames} production-animation frames, {faces} faces per frame: {out / "crumble.mp4"}')
