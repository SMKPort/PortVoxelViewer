# Port Armour Bend v0.1.0 (experimental)

A client-only compatibility addon for **Minecraft 1.21.1 NeoForge**, **Armourer's Workshop**, and **Emotecraft**. It attempts to synchronize the bend of a separately modeled elbow / forearm with Emotecraft emotes, including the Nexus prosthesis.

**Status: source prototype.** The Java bone-name smoke test passes, but full Gradle compilation and in-game behavior are not yet confirmed. Mod APIs may change between beta versions.

## Preparing the Armourer's Workshop model

For best results, model the upper and lower right arm separately:
- `Arm_R`: upper right arm.
- `Hand_R`: forearm, with the pivot correctly positioned at the elbow.

For named advanced child parts, use `Forearm_R`, `Antebraco_R`, `LowerArm_R`, or `Cotovelo_R`. Left-side equivalents use `_L`. Unnamed `Bone2` groups are not inferred. The mod rotates a rigid part, not individual voxels or a smoothly skinned mesh.

## Build via GitHub Actions

In [the repository's Actions tab](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/build-port-armour-bend.yml), select **Build Port Armour Bend (NeoForge 1.21.1)**. Choose **Run workflow** if needed. After a successful run, download the `portarmourbend-1.21.1-neoforge` artifact and extract its `.jar`.

Requires Java 21 and Gradle 8.10.2 to build locally. From this `PortArmourBend` directory: `gradle --no-daemon build`.

## Installation and configuration

Install matching NeoForge, Armourer's Workshop, and Emotecraft versions on the client. Put only the built `.jar` (not the ZIP) into `.minecraft/mods`. On the first launch it writes `config/portarmourbend.json`. Set `bendMultiplier` to `-1.0` if a forearm rotates backwards, then restart the game. `enableNativeHandParts` and `enableNamedForearmChildren` can disable either path.

**Limitations:** This is an experimental integration. It does not make an unsplit arm bend, and its Mixin hooks may need adjustment for the actual installed Armourer's Workshop build. No gameplay or multiplayer server code is changed.

Upstream references: [Armourer's Workshop](https://github.com/Armourers-Workshop/Armourers-Workshop), [Emotecraft](https://github.com/KosmX/emotes), [a related BendyLib adapter](https://github.com/Makarexe/weavers-emote-bend).
