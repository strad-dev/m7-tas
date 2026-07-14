# M7 Tool-Assissted Speedrun & Practice Map
This is a Minecraft 26.2 server plugin that simulates Master Mode The Catacombs Floor 7 in Hypixel SkyBlock (specifically version 0.24.5 - Assorted QoL Changes, the last version before pearl patch).

**This is not a client-side mod and cannot be used on the real Hypixel server, nor is it in any way associated or affiliated with Hypixel.**

This version of the plugin is a fork of the main M7 TAS plugin for use on Stradivarius Violin's Public M7 Practice Server.

**ADVANCED USERS**: This plugin and map can be locally hosted if you know how to run a PaperMC server.  It can also be built yourself within an IDE.  Stradivarius Violin is not responsible for any issues that may arise from improper building and installation, and does not guarantee that it will work on your machine.  Limited support for self-hosting is available in the [Discord Server](https://discord.gg/gNfPwa8)

## Practice Mode

### Getting Started

Select a class using the `/getcustomitems` command.  You will be given a kit and the corresponding class tag, allowing you to use the class's passives and abilities (if applicable).

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
