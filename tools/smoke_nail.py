"""Live command/fluid regression in the dedicated, disposable portal-smoke world only."""
import socket
import struct
import time
from pathlib import Path

root=Path(__file__).resolve().parents[1]
props=dict(line.split('=',1) for line in (root/'neoforge-1.21.1/run/portal-smoke/server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
assert props['level-name']=='fluid-scale-test', 'Only run against the disposable test world'
sock=socket.create_connection(('127.0.0.1',int(props['rcon.port'])),timeout=10)
def receive(n):
    data=b''
    while len(data)<n:
        part=sock.recv(n-len(data))
        if not part: raise ConnectionError('RCON closed')
        data+=part
    return data
def packet(kind,text):
    payload=struct.pack('<ii',17,kind)+text.encode()+b'\0\0'
    sock.sendall(struct.pack('<i',len(payload))+payload)
    size=struct.unpack('<i',receive(4))[0]
    reply=receive(size)
    assert struct.unpack('<i',reply[:4])[0]!=-1,'RCON authentication failed'
    return reply[8:-2].decode()
packet(3,props['rcon.password'])
log=[]
def cmd(text):
    result=packet(2,text)
    line=text+' -> '+result
    print(line,flush=True);log.append(line)
    return result
def passed(command,marker):
    assert marker in cmd(command), marker
try:
    cmd('forceload add -32 -32 48 48')
    time.sleep(2)
    passed('celestialnail summon default_size 0 230 0', 'height 72.0')
    passed('data get entity @e[type=celestial_nail:celestial_nail,limit=1] Scale','1.0f')
    cmd('celestialnail remove default_size')
    passed('celestialnail summon double_size 0 230 0 16 2','height 144.0')
    cmd('celestialnail remove double_size')
    # Isolated reservoir, including source water, flowing water, lava and a waterlogged fence.
    cmd('fill -18 198 -18 18 215 18 air')
    cmd('fill -18 199 -18 18 199 18 stone')
    cmd('fill -4 200 -4 4 210 4 water')
    cmd('setblock 0 212 0 water[level=4]')
    cmd('setblock 8 201 0 lava')
    cmd('setblock -8 201 0 oak_fence[waterlogged=true]')
    cmd('setblock 22 201 0 water')
    cmd('fill 32 200 32 40 212 40 air')
    passed('celestialnail summon fluid_test 0 216 0 16 0.1','height 7.2000003')
    passed('celestialnail launch fluid_test','still emerging')
    # Real server ticks (not time set), including the entire emergence and fluid settling.
    cmd('tick sprint 180')
    time.sleep(3)
    passed('celestialnail launch fluid_test','Launched Celestial Nail')
    cmd('tick sprint 400')
    time.sleep(5)
    passed('celestialnail list','No Celestial Nails')
    passed('execute if blocks -4 200 -4 4 212 4 32 200 32 all','Test passed')
    passed('execute if block 8 201 0 air','Test passed')
    passed('execute if block -8 201 0 air','Test passed')
    passed('execute if block 22 201 0 water','Test passed')
    print('LIVE SMOKE TEST PASSED',flush=True)
finally:
    (root/'build/portal-smoke-results.log').write_text('\n'.join(log)+'\n')
    cmd('celestialnail remove fluid_test')
    cmd('forceload remove -32 -32 48 48')
    cmd('stop')
    sock.close()
