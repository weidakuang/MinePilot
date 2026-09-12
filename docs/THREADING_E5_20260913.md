# Shared background analysis and E5 deployment controls — 2026-09-13

## Recovery point

Before changing behavior, the complete tracked source/listener/tests and the
installed JAR were backed up to GitHub:
https://github.com/weidakuang/MinePilot/commit/7b9a4f2fb94c1530bd17876608630d26b9bc900b

Branch: `codex/backup-before-threading-20260913`. Remote tree SHA
`cb3e33d4edcfb75d19f80a94ae9b420726061021` exactly matched the local backup tree.
GitHub received no production world or credentials. The backup is the version
BEFORE this optimization. The optimized source, tests and installed JAR are
archived separately on `codex/backup-after-threading-20260913`, under
`artifacts/backups/2026-09-13-after-threading/`.

## Runtime changes

- Route planners, snapshot finalization/threat math, resource section scans and
  excavation planning share a bounded CPU pool. Default: two workers when more
  than three logical processors are available, otherwise one. Idle workers exit
  after 30 seconds. Thread names have increasing suffixes; suffix 4 does not mean
  four workers are active. This is OS-scheduled threading, not core pinning.
- Global waiting queue: 32; per-owner outstanding work: four. Closing/cancelling
  one owner interrupts its work and removes queued work without shutting down
  another owner's worker. Planners cooperate with interruption.
- Resource matching runs on copies of palette sections (at most eight per batch).
  Workers hold no live chunk access path. Main-thread callbacks validate loaded
  state, current block type, exposure and scope before returning candidates.
- Navigation collision reads remain on the server thread. Collection and
  placement approaches now capture in slices instead of synchronously reading
  a whole route volume. Final immutable-map assembly and threat evaluation run
  with A* on the worker. Stale starts and live route obstructions remain checked.
- Resource dispatch/validation, navigation captures, structure records, general
  perception and excavation captures share one cooperative world-read budget.
  Default: 3 ms per tick, not independent 4+3+2 ms budgets. Completed local
  collection planning also records its cost so subsequent slices yield.
- Physics, native breaking/placement, menus, inventory changes, collision/ray
  queries and short tree/target/tool checks still run on the server thread.
  In particular, local tree analysis and explicit collection previews are not
  fully asynchronous. A single native operation or GC can exceed a slice: this
  is not a hard limit on all MinePilot main-thread work or a TPS guarantee.

## JVM controls

Add to Forge's `user_jvm_args.txt` before starting the server:

```
-Dminepilot.analysisThreads=2
-Dminepilot.worldReadBudgetMicros=3000
```

Supported ranges: 1–4 analysis workers, 500–10000 microseconds shared world-read
budget. Increasing workers does not parallelize one A* search, and increasing
read budget trades search latency for main-thread headroom. The tighter setting
`analysisThreads=1`, `worldReadBudgetMicros=2000` also passed the native resource,
water and bank-excavation test. No E5 hardware was available for measurement.
No model, API credential, perception radius, reach, game mode or streaming
configuration was changed. Python remains the external model listener process.

## Validation

- Build passed; 46 Java tests and 128 Python tests passed.
- Native gates passed: player follow/arrival/bridge/pillar, continuous collection,
  placement, all 14 navigation repair scenarios, resource/water and excavation.
- Snapshot isolation test changes the real block after copying its section;
  `minepilot-analysis-1` still reads the original owned copy. Public resource
  queries reject the subsequently removed candidate. Worker tests verify
  cancellation interrupt, owner isolation and bounded queue behavior.
- Accelerated resource/water gate, default two workers / 3 ms budget: measured
  shared-read peak 3.57 ms. One worker / 2 ms budget: peak 2.68 ms. These are
  scoped world-read timings on the current machine, not total tick timings,
  worst-case bounds, E5 numbers or before/after benchmarks.
- Normal-speed installed production smoke: a radius-150 wood query returned 16
  candidates in 188.58 ms. `coverageComplete=false`: enough nearby candidates
  were found; this did not exhaustively scan the whole sphere. Afterwards 40
  ticks elapsed over 2.008 seconds, health remained 20 and navigation stayed idle.
  Tracked shared-read peak was 3.13 ms. No test blocks or items were injected.
- A live JVM dump confirmed two `minepilot-analysis-*` worker threads; the
  DeepSeek listener reconnected, streaming to server only, complete player chat.

## Deployment

Installed JAR SHA-256: `185ecd16808201b1d99555ebbb6c20fefb904149ff8354d27bb8398b2ab93dc0`.

Stopped-world recovery backup: `/Users/weida/Documents/minecraft-ai-companion-forge/.minepilot-backup/threading-1789228874/production`.
