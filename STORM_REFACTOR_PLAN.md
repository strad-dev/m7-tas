# Storm Boss Refactor — Pre-Work Analysis

Research notes for tomorrow's Storm-mechanics implementation. No code yet.

---

## 1. Existing Storm pillar behavior

### Raw constants
- `CLONE_DELAY = 4` ticks (between consecutive clone commands)
- `AIR_START_TICK = 48`, `AIR_DELAY = 4`
- `CLONE_RESUME_TICK = 148`
- `PURPLE_PAD = (x1=103, x2=97, z1=68, z2=62)` — 7×7 horizontal footprint
- `YELLOW_PAD = (x1=49,  x2=43, z1=68, z2=62)` — 7×7 horizontal footprint

(Worth re-confirming: x1..x2 inclusive over 7 blocks = 49 columns per layer. Currently a `record PadConfig` holds only the corners.)

### `prepadSequence(pad)` — total span 0..176 ticks
Three phases:
1. **Drop 6 (tick 0..20):** clone y=175→174, 174→173, … 170→169. Six commands at delay 0,4,8,12,16,20.
2. **Clear 20 air (tick 48..124):** fill y=169..188 with air, one layer every 4 ticks.
3. **Drop 8 (tick 148..176):** clone y=189→188, 188→187, … 182→181. Eight commands at delay 148,152,…,176.

### `crushSequence(pad)` — total span 0..16 ticks
- **Drop 5 (tick 0..16):** clone y=181→180, 180→179, … 177→176. Five commands at delay 0,4,8,12,16.

### Sound coupling
Every scheduled clone/air command also fires `BLOCK_PISTON_CONTRACT` via `playPistonSound()` regardless of whether it's a clone or air fill. Likely fine but worth noting if we extract a generic primitive.

---

## 2. Generalization

Reduce all four phases to one **primitive op**: "drop the pillar one block." Concretely, a single clone command of the form
```
clone x1 srcY z1 x2 srcY z2 x2 (srcY-1) z2
```

Compose ops into **two operators**:

| Operator | Definition | Phases that use it |
|----------|------------|--------------------|
| `dropStack(pad, topY, count, delay)` | Issue `count` clones at delay×i ticks, each peeling one layer off the top of the column down (srcY = topY − i, dstY = srcY − 1). | prepad phase 1 (topY=175, count=6, delay=4), prepad phase 3 (topY=189, count=8, delay=4), crush (topY=181, count=5, delay=4). |
| `clearColumn(pad, yStart, yEnd, delay, startTick)` | Fill air one layer per `delay` ticks. | prepad phase 2 (169..188, delay=4, startTick=48). |

`prepad` then becomes a 3-call recipe; `crush` is a single `dropStack` call.

### Sketch (NO CODE YET — for tomorrow)
```
prepad(pad):
    dropStack(pad, topY=175, count=6, delay=4, startTick=0)
    clearColumn(pad, 169, 188, delay=4, startTick=48)
    dropStack(pad, topY=189, count=8, delay=4, startTick=148)

crush(pad):
    dropStack(pad, topY=181, count=5, delay=4, startTick=0)
```

This collapses ~50 lines of duplicated clone-command building into two helpers and four recipe calls.

---

## 3. 20-tick cycle mapping

CLONE_DELAY=4 means **5 commands per 20-tick (1-second) cycle**. Mapping the phases to whole cycles:

| Phase | Ticks | Cycles |
|-------|-------|--------|
| prepad drop-6 | 0..20 | 1.0 |
| (idle gap) | 20..48 | 1.4 |
| prepad air-clear (20 layers) | 48..124 | 3.8 |
| (idle gap) | 124..148 | 1.2 |
| prepad drop-8 | 148..176 | 1.4 |
| **prepad total** | **0..176** | **~8.8** |
| crush drop-5 | 0..16 | **0.8** |

**Observations once we're cycle-aligned:**
- Crush is essentially the unit op: a pillar falls 5 blocks (one "phase tick" of natural pillar speed) per 20-tick cycle.
- The two idle gaps in prepad (20..48 and 124..148) are dead time. Worth checking whether real Hypixel actually idles or whether the original implementation just left padding for some unrelated reason.
- Prepad fits naturally into "9 cycles" if we straighten the gaps.

