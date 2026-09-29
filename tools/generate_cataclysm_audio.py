"""Deterministic original sound design; no external recordings. Requires numpy and ffmpeg."""
from pathlib import Path
import subprocess, wave
import numpy as np
RATE=24000
out=Path(__file__).resolve().parents[1]/'common/src/main/resources/assets/celestial_nail/sounds'
rng=np.random.default_rng(729)
for name,seconds in [('presence',12),('sky_crack',4),('crystal_creak',3),('launch_charge',1.5),('impact_boom',9),('aftershock',7)]:
 t=np.arange(int(RATE*seconds))/RATE
 noise=rng.normal(0,1,len(t))
 low=np.convolve(noise,np.ones(160)/160,mode='same')
 if name=='presence':
  signal=.22*np.sin(2*np.pi*40*t)+.14*np.sin(2*np.pi*60*t)+.07*np.sin(2*np.pi*121*t)*(1+np.sin(2*np.pi*t/6))+.5*low
  env=np.minimum(1,t/.1)*np.minimum(1,(seconds-t)/.1)
 elif name=='launch_charge':
  signal=.4*np.sin(2*np.pi*(45*t+90*t*t))+.3*low+.04*noise
  env=np.minimum(1,t/.15)*np.clip((1.2-t)/.15,0,1)
 elif name=='crystal_creak':
  signal=sum(np.sin(2*np.pi*f*t+4*np.sin(t*2))*a for f,a in [(420,.1),(631,.07),(947,.04)])+.2*low
  env=np.sin(np.pi*t/seconds)**2
 else:
  signal=.45*np.sin(2*np.pi*(35*t+18*(1-np.exp(-t*3))))+1.8*low+.07*noise*np.exp(-t*8)
  env=np.minimum(1,t/.015)*np.exp(-t/(seconds*.26))*np.minimum(1,(seconds-t)/.2)
 signal=np.tanh(signal*env)*.85
 wav=out/(name+'.wav')
 with wave.open(str(wav),'wb') as f:
  f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE);f.writeframes((signal*32767).astype('<i2').tobytes())
 subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','3',str(out/(name+'.ogg'))],check=True)
 wav.unlink()
