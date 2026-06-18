# TAS Program Language Specification
## Introduction
The first version of this program had hardcoded instructions that were a pain to deal with at best, and a nightmare at worst.  This language is designed to ease that burden.

## TAS.txt file
This file lays out the barebones of the TAS.  It includes the following information;
1. The number of players
2. The identifiers for those players
3. The classes of the players, in order 
4. The number of phases 
5. The names of the phases

Note: The Server will ALWAYS have a config file that does not need to be defined in the TAS.txt file.

For example, here is the specification for the original TAS;
```
players;5
names;Archer,Berserk,Healer,Mage,Tank
classes;Archer,Berserk,Healer,Mage,Tank
phaseCount;6
phases;Clear,Maxor,Storm,Goldor,Necron,WitherKing
```

The names will be registered with the `/spectate` and `/unspectate` commands.  The phases will be registered with the `/tas` command.

All section separators and file names must the exactly the same as defined in this file.

### In all config files, the following conventions should be followed;

**Section Beginning**

The beginning of a section begins with `!!!` followed by the section's name.

Each section may have some initial setup instructions that are only run if it is accessed directly via the `/tas` command, these always come first and are separated from the main instructions by a line of at least 20 equal symbols.

Note that when a TAS is run, there will be 60 ticks of "setup" time.  You can use this to your advantage when there may be residual actions from previous phases that need to occure before the current one begins.

Example:
```
!!! Maxor
teleport;73.5,221,13.5,tp;0
swapitems;1,28;0
swapitems;7,35;0
shootterminator;;54
====================
sethotbarslot;6;0
// etc
```

**Instruction Separation**

All instructions are separated by newlines.

The instruction name is separated by the arguments with a semicolon.  All other arguments are separated by a comma.

Every instruction will always contain a final "delay" integer that tells the program how many ticks to wait **after the previous instruction**.  This is also separated by a semicolon.

Example:
```
move;1.12242,0,0,3,false;2
turnhead;180,0;5
move;0,0,1.12242,5,false;2
```

This will tell the specified player to wait 2 ticks, walk 3 ticks, wait 5 ticks, turn their head, wait 2 ticks, then walk another 5 ticks.

If there are two overlapping lines, the newest line will be executed.  For example:

```
move;1.12242,0,0,5,false;0
move;-1.12242,0,0,5,false;3
```

This code will cause the player to walk in the +X direction for 3 ticks, then the next instruction overrides it, making the player walk in the -X direction for 5 ticks.

**Conventions**

To ensure 100% deterministic behavior, all commands will use exact x/y/z in terms of movement direction and teleport locations.  It is possible to use the current yaw/pitch to determine movement, but it may cause non-deterministic behavior.

Java-style single-line comments are supported (i.e. `// this is a comment`)

## server.txt file
The server.txt file is the central aspect of the TAS.  It is the file that controls every server-related action (e.g. teleporting all players to a phase, dictating which phase is to begin, etc).

The server.txt file contains the following valid instructions;

### `command;[String command]`
Runs the supplied Minecraft command.

Example: `command;setblock 0 0 0 minecraft:bedrock;2`

### `teleportall;[double x],[double y],[double z]`
Teleports all fake players to the given x/y/z.

Example: `teleportall;0,100,0;3`

### `broadcast;[String message],[boolean title]`
Broadcasts the supplied message in chat to everyone.  Title controls whether it is duplicated in a title for all players.

Example: `broadcast;§c⚠ Maxor is enraged! ⚠,true;0`

### `beginphase;[String phase]`
Executes the script for the given phase.

Example: `beginphase;maxor;0`

## Phase-specific config files
These files are an extension of the server.txt file and control the properties of a specific phase.  They can run any command from server config files, any command from player config files, and the following:

### `spawnboss;[MobType type],[String name],[double hp],[double x],[double y],[double z],[double yaw],[double pitch]`
Spawns a boss mob with the specified mob type, the given name, the max HP it can have, and its location.

This mob is the mob that is assumed if any movement-related commands are run in the file.

Example: `spawnboss;minecraft:wither,Maxor,400,73.5,226,53.5,0,0;0`

### `setinvulnerable;[boolean invulnerable]`
Sets the damagable status of the boss.

Example: `setinvulnerable;true;1`

### `removeboss;`
Removes the boss of the current phase.

Example: `removeboss;;1000`

### `dialogue;[String text]`
The Boss (if present) will send a message to all players in the chat.  Otherwise it acts like a broadcast message.

Example: `dialogue;I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU.;60`

