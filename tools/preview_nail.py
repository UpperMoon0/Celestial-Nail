"""Rasterize the exported in-game mesh and atlas. This is an offline model preview."""
from pathlib import Path
import math
import argparse
import numpy as np
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--mesh',type=Path,default=root/'build/nail-mesh.txt')
parser.add_argument('--atlas',type=Path,default=root/'common/src/main/resources/assets/celestial_nail/textures/entity/celestial_nail.png')
parser.add_argument('--output',type=Path,default=root/'docs/celestial-nail-model-preview.png')
args=parser.parse_args() if __name__=='__main__' else parser.parse_args([])
mesh=np.loadtxt(args.mesh)
atlas=np.array(Image.open(args.atlas).convert('RGB'))
W,H=2180,1520
canvas=Image.new('RGB',(W,H),(222,224,226))
draw=ImageDraw.Draw(canvas)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',22)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',16)

background=(26,34,45)

def render(box, yaw, pitch, center, scale):
    x0,y0,w,h=box
    rgb=np.zeros((h,w,3),dtype=np.uint8); rgb[:]=background
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

def contact_sheet():
    global background
    background=(210,214,218)
    draw.text((35,20),'CELESTIAL NAIL / MODEL REVIEW',font=font,fill=(39,48,61))
    draw.text((35,54),'Actual entity geometry + nearest-neighbor game atlas / neutral daylight / offline render',font=small,fill=(74,87,103))
    for i,(angle,title) in enumerate([(math.pi,'FRONT'),(0,'BACK'),(math.pi/2,'LEFT'),(-math.pi/2,'RIGHT')]):
        render((20+i*325,98,315,1160),angle,0,(0,3.80,.42),132)
        draw.text((40+i*325,1270),title,font=small,fill=(44,63,78))
    render((1340,98,810,430),2.2,.18,(0,7.13,.54),315)
    draw.text((1360,540),'CROWN / BEVELED TRACERY / REAR PLATFORM',font=small,fill=(44,63,78))
    render((1340,580,395,340),0,-math.pi/2,(0,7.42,.32),130)
    render((1755,580,395,340),0,math.pi/2,(0,0,.32),130)
    draw.text((1360,931),'TOP / FRACTURED STONE CAP',font=small,fill=(44,63,78))
    draw.text((1775,931),'BOTTOM / CRYSTAL HEART',font=small,fill=(44,63,78))
    render((1340,975,395,440),math.pi,.015,(0,1.17,0),185)
    render((1755,975,395,440),3.3,0,(0,5.88,0),245)
    draw.text((1360,1426),'BROKEN CASING / CUBE FRAGMENTS',font=small,fill=(44,63,78))
    draw.text((1775,1426),'LANCETS / LONG DIAMOND INLAYS',font=small,fill=(44,63,78))
    draw.text((35,1360),f'{len(mesh):,} baked quads | default height: 72 blocks | flat facets and pixel materials',font=small,fill=(74,87,103))
    draw.text((35,1390),'Lighting, portal, shader pulses and atmosphere are excluded to make the model easier to compare.',font=small,fill=(74,87,103))
    args.output.parent.mkdir(parents=True,exist_ok=True)
    canvas.save(args.output)
    print(f'{len(mesh)} quads; preview: {args.output}')

if __name__=='__main__': contact_sheet()
