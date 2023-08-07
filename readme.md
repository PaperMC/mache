Mâché
=====

Mâché (from [papier-mâché](https://en.wikipedia.org/wiki/Papier-m%C3%A2ch%C3%A9)) is an environment which aims to
provide a clean-slate, fully compilable and workable decompiled Minecraft server source code. This serves as the base
for the PaperMC Minecraft server project, of which Paper's patches will apply to it. This can of course also be used by
anyone else who wants to use it for their own purposes.

This repository is analogous to MCPConfig from Forge ([NeoForm now in NeoForged](https://github.com/NeoForged/NeoForm)).
This is a separate project so that we could focus the output to be centered around our tooling for our purposes, and we
could customize the output however we like.

Goal
----

The current goal of this project is to provide the Minecraft source set as un-modified as possible. The patches present
are only for allowing the decompiled code to be re-compilable. We may still end up publishing additional builds for a
single Minecraft version, however, as we bring in updates from [codebook](https://github.com/PaperMC/codebook),
[VineFlower](https://github.com/Vineflower/vineflower),
[AutoRenamingTool](https://github.com/neoforged/AutoRenamingTool) or
[Parchment](https://github.com/ParchmentMC/Parchment).

The Minecraft sources generated here use Mojang's official mappings, and we use Parchment mappings for parameter name
mappings. Local variable mappings are generated at remap time via
[codebook](https://github.com/PaperMC/codebook).

Documentation
-------------

[See the wiki for docs on usage and further details.](https://github.com/PaperMC/mache/wiki)

License
-------

The code in this repository, as well as the patches, are licensed under [LGPL-3.0-only](license.txt). The decompiled
code is Mojang's proprietary code and not part of the licensed work.
