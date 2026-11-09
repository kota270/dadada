# Auto Totem Test — Fabric 1.21.11

Client-side test utility intended for an authorized private server.

## Features
- `R`: enable/disable automatic totem replacement.
- `G`: cycle timing profiles:
  - `SAFE`: waits 2 client ticks after detecting an empty offhand.
  - `FAST`: waits 1 client tick.
  - `INSTANT`: attempts the normal inventory swap on the next client tick.
- Reports the local time from observing an empty offhand to issuing the inventory click.

## Important limitations
- This is not an anti-cheat bypass and does not hide the mod.
- The displayed timing is a client-side measurement, not a guaranteed server round-trip time.
- The server remains authoritative: inventory actions can be delayed, rejected, or reordered by server/network conditions.
- Use only on a server where you have permission to test.

## Build
Requirements: Java 21 and Gradle 8.14+ (or a compatible Gradle installation).

From this folder:
```bash
gradle build
```

The built mod JAR will be in `build/libs/`.
Install it in the Minecraft 1.21.11 Fabric client's `mods` folder, together with Fabric API.

## Controls
You can change the keys under Options → Controls → Key Binds → Auto Totem Test.
