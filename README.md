# M7 Tool-Assissted Speedrun & Practice Map
This is a Minecraft 1.21.11 server plugin that simulates a perfect run of Master Mode The Catacombs Floor 7 in Hypixel SkyBlock.  **This is not a client-side mod and cannot be used on the real Hypixel server, nor is it in any way associated or affiliated with Hypixel.**

This plugin also allows players to practice M7 with perfect RNG and deterministic aggro behavior.

### Prerequisites
- You must be using the handcrafted map that I made.  A zip for it can be found at the top-level of the plugin.  If you compiled the plugins yourself, you can simply extract the world files and use those.

### Contributing
- If you found a time save or an error, open an issue and describe the error/timesave you found, or make a PR if you can understand my spaghetti code.

## TAS

### Getting Started

Run the `/setup` command to spawn all the fake players.

### Spectating

Use the `/spectate` command to view the TAS from the point of view of one of the players.  Run `/unspectate` if you wish to stop spectating.

### Running the TAS

Run the `/tas` command to run the entire TAS.  Alternatively, if you want to view a specific section, they are provided as follows: `clear` `boss` `maxor` `storm` `goldor` `necron` `witherking`

## Practice Mode

### Getting Started

Select a class using the `/getcustomitems` command.  You will be given a kit and the corresponding class tag, allowing you to use the class's passives and abilities (if applicable).

The recommended Minecraft game mode is **Adventure Mode**

### Running Practice Mode

Use the `/practice` command with 1-5 players.  By default, this will run all sections.

You can also specify a specific section (`clear` `boss` `maxor` `storm` `goldor` `necron` `witherking`) to practice.

If you are practicing a section that is NOT clear or maxor, the plugin will, by default, teleport you to the spawn location for that section.  You can bypass this by including the `--no-teleport` flag, but beware that it is up to you to start at the correct location!