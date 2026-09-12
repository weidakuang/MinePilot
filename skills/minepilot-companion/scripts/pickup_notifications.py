"""Counted pickup facts for a speech-only decision; never infer a nearby giver."""
import json


def source_facts(source):
    category = source.get('category', 'unknown')
    transfer = source.get('lastTransfer') or {}
    tossed = transfer.get('kind') == 'player_toss'
    actor = transfer if tossed else source
    facts = {'cause': 'player_toss' if tossed else category,
             'actor': {k:actor[k] for k in ('actorId', 'actorName') if k in actor} or None}
    if tossed:
        facts['originCause'] = category
    for key in ('block', 'dimension', 'x', 'y', 'z', 'sourceEntityType', 'reason', 'confidence', 'originCategory', 'damageType', 'killerId', 'killerName'):
        if key in source: facts[key] = source[key]
    if isinstance(source.get('cause'), dict):
        facts['trigger'] = {k:v for k,v in source['cause'].items()
                            if k in ('category','actorId','actorName','block','dimension','x','y','z','reason')}
    return facts


def summarize(events, truncated=False):
    groups = {}
    for event in events:
        for row in event.get('acquired', []):
            amount = row.get('count', 0)
            if type(amount) is not int or amount <= 0: continue
            identity = (row.get('item', 'unknown'), row.get('entryId'))
            group = groups.setdefault(identity, {'item':identity[0], 'count':0, 'sources':{}, 'receiptKinds':set()})
            group['count'] += amount
            group['receiptKinds'].add('ground_pickup' if row.get('entityId') else 'inventory_acquisition')
            source = row.get('source') or {}
            segments = source.get('lineage')
            if not isinstance(segments,list): segments = [{'count':amount, 'source':source}]
            remaining = amount
            for segment in segments:
                count = segment.get('count', 0)
                if type(count) is not int or count<=0: continue
                count = min(count, remaining)
                if count<=0: break
                facts = source_facts(segment.get('source') or {})
                key = json.dumps(facts,sort_keys=True,ensure_ascii=False)
                part = group['sources'].setdefault(key, {'count':0, **facts})
                part['count'] += count
                remaining -= count
            if remaining:
                part = group['sources'].setdefault('uncovered', {'count':0,'cause':'unknown','actor':None,
                                                               'reason':'Origin metadata does not cover these units'})
                part['count'] += remaining
    items = [{**{k:v for k,v in group.items() if k not in ('sources','receiptKinds')},
              'sources':list(group['sources'].values()), 'receiptKinds':sorted(group['receiptKinds'])}
             for group in groups.values()]
    return {'items':items, 'totalCount':sum(row['count'] for row in items),
            'eventSequences':[event['sequence'] for event in events], 'complete':not truncated,
            'meaning':'Already acquired quantities, not current inventory totals. Actor is the observed emitter/dropper, not automatically a giver. A toss is not proof of a gift. Unknown origins remain unknown.'}


def instructions():
    return """You are a Minecraft companion. A batch of items has just entered your inventory.
First decide whether to speak in game chat or stay silent. This turn is only that decision;
do not operate tools, interrupt body work, promise new tasks or ask what to do next.
Use pickupNotice: item registry IDs, acquired counts, counted sources, actor and cause.
The actor of death_drop is the deceased, not necessarily the killer. player_toss proves
who threw the items, not gifting intent. lastTransfer takes precedence for who handed
an item over; originCause describes earlier production. Never invent missing attribution.
Choose silence for routine mining, incidental pickups or already acknowledged items.
A brief natural thank-you can be appropriate for an actual player gift; follow conversation
context and name the right person. Silence is equally valid. Never mechanically list every
item or thank the player for your own mining/death loot. A mixed batch needs at most one
message. To speak, return one complete plain-language reply in the player's language;
to stay silent call minepilot_action with action=wait. No acknowledgement before deciding.
"""
