"""Exact, unambiguous short controls; other language stays with the model."""
import re
import json

def parse(text):
    text = text.strip().rstrip('。！!')
    if text in {'把工作台收回来','把工作台收回','收回工作台','把工作台收回来，挖完记得捡上'}:
        return {'action':'tool','tool_name':'collect',
                'arguments_json':json.dumps({'resource':'minecraft:crafting_table','output_item':'minecraft:crafting_table','source':'blocks','count':1,'radius':10}),
                'message':'好，我把工作台收回来。'}
    if re.fullmatch(r'(?:请)?(?:你)?(?:放下|放置|放)(?:一个|个)?工作台', text) or text in {'把工作台放下', '把工作台放地上', '把工作台放到地上'}:
        return {'action':'tool', 'tool_name':'place_block',
                'arguments_json':json.dumps({'item':'minecraft:crafting_table'}),
                'message':'好，我把工作台放下。'}
    if text in {'让开', '让一下', '让开一下', '让开，我来砍', '让开，我来挖', '让开吧', '让开，我来'}:
        return {'action':'yield', 'player_intent':text}
    if text in {'跳一下', '跳一次', 'jump once'}: return {'action':'jump'}
    match = re.fullmatch(r'(?:往|向)?(前|后)(?:走|移动)([一二两三四五六七八1-8])格', text)
    if match:
        direction, value = match.groups()
        count = int(value) if value.isdigit() else {'一':1,'二':2,'两':2,'三':3,'四':4,'五':5,'六':6,'七':7,'八':8}[value]
        return {'action':'navigate','target_kind':'coordinates','forward_blocks':count*(1 if direction=='前' else -1),
                'pace':'walk','message':f'好，向{direction}走{count}格。'}
    match = re.fullmatch(r'(?:朝向|转向)(\d{1,3}(?:\.\d+)?)度',text)
    if match and 0 <= float(match[1]) <= 360:
        return {'action':'tool','tool_name':'turn','arguments_json':'{"heading":'+match[1]+'}'}
    return None


def resolve_yield(client, decision, speaker):
    """Choose only a publicly observed free corridor, away from the current speaker."""
    observation = client.call_tool('observe', {})
    candidates = client.call_tool('sense', {'kind':'standing_positions'}).get('results', [])
    if not candidates:
        return None  # The model may inspect slopes or a longer detour.
    players = [p for p in observation.get('onlinePlayers', []) if p.get('name') == speaker]
    target = players[0] if len(players) == 1 else None
    if target and isinstance(target.get('position'), dict): target = target['position']
    if target and all(k in target for k in ('x', 'y', 'z')):
        candidates = sorted(candidates, key=lambda p: sum((p[k]-target[k])**2 for k in ('x','z')), reverse=True)
    chosen = candidates[0]
    value = {'action':'navigate', 'target_kind':'coordinates', 'pace':'walk',
             'message':'好，我让到旁边。', 'player_intent':decision['player_intent'],
             'acceptance_radius':.5, **{k:chosen[k] for k in ('x','y','z')}}
    current = client.call_tool('navigation_status', {})
    if current.get('requestId') and current.get('phase') not in {'IDLE','COMPLETED','APPROACHED','FAILED','CANCELLED'}:
        value['replace_request_id'] = current['requestId']
    return value
