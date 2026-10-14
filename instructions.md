# mc-tas lang
## Introduction
MC-TAS Lang is a language designed to allow Minecraft TASes to be written more akin to a traditional TAS: tick by tick actions VS recording movement live.  It achieves this by creating a fake Player then simulating that player sending specific packets via actions.  Any number of players can be simulated.

## File Structure
TAS scripts will be written in the config folder of the plugin.  From the server root, you can find the TAS scripts root by navigating to `/plugins/mc-tas lang/`

Each player will have their own file.  The player's name will be the name of the file.  For example, for a player named "Archer", their script will be stored in `Archer.txt`

## File Header
Befor the script itself, important information about the fake player, as well as any setup actions to be run, should be included at the top of the file.

An example header for a M7 TAS player looks like this:

```
spawn : -120.5 71 -183.5 
gamemode : adventure
# TODO work in progress
```


## Language Basics
In general, every line will have one and only one action.  The start of the line will be a number, representing the number of ticks to delay this action by with respect to time passed since the start (OR a previous checkpoint, more on that later).  Separate the two halves with a colon.  The second half will call a function, similar to like in Java, except this function will be an action a player can take on a real Minecraft client.

Example:
`10 : openInventory()`

This opens the player's inventory after ten ticks of delay.

Functions may also require arguments.  You can supply them similar to Java.

Example:
`20 : setHotbarSlot(5)`

This sets the player's selected hotbar slot to 5 (which is the sixth slot in the hotbar) after 20 ticks of delay.

Actions MUST be ordered sequentially.  For example, this would be an error:

```
1 : setHotbarSlot(1)
10 : rightClick()
5 : leftClick()
```

## Checkpoints
Any script may designate a specific tick a "Checkpoint Tick".  These are useful for keeping the tick delays managable while also allowing you to run the script from any point in the run (as opposed to the start).  Checkpoint Ticks can be denoted by calling the `checkpoint()` function, then supplying a name for the checkpoint as an argument.

Example:
```
495 : leftClick()
500 : checkpoint("Storm")
0 : leftClick()
5 : leftClick()
```

In this example, tick 500 is designated a checkpoint tick with the label "Storm".  When running the script, you can run `/tas storm` and it will start from there.

After creating a checkpoint, you MUST reset to 0.  The tick of the checkpoint tick will be tick 0 when referencing ticks with respect to them.

Multiple players can create checkpoint ticks with the same label.  If a player has a checkpoint tick but another player doesn't, the plugin will begin the other player's script from the same relative tick.

**WARNING** You MUST run the full script after creating a checkpoint so that the server can save the location and current action of all players, otherwise it will not know where to spawn them!!!  You can find these in the `/checkpoints` folder in the root of the plugin's config folder.

