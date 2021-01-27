# TellMe Fork for generating JER DYI worldgen files
After 1.12, Just Enough Resources removed the capability to profile worlds to generate `world-gen.json` files. That makes its ore distribution feature essentially useless, as it can only know the vanilla ore distributions. I've decided to spend some time fixing it, and this was the result.

This is a fork of TellMe butchered by me to implement two additional commands, allowing to scan the world for blocks (keeping track of the number of each kind on each y-level) and generating JER-compatible `world-gen.json` files from it. **It is not meant to be used for anything else**. I also don't see a reason to make a PR to the parent repo, as the code quality in my fork is rather atrocious. However, **if you're a modpack developer for looking to include JER oredistribution graphs in your modpack**, this tool was made for you.

# Instructions
1. Put the mod's jar (see Releases, or compile from source) into the `mods` folder. You'll also want a way to efficiently pregenerate the world, like [Chunk Pregenerator](https://www.curseforge.com/minecraft/mc-mods/chunkpregenerator).
2. Make a new world. Pregenerate a large area around the world origin - for example, `/pregen start gen radius pregentheworld SQUARE 0 0 34` to pregenerate a square a bit bigger than 64 chunks at a side. This will take multiple minutes (the GUI will show progress).
3. Use the command `/tellme block-stats-by-level count area -512 -512 511 511` to scan a 1024x1024 block area around the world origin. This will take a few minutes, and will print a message when done.
4. Use the command `/tellme block-stats-by-level create-jer-file <dimension>` to dump the data to a file. Select the right dimension (the one you profiled in) using autocompletion. This will print a message when finished. Sometimes it can take as much as 10 minutes (not sure why - it might be the chunks loaded by the previous step unloading, since it really isn't doing anything complicated).

The result will be a file `/config/tellme/world-gen_<timestamp>.txt`. Rename it to `world-gen.json` and copy it to `/config/`. Reload the world, and JER should pick it up and start showing ore distribution graphs (check recipes for some non-vanilla ore).

To profile several dimensions, profile each and then manually merge the resulting files.

# Current known limitations
~~1. Does not currently support anything but Overworld.~~ Fixed in 0.1.1, instructions updated accordingly.

2. The output has no filtering - *every* block that got into the scanned area will be in the output file. You can filter the final file yourself, or I might get around to implementing it later.

3. The chunks are currently loaded all at once before processing. For me, it's possible with some pain to make a scan 4 times as big as the one in the instructions (2048x2048 blocks, or 16 thousand chunks). The scanning takes around 2 minutes (most of it loading the chunks, the counting only takes 23 seconds), but the file output takes 8 minutes (and I don't know why). I might implement a better process in the future that slices the area in parts and processes them in order. For nice, smooth graphs, you probably want a scan of about this size, rather than the one in instructions.



TellMe
=========================

TellMe is a small informational mod for Minecraft.
It is mainly meant for modpack makers or other users who need some technical type information
about the game or some settings.

For documentation of all the commands and features, go to:

* https://minecraft.curseforge.com/projects/tellme


=====================================

Compilation and installation from source:

* git clone https://github.com/maruohon/tellme.git
* cd tellme
* gradlew build

Then copy the tellme-&lt;version&gt;.jar from build/libs/ into your Minecraft mods/ directory.
The mod needs Forge to be installed.
