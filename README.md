# M7 Tool-Assissted Speedrun & Practice Map
This is a Minecraft 26.2 server plugin that simulates Master Mode The Catacombs Floor 7 in Hypixel SkyBlock (specifically version 0.24.5 - Assorted QoL Changes, the last version before pearl patch).  Note that the TAS and any TAS-related features are currently disabled due to a client change in Minecraft 26.1 that renders it obsolete.

**This is not a client-side mod and cannot be used on the real Hypixel server, nor is it in any way associated or affiliated with Hypixel.**

A practice server is available at `mc.strad.dev` for Minecraft 26.2

**ADVANCED USERS**: This plugin and map can be locally hosted if you know how to run a PaperMC server.  It can also be built yourself if you know how to get Paper NMS via Maven (the dependency is there, there are a few extra steps).  Stradivarius Violin is not responsible for any issues that may arise from improper building and installation, and does not guarantee that it will work on your machine.  Limited support for self-hosting is available in the [Discord Server](https://discord.gg/gNfPwa8)

## Practice Mode

### Getting Started

Select a class using the `/class` command, then tune its kit with `/m7loadout`.  `/practice` hands you that kit and the corresponding class tag, allowing you to use the class's passives and abilities (if applicable).

The recommended Minecraft game mode is **Adventure Mode**

### Running Practice Mode

Use the `/practice` command with 1-5 players.  By default, this will run all sections.

You can also specify a specific section (`clear` `boss` `maxor` `storm` `goldor` `necron` `witherking`) to practice.

If you are practicing a section that is NOT clear or maxor, the plugin will, by default, teleport you to the spawn location for that section.  You can bypass this by including the `--no-teleport` flag, but beware that it is up to you to start at the correct location!

## Quality of Life

### General

- `/eq` is supported, and will also show your speed
- Your speed automatically assumes Black Cat w/ Unalloyed Speed when equipping Racing Helmet or Cow Hat
- You can shift + left click an item in your inventory to swap it with whatever is in the hotbar slot directly below it.  Exception: items in the right-most column will be swapped with whatever is in your 8th slot

### Verbose Mode

**TIMER Mode (Default)**

Displays the amount of time in ticks and seconds it took for important boss progress to be made.

**VERBOSE Mode (On)**

Also shows what packets are being sent by Fake Players and standard information about how ability results are calculated

**SUPER VERBOSE Mode**

Sends a tick-by-tick replay of most things in addition to adding location data and marking the tick each item was sent

### Main Differences from Hypixel

**Aggro**

Maxor, Storm, and Necron will aggro onto the player that last hit them.  If they are invulnerable, AOTS and Mage Beam count.  If no player has hit the boss yet, it will aggro onto the nearest player.

**Storm**

Lightning will not actually kill you.

**Goldor**

- Goldor has no death ticks.
- Goldor will not chase you around at his maximum speed even if you complete a section early
- Terminals will automatically complete 1 tick after you click on them
- SS: You need to click the button 15 times total (i1 but very generous timing)
- i4: You just need to hit each of the 9 spots at least once with arrows while on the pressure plate

**Wither King**

Dragons will always spawn in this order: `purple` `blue` `orange` `red` `green`

## For Plugin Developers

**SUPER ADVANCED USERS ONLY.**  There is no published artifact to depend on yet, so the supported way in is
reflection: you load M7 TAS's classes by name at runtime and read them through `Method#invoke`, with no
compile-time safety net whatsoever.  A typo in a class name, a field this plugin renames, or an M7 TAS
version older than the one you tested against are all runtime surprises, not compile errors.  Nothing here
is a stable, versioned API: it can change between releases, and it is on you to fail gracefully when it
does.  If that trade doesn't appeal, wait for the proper option below.

M7 TAS exposes a deliberately small API in two halves: an **item catalogue** (what the classes carry, as
real `ItemStack`s) and **custom events** (what just happened in a run).  Both halves are plain Bukkit.  You
do **not** need NMS or `paper-nms` to consume them, even though the plugin itself is full of NMS.

Two principles shape the whole thing:

1. **M7 TAS never calls out.** It fires its events into the void; if nothing is listening, nothing happens.
   The plugin stays fully standalone, so your integration is always optional and can never break a run.
2. **Everything that crosses the plugin boundary is a Bukkit type, a `String`, or JSON.** That means you can
   integrate *without compiling against M7 TAS at all* (see [Hooking in](#hooking-in) below).

### The item catalogue

Every custom item (e.g. Hyperion, Terminator, Infinityboom TNT, Infinileap) is an ordinary `ItemStack` whose
**identity is its first lore line**.  There is no NBT, no persistent-data container, no registry: an item *is*
a Terminator if and only if its first lore line reads `skyblock/combat/terminator`.  The ability listener
dispatches purely on that string, so anything that reproduces the lore line behaves like the real item.

```java
String id = listeners.CustomItems.getID(player.getInventory().getItemInMainHand());
if(id.equals("skyblock/combat/terminator")) {
	// they're holding a Terminator
}
```

`getID` returns `""` for a vanilla item, so it's always safe to call.  The IDs currently in use:

| Namespace | IDs |
|-----------|-----|
| `skyblock/combat/` | `aots` `aotv` `bonzo` `claymore` `dungeonbreaker` `explosive_bow` `flaming_flay` `golem_sword` `gyro` `ice_spray` `infinityboom` `jerrychine` `last_breath` `rag` `scylla` `spring_boots` `stonk` `tac` `terminator` |
| `skyblock/game/` | `energy_crystal` |
| `skyblock/utility/` | `infinileap` |

Two ways to *get* those items:

**In code** (requires compiling against the plugin, route B below):

```java
ItemStack[] kit = plugin.FakePlayerInventory.classLoadoutContents("Mage"); // 41 slots, see layout below
plugin.FakePlayerInventory.applyClassLoadout(player, "Archer");            // the class's DEFAULT kit, ignoring saved edits
loadout.Loadouts.applyFor(player);                                        // the player's SAVED kit + class tag (what /practice does)
```

Valid roles are `Archer`, `Berserk`, `Healer`, `Mage`, `Tank`.

**From disk, with no dependency at all.**  On every enable, `plugin.Catalog` exports the whole catalogue to
`<server>/../data/m7-item-catalog.json`.  M7 TAS is the sole writer of that file;
it is the single source of truth for item definitions and anyone may read it (may change/be configurable later):

```json
{
  "palette":  [ "<base64 ItemStack>", ... ],
  "defaults": { "Archer": [ 41 entries, base64 or null ], "Mage": [ ... ], ... }
}
```

- **`palette`**: every distinct item across all five kits, deduped.  Use it to build an item picker.
- **`defaults`**: each class's default kit as a 41-slot array: `[0..35]` main inventory, `[36]` helmet,
  `[37]` chestplate, `[38]` leggings, `[39]` boots, `[40]` off-hand.  `null` means an empty slot.

Each entry is base64 of Paper's `ItemStack#serializeAsBytes()`, so decoding is two lines and needs nothing
from this plugin:

```java
ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
```

(`plugin.ItemSerial#toB64`/`#fromB64` is that pair of one-liners if you'd rather call them.  Because it's
Paper's own binary format, both sides must run the same Paper build, which they do, since you're reading a
file written by the server you're running on.)

### Custom events

There are two events, both ordinary Bukkit `Event`s.  Neither is `Cancellable` and there's nothing to
override.  They are notifications, not hooks:

- **`plugin.RunCompleteEvent`**: fired the moment a `/practice` run finishes (for Wither-King runs, only
  *after* the death dialogue ends).
- **`plugin.ScoreMilestoneEvent`**: fired MID-RUN, the instant the team reaches a clear-score milestone
  (currently only 300).  That's the point of it: the milestone's time is a real achievement whether or not the
  run is ever finished, since the team can reset straight after hitting 300.  Adds `int score()` on top of the
  same payload.

Both carry a **`plugin.RunResult`**: a snapshot of every fact about the run, deliberately knowing nothing
about leaderboards or categories.  Deciding what a run *qualifies for* is your plugin's business.

| Field | Meaning |
|-------|---------|
| `section` | what `/practice` was invoked with: `all` `clear` `boss` `maxor` `storm` `goldor` `necron` `witherking` |
| `runId` | unique per run, **identical across every report that run makes** (see the dedupe note below) |
| `success` | `false` only for a failed run (enraged Storm with no pillars left) |
| `runTicks` | total run length in server ticks |
| `clearEndTick`, `bloodDoneTick`, `score300Tick`, `fullClearTick` | clear-phase milestones, as overall ticks |
| `teamScore`, `grade` | final score and its letter grade (`S+`, `S`, `A`, …) |
| `phaseDurations` | per-boss durations, relative to that boss's own start, measured to the end of that boss's phase (the tick it chains to the next one), not to its killing blow |
| `splitEnds` | overall tick each section ended: `Clear` `Maxor` `Storm` `Terminals` `Goldor` `Necron` `WitherKing` |
| `participants` | uuid, name, and `stayedAdventure` for everyone still in the run at completion |

Four things to know before you use them:

- **`RunCompleteEvent` fires for failed runs too** (`success == false`).  That's intentional, so a listener
  holding a session or a slot still gets told to let go.  Check `success` before recording anything.
- **Deduplicate on `runId`.**  A run that hits 300 and then finishes reports its `score300Tick` *twice* - once
  live on `ScoreMilestoneEvent`, once again in the final `RunCompleteEvent`.  Both payloads share the same
  `runId`, so ignore a milestone you've already recorded for that id rather than counting it twice.
- **`Integer`/`String` fields are `null` when not reached.**  Every clear milestone is `null` on a boss-only
  practice; `witherKing` is `null` for a run that stopped at Necron.  A score of `null` is not a score of 0.
- All ticks are server ticks (20/s).  "Overall" ticks are relative to the run's t=0; phase durations are
  relative to their own boss's start, and run to the **end of the phase** (after the death dialogue, the tick
  the next boss spawns), not to the killing blow, so they line up with the `splitEnds` deltas.

### Hooking in

**Route A: no dependency (recommended).**  Read `m7-item-catalog.json` for items, and reach the event
reflectively.  The event exposes a single no-arg `String json()` method for exactly this purpose, so the whole
integration is one `Class.forName` and one `getMethod("json")` (you never touch `RunResult` itself):

```java
try {
	Class<? extends Event> ev = (Class<? extends Event>) Class.forName("plugin.RunCompleteEvent");
	getServer().getPluginManager().registerEvent(ev, new Listener() {}, EventPriority.MONITOR,
			(listener, event) -> {
				String json = (String) event.getClass().getMethod("json").invoke(event);
				// parse json with your own DTO; field names match RunResult
			}, this);
} catch(ClassNotFoundException e) {
	// M7 TAS isn't installed on this server; skip the integration
}
```

Register the same way for `plugin.ScoreMilestoneEvent` if you want the live 300 (its extra `int score()` reads
reflectively just like `json()` does).  Add `softdepend: [m7tas]` to your `plugin.yml` so you load *after* M7 TAS
and the class resolves.  This is how
the strad.dev network plugin consumes it, and it means one jar can run on servers with and without M7 TAS.

**Route B: compile against the plugin.**  Put the M7 TAS jar on your compile classpath with `provided`
scope and `depend: [m7tas]` in your `plugin.yml`; at runtime Paper hands you M7 TAS's classes through its
plugin classloader.  You get real types and IDE completion, at the cost of a hard dependency.

The API classes (`RunCompleteEvent`, `RunResult`, `ItemSerial`, `Catalog`, `FakePlayerInventory`) are pure
Bukkit, so `paper-nms` is *not* needed to compile against them, only to build M7 TAS itself.

> **Don't copy `RunCompleteEvent` into your own plugin.**  Bukkit matches listeners by class identity; your
> copy would be a different class from the one M7 TAS fires, and your listener would simply never run.  Depend
> on the jar (route B) or go reflective (route A).

### Proper Artifact When???

Reflection is the recommended approach *for now*.  A proper artifact you can depend on will be ready in
**3-5 business days**.
