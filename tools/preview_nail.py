"""Rasterize the exported in-game mesh and atlas. This is an offline model preview."""
from pathlib import Path
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
mesh=np.loadtxt(root/'build/nail-mesh.txt')
atlas=np.array(Image.open(root/'common/src/main/resources/assets/celestial_nail/textures/entity/celestial_nail.png').convert('RGB'))
W,H=1800,1300
canvas=Image.new('RGB',(W,H),(22,29,39))
draw=ImageDraw.Draw(canvas)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',22)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',16)
draw.text((50,24),'CELESTIAL NAIL  /  FACETED STONE & CRYSTAL',font=font,fill=(218,222,224))
draw.text((50,59),'Offline preview of the actual entity mesh and pixel atlas',font=small,fill=(136,164,180))

def render(box, yaw, pitch, center, scale):
    x0,y0,w,h=box
    rgb=np.zeros((h,w,3),dtype=np.uint8); rgb[:]=(26,34,45)
    depth=np.full((h,w),-np.inf)
    ca,sa=math.cos(yaw),math.sin(yaw); cp,sp=math.cos(pitch),math.sin(pitch)
    rot=np.array([[ca,0,-sa],[sa*sp,cp,ca*sp],[sa*cp,-sp,ca*cp]])
    for row in mesh:
        mat=int(row[0]); shade=row[1]
        points=row[2:14].reshape(4,3)
        alpha=row[14] if len(row)>14 else 1
        v=(points-np.array(center))@rot.T
        v[:,0]=v[:,0]*scale+w/2; v[:,1]=h/2-v[:,1]*scale
        uv=np.array([[0,0],[15,0],[15,15],[0,15]],float)
        for ids in [(0,1,2),(0,2,3)]:
            p=v[list(ids)]; tex=uv[list(ids)]
            lo=np.maximum(np.floor(p[:,:2].min(axis=0)).astype(int),0)
            hi=np.minimum(np.ceil(p[:,:2].max(axis=0)).astype(int),(w-1,h-1))
            if (hi<lo).any(): continue
            xx,yy=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
            den=(p[1,1]-p[2,1])*(p[0,0]-p[2,0])+(p[2,0]-p[1,0])*(p[0,1]-p[2,1])
            if abs(den)<1e-8: continue
            a=((p[1,1]-p[2,1])*(xx-p[2,0])+(p[2,0]-p[1,0])*(yy-p[2,1]))/den
            b=((p[2,1]-p[0,1])*(xx-p[2,0])+(p[0,0]-p[2,0])*(yy-p[2,1]))/den
            c=1-a-b; z=a*p[0,2]+b*p[1,2]+c*p[2,2]
            sl=np.s_[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
            mask=(a>=0)&(b>=0)&(c>=0)&(z>depth[sl])
            if not mask.any():continue
            u=np.clip(np.rint(a*tex[0,0]+b*tex[1,0]+c*tex[2,0]),0,15).astype(int)
            t=np.clip(np.rint(a*tex[0,1]+b*tex[1,1]+c*tex[2,1]),0,15).astype(int)
            color=np.clip(atlas[t,u+mat*16]*shade,0,255).astype(np.uint8)
            rgb[sl][mask]=(color[mask]*alpha+rgb[sl][mask]*(1-alpha)).astype(np.uint8)
            if alpha>.99: depth[sl][mask]=z[mask]
    canvas.paste(Image.fromarray(rgb),(x0,y0))

for i,(angle,title) in enumerate([(math.pi,'FRONT'),(math.pi*.65,'THREE-QUARTER'),(0,'BACK')]):
    render((30+i*330,110,315,1090),angle,.025,(0,3.93,.28),132)
    draw.text((65+i*330,1210),title,font=small,fill=(181,207,219))
render((1040,110,730,500),2.2,.45,(0,7.13,.48),290)
draw.text((1060,620),'CROWN / INSET LANCETS / SWEPT PETALS',font=small,fill=(181,207,219))
render((1040,670,350,500),2.7,.03,(0,1.18,0),205)
draw.text((1060,1190),'FRACTURED TIP',font=small,fill=(181,207,219))
render((1420,670,350,500),3.3,0,(0,5.90,0),320)
draw.text((1440,1190),'GOLD TRACERY',font=small,fill=(181,207,219))
target=root/'docs/celestial-nail-model-preview.png'
target.parent.mkdir(exist_ok=True)
canvas.save(target)
print(f'{len(mesh)} quads; preview: {target}')
