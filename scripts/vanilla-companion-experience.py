#!/usr/bin/env python3
"""Ten-minute protocol-only vanilla-client conversation in an isolated normal-tick world.

The listener is the sole AI controller. This probe only sends ordinary player
packets and observes public tools. Optional FIFO commands position/equip the
test player, never the companion. No terrain, time, AI inventory or health edits.
"""
import argparse
import importlib.util
import json
import math
import os
from pathlib import Path
import select
import struct
import sys
import time
import uuid

spec=importlib.util.spec_from_file_location('vanilla_wire',Path(__file__).with_name('vanilla-protocol-smoke.py'))
wirelib=importlib.util.module_from_spec(spec);spec.loader.exec_module(wirelib)
Wire,vi,utf,readvi=wirelib.Wire,wirelib.vi,wirelib.utf,wirelib.readvi

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--port',type=int,required=True);p.add_argument('--profile',type=Path,required=True);p.add_argument('--directory',type=Path,required=True);p.add_argument('--duration',type=int,default=610);p.add_argument('--output',type=Path,required=True);p.add_argument('--scenario',choices=['full','final-repairs'],default='full');args=p.parse_args()
    if args.port==25565 or args.directory.resolve().name!='run-takeover-natural-20260910':raise ValueError('Use the isolated copied-world server')
    sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'skills/minepilot-companion/scripts'));import minepilot
    config=json.loads(args.profile.read_text());client=minepilot.McpClient(config['url'],Path(config['tokenFile']).read_text().strip());client.initialize()
    name='VanillaProbe';wire=Wire(args.port);wire.hello(776,2,args.port);wire.send(0,utf(name)+uuid.uuid3(uuid.NAMESPACE_DNS,name).bytes)
    phase='login';joined=None;position=False;sequence=0;next_poll=0;events=[];observations=[];chats=[];sent=set();start=time.monotonic()
    script=[(4,'你好，杰克雷。我们一起生存，先把缺的圆石补够，做熔炉烧一块木炭并捡回。'),
            (35,'我喜欢在河边扎营，你记住这个。继续干活吧，不用每一步问我。'),
            (95,'你现在忙什么呢？我们以后一起探索村庄。'),
            (180,'这几个我刚丢给你的东西是什么？'),
            (250,'你继续刚才的生存任务，做完了就自己准备基本工具。'),
            (330,'停下'),(345,'放一个工作台'),
            (410,'把工作台收回来，挖完记得捡上。'),
            (480,'去找四块木头吧，你自己找。'),
            (565,'停下'),(580,'我刚才说喜欢在哪里扎营？')]
    if args.scenario=='final-repairs':
        script=[(6,'你看看我现在手里拿的是什么？'),(25,'把工作台收回来，挖完记得捡上。'),(115,'停下'),(125,'放一个工作台'),(142,'把工作台收回来，挖完记得捡上。'),(165,'去找四块木头吧，你自己找。'),(270,'停下'),(283,'我喜欢在哪里扎营？')]
    def console(command):
        fd=os.open(args.directory/'server-console.fifo',os.O_WRONLY|os.O_NONBLOCK)
        try:os.write(fd,(command+'\n').encode())
        finally:os.close(fd)
    def place_probe():
        o=client.call_tool('observe',{});candidates=client.call_tool('sense',{'kind':'standing_positions'}).get('results',[])
        pos=candidates[0] if candidates else {'x':o['x'],'y':o['y']+2.5,'z':o['z']}
        console('tp '+name+' '+str(pos['x'])+' '+str(pos['y'])+' '+str(pos['z']))
        return pos,o
    def persist():
        args.output.write_text(json.dumps({'protocol':776,'vanillaHandshake':True,'forgeHandshakeSent':False,'renderedClient':False,'probeMode':'creative (protocol probe has no client physics); companion remains survival','scenario':args.scenario,'durationSeconds':time.monotonic()-start,'events':events,'chat':chats,'observations':observations},ensure_ascii=False,indent=2)+'\n')
    try:
        while time.monotonic()-start<args.duration+30:
            now=time.monotonic();elapsed=0 if joined is None else now-joined
            if joined is not None and elapsed>args.duration:break
            if phase=='play' and joined is not None and position:
                if 'position' not in sent:
                    console('gamemode creative '+name);place_probe();sent.add('position');events.append({'at':elapsed,'kind':'position_test_player_only'})
                if elapsed>2 and 'mark' not in sent:
                    wire.send(32,struct.pack('>ffB',0,90,1));wire.send(7,utf('minepilot_mark'));sent.add('mark');events.append({'at':elapsed,'kind':'mark'})
                if elapsed>(1 if args.scenario=='final-repairs' else 170) and 'gift_setup' not in sent:
                    gift_pos,gift_body=place_probe();console('clear '+name);console('give '+name+' minecraft:emerald 3');sent.add('gift_setup')
                if args.scenario=='full' and elapsed>174 and 'gift' not in sent:
                    dx=gift_body['x']-gift_pos['x'];dz=gift_body['z']-gift_pos['z'];yaw=math.degrees(math.atan2(-dx,dz));pitch=math.degrees(math.atan2(gift_pos['y']-gift_body['y']+1, max(.01,math.hypot(dx,dz))))
                    wire.send(32,struct.pack('>ffB',yaw,pitch,1));wire.send(53,struct.pack('>h',0));wire.send(41,vi(3)+struct.pack('>qB',0,0)+vi(1));sent.add('gift');events.append({'at':elapsed,'kind':'vanilla_player_toss','item':'emerald','count':3})
                if elapsed>(18 if args.scenario=='final-repairs' else 400) and 'clear_work_area' not in sent:
                    o=client.call_tool('observe',{});console('tp '+name+' '+str(o['x'])+' '+str(o['y']+4)+' '+str(o['z']));sent.add('clear_work_area');events.append({'at':elapsed,'kind':'move_probe_above_work_area_to_avoid_stealing_drops'})
                for index,(at,text) in enumerate(script):
                    if elapsed>=at and index not in sent:
                        emitted=time.monotonic();wire.send(9,utf(text)+struct.pack('>qq',int(time.time()*1000),0)+b'\0'+vi(0)+b'\0\0\0\0');sent.add(index);events.append({'at':elapsed,'kind':'chat_sent','text':text,'wallAt':time.time()});print(json.dumps(events[-1],ensure_ascii=False),flush=True)
                        if text=='停下':
                            while time.monotonic()-emitted<5:
                                stopped=client.call_tool('observe',{})
                                if stopped.get('autonomy',{}).get('paused') and all(not stopped.get(k,{}).get('ownsBody') for k in ('gather','camp','survival','placement','collection','excavation')) and stopped.get('navigation',{}).get('phase') in ('IDLE','CANCELLED','COMPLETED','FAILED'):
                                    events.append({'at':elapsed,'kind':'stop_verified','milliseconds':round((time.monotonic()-emitted)*1000,2),'serverTick':stopped.get('serverTick')});break
                                time.sleep(.02)
                if now>=next_poll:
                    next_poll=now+1
                    read=client.call_tool('read_chat',{'after_sequence':0,'after_system_sequence':sequence,'limit':50});system=read.get('systemChat',{})
                    for row in system.get('messages',[]):chats.append({'at':elapsed,**row});sequence=max(sequence,row['sequence'])
                    o=client.call_tool('observe',{})
                    observations.append({'at':elapsed,**{k:o.get(k) for k in ('serverTick','x','y','z','health','food','gather','survival','camp','placement','collection','navigation','autonomy','playerFocus')},'inventory':[(row['item'],row['count']) for row in o['inventorySummary']['entries']],'conversationMemory':o.get('conversationMemory')})
                    # Detailed receipts remain in public tools; keep the chronological record bounded.
                    for kind in ('placement','collection','navigation'):
                        if isinstance(observations[-1].get(kind),dict):observations[-1][kind]={k:v for k,v in observations[-1][kind].items() if k not in ('receipts','pickupReceipts','routeOptions','options')}
                    persist()
            if not select.select([wire.sock],[],[],.05)[0]:continue
            packet,buf=wire.receive()
            if phase=='login':
                if packet in (0,1):raise RuntimeError('Login rejected/encryption required')
                if packet==3:wire.compression=readvi(buf.read)
                elif packet==4:wire.send(2,vi(readvi(buf.read))+b'\0')
                elif packet==5:key=buf.read(readvi(buf.read)).decode();wire.send(4,utf(key)+b'\0')
                elif packet==2:wire.send(3);phase='configuration'
            elif phase=='configuration':
                if packet==2:raise RuntimeError('Configuration rejected')
                if packet==0:key=buf.read(readvi(buf.read)).decode();wire.send(1,utf(key)+b'\0')
                elif packet==14:wire.send(7,b'\0')
                elif packet in (4,5):wire.send(packet,buf.read())
                elif packet==19:wire.send(9)
                elif packet==3:wire.send(3);phase='play'
            else:
                if packet==32:raise RuntimeError('Play disconnect: '+buf.read()[:300].hex())
                if packet==49:joined=time.monotonic();wire.send(44);wire.send(12,vi(0))
                elif packet==72:wire.send(0,vi(readvi(buf.read)));position=True
                elif packet==44:wire.send(28,buf.read())
                elif packet==61:wire.send(45,buf.read())
                elif packet==11:wire.send(11,struct.pack('>f',8))
        if joined is None:raise RuntimeError('Never entered play')
        persist();print(json.dumps({'completed':True,'seconds':time.monotonic()-joined,'messagesSent':len([e for e in events if e['kind']=='chat_sent'])}),flush=True)
    finally:persist();wire.sock.close()

if __name__=='__main__':main()
