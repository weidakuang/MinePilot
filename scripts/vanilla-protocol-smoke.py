#!/usr/bin/env python3
"""Offline loopback acceptance with vanilla 26.2 packets and no Forge handshake.

Packet IDs/codecs are from Minecraft 26.2 GameProtocols, LoginProtocols and
ConfigurationProtocols in the pinned Forge 65.0.9 development sources. This is
protocol/command acceptance, not a rendered-client gameplay test. Use only an
isolated offline test server. The probe joins, looks down and marks its target.
"""
import argparse
import io
import json
import select
import socket
import struct
import sys
import time
import uuid
import zlib
from pathlib import Path


def vi(value):
    data=bytearray();value &= 0xffffffff
    while value>127: data.append((value & 127)|128);value >>= 7
    data.append(value);return bytes(data)


def readvi(read):
    value=0
    for n in range(5):
        raw=read(1)
        if not raw: raise EOFError('Connection closed')
        b=raw[0];value|=(b & 127) << (7*n)
        if b<128:return value
    raise ValueError('Invalid VarInt')


def utf(text):
    data=text.encode();return vi(len(data))+data


class Wire:
    def __init__(self,port):
        self.sock=socket.create_connection(('127.0.0.1',port),timeout=8)
        self.compression=-1
    def read(self,n):
        data=bytearray()
        while len(data)<n:
            part=self.sock.recv(n-len(data))
            if not part:raise EOFError('Connection closed')
            data.extend(part)
        return bytes(data)
    def send(self,packet,data=b''):
        payload=vi(packet)+data
        if self.compression>=0:
            payload=vi(len(payload))+zlib.compress(payload) if len(payload)>=self.compression else b'\0'+payload
        self.sock.sendall(vi(len(payload))+payload)
    def receive(self):
        length=readvi(self.read)
        if length>8*1024*1024:raise ValueError('Packet too large')
        data=self.read(length)
        if self.compression>=0:
            buf=io.BytesIO(data);size=readvi(buf.read);data=zlib.decompress(buf.read()) if size else buf.read()
        buf=io.BytesIO(data);return readvi(buf.read),buf
    def hello(self,protocol,intent,port):
        self.send(0,vi(protocol)+utf('localhost')+struct.pack('>H',port)+vi(intent))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port',type=int,required=True)
    parser.add_argument('--profile',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.port==25565:raise ValueError('Use an isolated test port, not the player server')
    name='VanillaProbe'
    status=Wire(args.port);status.hello(-1,1,args.port);status.send(0)
    pid,buf=status.receive();info=json.loads(buf.read(readvi(buf.read)));status.sock.close()
    protocol=info['version']['protocol']
    if info['version']['name']!='26.2':raise ValueError('This probe requires Minecraft 26.2')
    wire=Wire(args.port);wire.hello(protocol,2,args.port)
    wire.send(0,utf(name)+uuid.uuid3(uuid.NAMESPACE_DNS,name).bytes)
    phase='login';joined=False;marked=False;position=False;custom_queries=0;start=time.monotonic();joined_at=0
    try:
        while time.monotonic()-start<25:
            if phase=='play' and joined and position and not marked and time.monotonic()-joined_at>.7:
                wire.send(32,struct.pack('>ffB',0,90,1))
                wire.send(7,utf('minepilot_mark'));marked=True
            if marked and time.monotonic()-joined_at>2:break
            if not select.select([wire.sock],[],[],.1)[0]:continue
            pid,buf=wire.receive()
            if phase=='login':
                if pid==0:raise RuntimeError('Login rejected: '+buf.read()[:200].decode(errors='replace'))
                if pid==1:raise RuntimeError('Probe requires the isolated offline server')
                if pid==3:wire.compression=readvi(buf.read)
                elif pid==4:
                    query=readvi(buf.read);wire.send(2,vi(query)+b'\0');custom_queries+=1
                elif pid==5:
                    key=buf.read(readvi(buf.read)).decode();wire.send(4,utf(key)+b'\0')
                elif pid==2:wire.send(3);phase='configuration'
            elif phase=='configuration':
                if pid==2:raise RuntimeError('Configuration rejected: '+buf.read()[:200].hex())
                if pid==0:
                    key=buf.read(readvi(buf.read)).decode();wire.send(1,utf(key)+b'\0')
                elif pid==14:wire.send(7,b'\0') # no shared registry pack cache
                elif pid==4:wire.send(4,buf.read())
                elif pid==5:wire.send(5,buf.read())
                elif pid==19:wire.send(9)
                elif pid==3:wire.send(3);phase='play'
                # Vanilla ignores unknown plugin/custom payloads.
            elif phase=='play':
                if pid==32:raise RuntimeError('Disconnected in play: '+buf.read()[:200].hex())
                if pid==49:joined=True;joined_at=time.monotonic();wire.send(44)
                elif pid==72:
                    teleport=readvi(buf.read);wire.send(0,vi(teleport));position=True
                elif pid==44:wire.send(28,buf.read())
                elif pid==61:wire.send(45,buf.read())
                elif pid==11:wire.send(11,struct.pack('>f',8))
        if not joined or not marked:raise RuntimeError('Did not reach play and mark; phase='+phase)
        sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'skills/minepilot-companion/scripts'))
        import minepilot
        config=json.loads(args.profile.read_text());client=minepilot.McpClient(config['url'],Path(config['tokenFile']).read_text().strip());client.initialize()
        obs=client.call_tool('observe',{})
        focus=[v for v in obs.get('playerFocus',[]) if v.get('explicitMarker') and v.get('speaker')==name]
        evidence={'version':info['version'],'vanillaHandshake':True,'forgeHandshakeSent':False,'loginCustomQueriesDeclined':custom_queries,
                  'enteredPlay':joined,'teleportAcknowledged':position,'commandSent':'minepilot_mark',
                  'onlinePlayers':obs.get('onlinePlayers',[]),'marker':focus,'elapsedSeconds':time.monotonic()-start}
        args.output.write_text(json.dumps(evidence,indent=2,ensure_ascii=False)+'\n')
        if not focus:raise RuntimeError('Joined but server observation did not confirm marker; inspect evidence')
        print(json.dumps({'enteredPlay':True,'markerConfirmed':True,'kind':focus[0].get('kind'),'seconds':evidence['elapsedSeconds']}))
    finally:wire.sock.close()


if __name__=='__main__':main()
