"""Exact, unambiguous short controls; other language stays with the model."""
import re

def parse(text):
    text = text.strip().rstrip('。！!')
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
