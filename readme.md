Mâché
=====

Mache (from [papier-mâché](https://en.wikipedia.org/wiki/Papier-m%C3%A2ch%C3%A9)) is an environment which aims to
provide a clean-slate, fully compilable and workable decompiled Minecraft server source code. This serves as the base
for the PaperMC Minecraft server project, of which Paper's patches will apply to it. This can of course also be used by
anyone else who wants to use it for their own purposes.

This repository is analogous to MCPConfig from Forge ([NeoForm now in NeoForged](https://github.com/NeoForged/NeoForm)).
This is a separate project so we could focus the output to be centered around our tooling for our purposes, and we could
customize the output however we like.

Goal
----

The current goal of this project is to provide the Minecraft source set as un-modified as possible. The patches present
are only for allowing the decompiled code to be re-compilable. We may still end up publishing additional builds for a
single Minecraft version, however, as we bring in updates from [codebook](https://github.com/PaperMC/codebook),
[VineFlower](https://github.com/Vineflower/vineflower), or [yarn](https://github.com/FabricMC/yarn).

The Minecraft sources generated here use Mojang's official mappings, and we use yarn mappings as a supplement for
parameter name mappings. Local variable mappings are generated at remap time via
[codebook](https://github.com/PaperMC/codebook).

Usage
-----

Set up all present versions:
```sh
./gradlew setup
```

Set up a specific version:
```sh
./gradlew :versions:1.20.1:setup
```

Patches
-------

If you make changes, you can re-build patches with:
```sh
./gradlew :versions:1.20.1:rebuildPatches
```

You may notice that the source set directory for each version is also its own tiny git repository. This repo is not used
at all in the process of generating or applying patches, it exists solely to improve the developer experience, making it
easier to view your current changes, and the changes pulled in from the existing patches.

License
-------

The code in this repository, as well as the patches, are licensed under [LGPL-3.0-only](license.txt). The decompiled
code is Mojang's proprietary code and not part of the licensed work.