### `spawnentity;[EntityType type],[String name],[double hp],[double x],[double y],[double z],[double yaw],[double pitch]`
Spawns an Entity at the specified location.  The Entity will be assigned an internal ID based on the order it was spawned in.

Example:
```
spawnentity;minecraft:ender_crystal,Energy Crystal,92.5,238.48,50.5,0,0;0
spawnentity;minecraft:ender_crystal,Energy Crystal,64.5,238.48,50.5,0,0;0
```
In this example, the first crystal (left) will be given the internal ID of "Energy Crystal1" and the second crystal (right) will be given the internal ID of "Energy Crystal2"

### `spawnentities;[EntityType type],[String name],[double hp],[double x],[double y],[double z],[int count],[int radius]`
Spawns a number of Entities at the specified location, spread randomly within the given radius.  Like before, the Entities will be assigned internal IDs, but those probably won't be that useful.

Example: `spawnentities;minecraft:wither_skeleton,Wither Miner,20,100,100,100,3,3;0om`

### `removeentity;[String id]`
Removes the specified Entity.

### `removeentities;[EntityType type]`
Removes all Entities of the given type.

Examples:
```
removeentity;Energy Crystal1
removeentities;minecraft:ender_crystal
```
In this example, the first command would remove all End Crystals, while the second would only remove the left crystal.

## Player-specific files

### Initial Inventory Configuration
All player-specific files will begin with the name of the skin it should use (which doubles as the player name).  This will simply be the name of the player on one line.

The following 40 lines will be the initial inventory setup.  Each line represents one inventory slot, in order from 0 to 40.

Minecraft items will utilize the following format: `minecraft:[String itemname],[int count]`

Empty items should use `minecraft:air`

If the item is leather armor, include a hex code: `minecraft:[String itemname],[String customname],[int count],[String hex]`

If the item is a custom player head, include the name of the player: `minecraft:player_head,[int count],[String name]`

Custom SkyBlock items will utilize the following format: `[String id],[String customname]`, where the id is the item ID in the SkyBlock in Vanilla plugin.

Note: The hard dependency on the plugin is removed, but the format is still the same.

Here is an example of a Mage's inventory:
```
Beethoven_
skyblock/combat/scylla,§dHeroic Hyperion
skyblock/combat/AOTV,§6Warped Aspect of the Void
skyblock/combat/ice_spray_wand,§6Heroic Ice Spray Wand
skyblock/combat/claymore,§dWithered Dark Claymore
minecraft:ender_pearl,§6Infinleap,1
minecraft:diamond_pickaxe,§cScraped Dungeonbreaker
skyblock/combat/gyro,§6Gyrokinetic Wand
minecraft:ender_pearl,,16
minecraft:nether_star,§cSkyBlock Menu,1
minecraft:chainmail_boots,§9Renowned Spring Boots
[18x minecraft:air]
skyblock/combat/bonzo_staff,§5Heroic Bonzo Staff
skyblock/combat/tactical_insertion,§6Tactical Insertion
skyblock/combat/scylla,§dWithered Hyperion
minecraft:tnt,§6Infiniboom TNT,1
minecraft:golden_axe,§5Withered Ragnarok Axe,1
minecraft:bow,§dPrecise Last Breath,1
minecraft:air
minecraft:air
minecraft:leather_boots,§dAncient Storm's Boots,1,1cd4e4
minecraft:leather_leggings,§dAncient Storm's Leggings,1,17a8c4
minecraft:leather_chestplate,§dAncient Storm's Chestplate,1,1793c4
minecraft:player_head,1,ReactionBrineYT
minecraft:air
```

Afterwards, the last line consists of a String and 5 numbers:
1. The class this player is simulating (`Archer` `Berserk` `Healer` `Mage` `Tank`)
2. Melee/mage beam damage
3. Melee/mage beam damage with rag active
4. Terminator damage
5. Terminator damage with rag active
6. Damage increase per hit landed (Bers-exclusive)

Example for a Mage simulating p24: `Mage,70,90,1,1,0`

After this, use `!!!` section break to denote the first instructions.

### `move;[double x],[double y],[double z],[int ticks],[boolean force]`
Moves the player in the given x/y/z for the specified number of ticks.  `force` determines if the movement is forced (i.e. ignores collisions)

Example: `move;1.12242,0,0,5,false;0`

### `jump`
Causes the player to jump.

Example: `jump;;1`

### `sethotbarslot;[int index]`
Sets the currently selected hotbar slot of the player.  Valid numbers are 0-8.

Example: `sethotbarslot;1;2`