### Why this matters for tomorrow
Most boss/global timings in this codebase are multiples of 20 (Necron frenzy=140, Storm initial-leg=100, Maxor dialogue=60, etc.). If pillar primitives are parameterized in cycles rather than raw ticks, scheduling on top of the boss timeline gets cleaner — `prepad(pad).startAt(cycle=N)` instead of `startAt(tick=20*N)`.

---

## 4. Boss movement patterns (for the interface design)

### Storm — fully scripted force-moves
- `initialMovement()`: 4 legs × 100 ticks (5 cycles each) with `Actions.turnHead` + `Actions.forceMove`. Traces a square.
- Mid-fight: more `forceMove` at tick 665 (fall onto pad), tick 694 (enrage push).

### Goldor — fully scripted force-moves
- 3 legs: (0.1,0,0)×200, (0,0,0.1)×38, (-0.79,0,-0.07)×52. No pivoting beyond `turnHead`.

### Maxor — live aggro chase
- `WitherActions.setWitherAggro(maxor, Tank.get(), 3.0, 1.0)` — runs the per-tick chase math (`vx`/`vy`/`vz` PD-like controller, `noPhysics=true`, manual `move()`, `lookAt`).
- During stun: `clearWitherAggro` ; on enrage: `setWitherAggro` again.

### Necron — stationary
- No movement at all. Only `destroyPlatform()` fireballs + frenzy sounds.

### WitherKing — stationary
- 5 HP, decremented one per dragon kill. Movement nil.

### Pattern: two movement "modes"
1. **Scripted**: list of `(headYaw, headPitch, velocity, durationTicks)` segments → drives `Actions.turnHead` + `Actions.forceMove`.
2. **Live aggro**: a `LivingEntity target` + `(stopDistance, yOffset)` tuple → `WitherActions.setWitherAggro`.

Both fit behind a `MovementController` abstraction (one impl per mode). Necron/WK use a no-op controller.

---

## 5. Mob spawning patterns

### Common per-entity setup (Maxor miners, Storm miners, Storm guards)
Every wither-skeleton spawn does the same 7 steps:
1. `world.spawnEntity(loc, WITHER_SKELETON)`
2. MAX_HEALTH=4, ARMOR=-30, ARMOR_TOUGHNESS=-20
3. `setHealth(4)`
4. Equipment in main hand (pickaxe / bow)
5. Custom name `"<Role> 6M❤"`, `setCustomNameVisible(true)`
6. Compute yaw/pitch toward fixed target (73.5, Y, 53.5) + teleport
7. NMS `setAggressive(true)` for raised-arms pose
8. Stash in `WitherSkeleton[]` array

Maxor skips steps 4–7's `setAggressive` (his miners' AI is off, no arms raised), and Maxor uses `setAI(false)` while Storm's *miners* don't (so they wander).

### Storm-specific mob groups
- **44 Wither Guards** at fixed pillar-sentry positions (`sentryLocations`). AI off, bow.
- **200 Wither Miners** = 20 clusters × 10 random offsets within ±5 blocks of cluster center (`minerCenters`). AI on. Stone pickaxe.
- **4 Shadow Assassins** (zombies) at corner spawns, invisible via potion effect, AI off, sword.

### Generalization
A `MobSpawnSpec` record / builder:
```
entityType, count, locationProvider, hp, armor, armorToughness,
mainHand, customName, aiEnabled, aggressive,
facingTarget (Location|null), potionEffects, ...
```
Plus a `MobGroup` that owns the resulting array and offers `cleanup()`.

This eats Maxor's `spawnMiners()`, Storm's miner loop, Storm's guard loop, and the shadow-assassin loop. Per the memory note `project_storm_spawn_timing`, the real-server staggers cluster spawns — so the spawner should accept `startTick` per spec (default 0).

---

## 6. Shared Wither boss boilerplate (target of the interface)

