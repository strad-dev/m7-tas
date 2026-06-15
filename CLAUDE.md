# M7TAS - Master Mode Floor 7 Tool-Assisted Speedrun Plugin

## Overview
Spigot-NMS plugin (Java 21, Minecraft 1.21.11) that simulates Hypixel SkyBlock Master Mode Floor 7 dungeon runs using server-side fake players. Fake players are real `ServerPlayer` instances with dummy network connections — NOT Citizens NPCs.
This project aims to find the fastest theoretical limit to this floor while sending packets theoretically possible with a vanilla client.  The plugin is run LOCALLY - it is NOT the real Hypixel server and you can safely disregard Hypixel anticheat concerns (although you should mimic real client behaviour as closely as possible).

In the current refactor, Berserk.java, Tank.java, and Healer.java are all cosplaying the Mage class (running on players named Mage3, Mage2, and Mage4, respectively — all of which use the Mage inventory).  Berserk.java executes the clear tasks previously done by the Healer (Wizard, Well, Ice Fill rooms) on the Mage3 player.  Tank.java executes Tank tasks on the Mage2 player.  Healer.java runs on the Mage4 player.  Mage.java itself runs on Mage1.  Only Archer.java runs on a player ("Archer") with its own distinct inventory.

## Architecture

### Fake Players
- Spawned as real `ServerPlayer` with `GameProfile`, dummy `Connection(PacketFlow.SERVERBOUND)` and a custom `TASGamePacketListenerImpl`
- Added to the server's `PlayerList` so Bukkit sees them as `Player`
- No real network connection — packets are simulated server-side, not sent over the wire
- Skin layers set via `Player.DATA_PLAYER_MODE_CUSTOMISATION` with `(byte) 0x7F`
- Fake player name → role mapping: `"Archer"` = Archer, `"Mage1"` = Mage, `"Mage2"` = Tank (Mage class), `"Mage3"` = Berserk (Mage class), `"Mage4"` = Healer (Mage class). Only `"Archer"` has its own inventory; all four `MageN` players share the Mage inventory (the `"Healer"`/`"Berserk"`/`"Tank"` cases in `FakePlayerInventory.setInventories()` are dead code — no fake player has those names)

### Fake Player Lifecycle
- `startFakePlayerTicker()` runs a repeating task every tick that calls `setNoGravity(false)`, `updatePlayerPose()` (via reflection), and `aiStep()` on each fake player to keep them physically simulated
- `forceCustomConnection()` runs a separate repeating task every tick that re-assigns `serverPlayer.connection = TASGamePacketListenerImpl` — this is necessary because the server periodically overwrites it with a vanilla connection

### Boss Tick Ordering (INVARIANT)
- **All boss actions MUST run at the start of the tick — before any player choreography.** This is what lets a player's mage-beam/melee hit on tick T read the boss's already-finalized state (stun/enrage/HP/position) for that same tick, instead of the previous tick's. It removes the need for the +1-tick offsets that previously forced player hits to wait a tick for the boss.
- Enforced by a single **boss-priority heartbeat** in `BossScheduler` (`BossScheduler.start()`, called first in `M7tas.onEnable`). Because it is created at plugin-enable time, its Bukkit scheduler task id is the lowest of anything created at runtime, and CraftBukkit runs same-tick tasks in task-id order — so every registered boss ticker runs before per-run choreography (scheduled only when `/tas` runs).
- Per-tick boss simulation registers via `BossScheduler.addTicker(Runnable)` / `removeTicker(...)` and runs in registration order (so a boss that needs "move, then scan" must register the mover first). Already migrated: the shared aggro mover (`WitherActions`), Maxor's laser/stun scan, Storm's pad/crush poll (an internal counter preserves its 20-tick cadence). Necron is HP-threshold/event-driven (no per-tick scan). **Any new boss mechanic that mutates state a player hit reads must be registered as a boss ticker, NOT scheduled with its own `runTaskTimer`/`Utils.scheduleTask`.**
- **Timed boss state changes** (stun→enrage, immunity/cooldown windows ending, interlude ends) use `BossScheduler.schedule(Runnable, delayTicks)` — a boss-lane one-shot that fires at the START of the target tick — NOT `runTaskLater`/`Utils.scheduleTask` (which fire mid-tick, after the beam). Invariant example: Maxor enters the laser at the start of tick 195 → `BossScheduler.schedule(enrage, 160)` fires enrage at the start of tick 355, so a beam on tick 355 sees the re-armored boss. Delay semantics are identical whether scheduled from a boss ticker or from a damage handler (`bossTick` advances at the start of the heartbeat). Migrated: Maxor enrage + stun-cooldown reset, Storm enrage, Necron interlude end.
- Note: this only orders scheduler tasks. Entity-physics events (e.g. arrow `ProjectileHitEvent`) and the `WitherLord.tick++` counter are outside the heartbeat — see `WitherLord.displayTick()` for the separate entity-vs-heartbeat off-by-one.