### `swapitems;[int index1],[int index2]`
Swaps the two items in the given indexes.  Follows the standard Minecraft slot numbering.

Example: `swapitems;9,39;3`

### `turnhead;[double yaw],[double pitch]`
Turns the head of the player.

Example: `turnhead;90,0;4`

### `teleport;[double x],[double y],[double z],[String type]`
Performs a teleport using an Ender Pearl, AOTV, Etherwarp, or some other reason (e.g. as part of setup).  The player is teleported to the given x/y/z.

Valid options: `enderpearl` `aotv` `etherwarp` `tp` `witherimpact`

`enderpearl` requires an Ender Pearl to have been thrown recently.  `aotv` and `etherwarp` require the item in hand to be a `minecraft:diamond_shovel` named Aspect of the Void.  `witherimpact` requires the item in hand must be a `minecraft:iron_sword` named Hyperion.

Option `witherimpact` also deals 1 damage to all mobs in a 6-block radius of the destination.

Example: `teleport;100,100,100,enderpearl;6`

### `stonk;[double x],[double y],[double z]`
Performs a Stonking action on the block at the given x/y/z.  The block regenerates after 200 ticks.  The item in hand must be a `minecraft:diamond_pickaxe` named Dungeonbreaker

Example: `stonk;100,100,100;7`

### `superboom;[double x1],[double y1],[double z1],[double x2],[double y2],[double z2],[String type]`
Performs a Superboom action, temporarily removing the blocks in the area of the given coordinates depending on the type.  The blocks regenerate after 200 ticks.

Valid options: `wall` `crypt`  The item in hand must be a `minecraft:tnt` named Infiniboom

If `wall` is selected, ONLY Cracked Stone Bricks are affected.  If `crypt` is selected, a Crypt Undead will spawn at the location.

Example: `superboom;100,100,100,102,102,102.wall;8`

### `leap;[String player]`
Uses Spirit Leap to teleport to the given player.  The item in hand must be a `minecraft:ender_pearl` named Infinileap

Example: `leap;Archer;9`

### `throwpearl;`
Throw an Ender Pearl.  The item in hand must be a vanilla `minecraft:ender_pearl`

**WARNING** The Ender Pearl thrown is only simulated.  The actual teleporting must be performed using the `teleport` instruction.

Example: `throwpearl;;10`

### `rag;`
Use the Ragnarok Axe ability.  The item in hand must be a `minecraft:golden_axe` named Ragnarok Axe.

Example: `rag;;11`

### `shootterminator;`
Shoots Terminator arrows.  The item in hand must be a `minecraft:bow` named Terminator.

Example: `shootterminator;;12`

### `salvation;`
Shoots a Salvation beam.  The item in hand must be a `minecraft:bow` named Terminator.

Example: `salvation;;13`

### `springboots;[int ticks]`
Perform a Spring Boots jump for the specified number of ticks.  Player must be wearing, in the Boots slot, `minecraft:chainmail_boots` named Spring Boots.

Example: `springboots;16;14`

### `aots;`
Simulates an Axe of the Shredded axe throw.  The item in hand must be a `minecraft:diamond_axe` named Axe of the Shredded

Example: `aots;;30`

### `lb;[int ticks]`
Shoots a Last Breath shot after drawing the bow back for the specified number of ticks.  The item in hand must be a `minecraft:bow` named Last Breath.

Example: `lb;3;31`

### `icespray;`
Uses the Ice Spray ability.  The item in hand must be a `minecraft:stick` named Ice Spray Wand.

Example: `icespray;;34`

### `flay;`
Uses the Flaming Flay ability.  The item in hand must be a `minecraft:fishing_rod` named Flaming Flay.

Example: `flay;;35`

### `gyro;`
Uses the Gyrokinetic Wand's Gravity Storm ability.  The item in hand must be a `minecraft:blaze_rod` named Gyrokinetic Wand.

Example: `gyro;;36`

### `tac;`
Uses the Tactical Insertion ability.  The item in hand must be a `minecraft:blaze_rod` named Tactical Insertion.

Example: `tac;;37`

### `bonzo;[double x],[double z]`
Uses the Bonzo Staff ability to move in the specified x/z.  The item in hand must be a **`minecraft:breeze_rod`** named Bonzo Staff.

Example: `bonzo;1.52552,0;38`

### `lavajump;[boolean big]`
Performs a lava jump.  `big` determines if the lava jump is a large one.

Example: `lavajump;false;39`

### `attack;`
Performs a melee hit.  Players simulating Mage will instead use their Mage Beam.

Example: `attack;;40`