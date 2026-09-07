#!/usr/bin/env python3
"""Extract subtitle/range metadata from the user's pinned vanilla asset cache; no audio is copied."""
import argparse, hashlib, json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--assets',type=Path,required=True);p.add_argument('--version-json',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
version=json.loads(a.version_json.read_text());index_info=version['assetIndex'];index=a.assets/'indexes'/(index_info['id']+'.json')
assert hashlib.sha1(index.read_bytes()).hexdigest()==index_info['sha1'], 'Version asset index mismatch'
objects=json.loads(index.read_text())['objects']
def asset(name):
 meta=objects[name];raw=(a.assets/'objects'/meta['hash'][:2]/meta['hash']).read_bytes();assert hashlib.sha1(raw).hexdigest()==meta['hash'];return json.loads(raw)
sounds=asset('minecraft/sounds.json');zh=asset('minecraft/lang/zh_cn.json');events={}
for name,data in sounds.items():
 variants=[]
 for sound in data.get('sounds',[]):
  sound={'name':sound} if isinstance(sound,str) else sound
  volume=sound.get('volume',1);assert isinstance(volume,(float,int))
  if sound.get('type')=='event':variants.append({'event':sound['name'].removeprefix('minecraft:'),'volume':volume})
  else:
   sound_path=sound['name'].removeprefix('minecraft:')
   if 'minecraft/sounds/'+sound_path+'.ogg' not in objects:continue
   variants.append({'weight':sound.get('weight',1),'volume':volume,'distance':sound.get('attenuation_distance',16)})
 events[name]={'variants':variants}
 if 'subtitle' in data:events[name]['subtitle']=data['subtitle']
keys={v['subtitle'] for v in events.values() if 'subtitle' in v}
text={k:v for k,v in zh.items() if k in keys or k.startswith(('entity.minecraft.','block.minecraft.'))}
result={'minecraft':version['id'],'assetIndexSha1':index_info['sha1'],'soundsSha1':objects['minecraft/sounds.json']['hash'],'languageSha1':objects['minecraft/lang/zh_cn.json']['hash'],'events':events,'zh_cn':text}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n')
print('Wrote',len(events),'sound definitions and',len(text),'short native labels to',a.output)