### Packet Simulation
- Serverbound packets are simulated by calling `packet.handle(nmsPlayer.connection)`
- All interactions EXCEPT movement are driven by crafting and processing serverbound packets. Movement is simulated by setting the player's movement variables directly.

### Movement Input
- `Actions.move(LivingEntity, String input, int ticks)` sets NMS movement fields directly for the given number of ticks, or forever if set to 0 ticks
- Input string characters: `W`/`A`/`S`/`D` for direction, `J` to jump (sets `setJumping(true)` each tick while grounded), `P` for sprint (only applied when `W` is held and neither `S` nor `N` is present), `N` for sneak (also multiplies `xxa`/`zza` by 0.3)
- Opposing directions cancel each other (e.g. `W`+`S` → `zza = 0`)

### Ability System
- Custom items are identified by an ID string stored in the **first lore line** of the item (`CustomItems.getID()` reads `getLore().getFirst()`), e.g. `skyblock/combat/scylla`, `skyblock/combat/terminator`
- All abilities are triggered via `Actions.leftClick(Player)` or `Actions.rightClick(Player)` while the fake player holds the appropriate item — the `CustomItems` listener dispatches the correct ability based on the item's lore ID
- **`leftClick`**: sends `ServerboundSwingPacket`, then ray-traces entities within 5 blocks (excludes self, other fake players, and spectating real players); if an entity is hit sends `ServerboundInteractPacket` attack and returns; otherwise ray-traces blocks and sends `START_DESTROY_BLOCK` + `STOP_DESTROY_BLOCK` only if the item in hand is `DIAMOND_PICKAXE`
- **`rightClick`**: ray-traces blocks first (up to `BLOCK_INTERACTION_RANGE`); if a block is hit sends `ServerboundUseItemOnPacket` and returns early; then ray-traces non-player LivingEntities within 5 blocks and sends `ServerboundInteractPacket` interact if one is hit; **always** ends by sending `ServerboundUseItemPacket` (right-click-air) regardless — this is what triggers item abilities such as etherwarp, ender pearls, and food

### Spectator System
- Real players "spectate" fake players by being teleported every tick to the fake player's position
- Fake player entity is hidden from spectators via `ClientboundRemoveEntitiesPacket` each tick
- Eye height synced by setting spectator's Pose to CROUCHING/STANDING matching the fake player's sneak state
- No-collision managed via scoreboard teams

### Ender Pearl Simulation
- Collision detection and teleport positioning handled server-side

## Conventions
- NMS imports use Mojang mappings (e.g., `ServerPlayer`, `ServerboundMovePlayerPacket`, not `EntityPlayer`/`PacketPlayInFlying`)
- CraftBukkit access via `((CraftPlayer) player).getHandle()` pattern
- Utility methods in `Utils` class (e.g., `Utils.simulatePacket()`, `Utils.broadcastPacket()`)
- Logging via `Utils.debug(DebugType, String)`: `[Client]` (dark aqua) for simulated client packets, `[Server]` (green) for server-side events, `[Game]` (light purple) for boss/game events

## Notes
- Do NOT do any builds or compiles.
- Use python, NOT python3