Every Wither boss does this prefix verbatim:
```
1. if(boss != null) boss.remove();
2. tear down old BossBar + bossBarUpdateTask
3. spawnEntity Wither at <spawnLoc>
4. setAI(false), setSilent(true), setPersistent(true), setRemoveWhenFarAway(false)
5. setCustomName "﴾ <Name> ﴿ <HP>M❤"
6. MAX_HEALTH=<n>, ARMOR=-30, ARMOR_TOUGHNESS=-20
7. setHealth(<n>)
8. addScoreboardTag("TASWither") + addScoreboardTag("TAS<Name>")
9. WitherActions.setWitherArmor(boss, true)
10. scheduleTask(() -> CustomBossBar.setupWitherBossBar(boss, "<Name>"), 1)
```
Plus the duplicated `sendChatMessage()` helper (only the prefix `"[BOSS] <Name>: "` differs across the 4 files — Wither-King also wraps with MAGIC).

`formatTick()` is duplicated in Maxor (and needed elsewhere) — depends on a per-boss `PRE_<NAME>_TICKS` offset.

`cleanupMobs()` pattern (iterate array, null-check, isValid check, remove) appears 3 times in Storm and once in Maxor.

---

## 7. Proposed `WitherLord` design (sketch for discussion tomorrow)

Two viable shapes:

### (a) Abstract base class — preferred
```
abstract class WitherLord {
    protected Wither boss;
    protected World world;
    protected int tick;        // own per-fight ticker
    protected boolean dying, doContinue;
    protected final List<MobGroup> mobGroups = new ArrayList<>();

    // template method
    public final void start(World w, boolean doContinue) {
        cleanup();
        spawn();          // shared boilerplate (steps 1-10)
        onStart();        // hook: schedule dialogue, movement, mobs
        tickerTask = ...; // auto-increment tick
    }

    // shared helpers
    protected final void sendChatMessage(String msg) { ... }
    protected final String formatTick(int t) { ... uses previousTicks() ... }
    protected final void setArmor(boolean on) { WitherActions.setWitherArmor(boss, on); }
    protected final void setAggro(LivingEntity tgt, double sd, double yo) { ... }
    protected final void cleanupMobGroups() { mobGroups.forEach(MobGroup::cleanup); }

    // abstract config
    protected abstract String name();
    protected abstract Location spawnLocation();
    protected abstract double maxHealth();
    protected abstract int previousTicks();
    protected abstract void onStart();
    protected abstract void onDeath();
}
```
Concrete: `Maxor extends WitherLord`, `Storm extends WitherLord`, `Goldor extends WitherLord`, `Necron extends WitherLord`, `WitherKing extends WitherLord`. WitherKing overrides name-formatting (MAGIC wrap).

### (b) Sealed interface + records — fancier but loses access to mutable per-fight state
Not as good a fit because every boss has substantial mutable state (`stunDamageDealt`, `platesActive`, `pendingPlateChecks`, the `EnderCrystal` slots, the laser scan task). An abstract class holds that naturally.

### Pillar/Mob abstractions live in their own modules
```
class Pillar(World, Region region, World world)
    void dropStack(int topY, int count, int delayTicks, int startTick)
    void clearColumn(int yStart, int yEnd, int delayTicks, int startTick)
    void prepad()    // 3-recipe call above
    void crush()     // 1-recipe call above

class MobGroup(MobSpawnSpec spec)
    void spawn()
    void cleanup()
```
These are *used by* `Storm extends WitherLord`, not part of `WitherLord` itself (only Storm uses pillars).

---

## 8. Things to confirm with user tomorrow before writing code

1. **Pad footprint:** purple `(103..97, 68..62)` is 7×7 inclusive — confirm intent (not 6×6).
2. **Real prepad gaps:** are tick 20..48 and 124..148 idle gaps physically meaningful, or padding to be cleaned up?
3. **Storm cluster spawn timings:** the memory note says we need observed per-cluster delays — does the user have them yet?
4. **WitherKing in or out of WitherLord?** It has a 5 HP placeholder and dragon-based "damage." Mostly fits, but death sequence is uniquely the end-of-run scoreboard.
5. **Refactor scope:** lift only the Wither boilerplate first, or also pillars + mobs in one PR? Recommend phased: WitherLord base first (purely mechanical), then Storm-specific Pillar/MobGroup extractions.
