"""Reproducible 16-pixel material swatches for the baked entity mesh (Pillow)."""
from pathlib import Path
from random import Random
from PIL import Image

root = Path(__file__).resolve().parents[1]
rng = Random(614)
image = Image.new('RGBA', (128, 16))
palette = [(235, 234, 223), (165, 155, 130), (135, 143, 147), (23, 167, 244), (91, 222, 255), (4, 9, 23), (163, 231, 255), (249, 255, 255)]
for tile, base in enumerate(palette):
    for y in range(16):
        for x in range(16):
            delta = rng.choice([-2, -1, 0, 0, 0, 1, 2])
            if tile == 1:
                delta += 12 if x < 2 else (-17 if x > 13 else 0)
            if tile in (3,4,6,7):
                delta += (y//4)*5 + (5 if x<6 else -4)
            image.putpixel((tile*16+x,y),tuple(max(0,min(255,c+delta)) for c in base)+(255,))
    if tile in (0,2):
        # Sparse stepped veins, never filtered gradients.
        for y in range(3,13):
            x=5+(y//3)%3
            image.putpixel((tile*16+x,y),tuple(max(0,c-5) for c in base)+(255,))
target=root/'common/src/main/resources/assets/celestial_nail/textures/entity/celestial_nail.png'
target.parent.mkdir(parents=True,exist_ok=True)
image.save(target)
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
print(target)
