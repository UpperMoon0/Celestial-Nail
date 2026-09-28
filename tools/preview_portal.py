"""Offline contact sheet of real portal geometry at four timeline positions."""
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import preview_nail as preview

scene=np.loadtxt(preview.root/'build/portal-scene.txt')
canvas=Image.new('RGB',(1680,1120),(15,24,39))
preview.canvas=canvas
draw=ImageDraw.Draw(canvas)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',24)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',17)
draw.text((35,25),'CELESTIAL RIFT  /  SUMMON → EMERGE → HOVER → CLOSE',font=font,fill=(225,240,255))
draw.text((35,62),'Actual mesh and animation poses · offline rendering · default nail height 72 blocks',font=small,fill=(147,181,207))
for i,(title,detail) in enumerate([
    ('OPENING','White slit expands into a four-point rift'),
    ('EMERGING','Nail passes through the clipped aperture'),
    ('FLOATING','Rift stays 36 blocks above the crown'),
    ('LAUNCH','Aperture collapses before descent')]):
    preview.mesh=scene[scene[:,0]==i,1:]
    preview.render((20+i*415,110,395,900),2.8,-.28,(0,9.5,0),40)
    draw.text((30+i*415,1030),title,font=font,fill=(200,235,255))
    draw.text((30+i*415,1065),detail,font=small,fill=(147,181,207))
target=preview.root/'docs/celestial-portal-sequence.png'
canvas.save(target)
print(target)
