# Folia Compatibility Notes

## Recent Improvements (November 2024)

### Thread-Safe Teleport Operations

Fixed a critical bug where player teleports from async contexts would cause crashes on Spigot/Bukkit servers.

#### Issue
`FoliaUtil.teleportPlayer()` was being called from async movement event handlers, causing the Bukkit `PlayerTeleportEvent` to be triggered from an async thread. This resulted in:
```
java.lang.IllegalStateException: PlayerTeleportEvent may only be triggered synchronously.
```

#### Solution
Modified `FoliaUtil.teleportPlayer()` to:
- Check if the current thread is the primary thread using `Bukkit.isPrimaryThread()`
- On Spigot/Bukkit: Schedule teleport on main thread via `Bukkit.getScheduler().runTask()` if called from async
- On Folia: Continue using `teleportAsync()` which handles thread safety internally

This ensures all teleport operations are thread-safe on both Folia and Spigot/Bukkit platforms.

### Elytra Flight False Positive Fix

Fixed false positive Flight flags when players rejoin the server while elytra-flying.

#### Issue
When a player disconnected while gliding and reconnected:
1. Player remains in gliding state but plugin cache is reset
2. `glidingTicks` reset to 0, `lastGliding` timestamp cleared
3. FlightA and FlightC immediately flag the player for 2-3 seconds after join

#### Solution
Added gliding state detection in `LACPlayerListener.loadLacPlayer()`:
- Checks if player is gliding on join using `VerPlayer.isGliding(player)`
- Sets `lastGliding` to current time and `glidingTicks = 5`
- Existing grace periods in FlightA/FlightC (`lastGliding` within 2000ms, `glidingTicks >= -3`) now prevent false flags
- Player can naturally transition from gliding state without triggering checks

### ClickTP Detection Enhancement

Improved detection of the ClickTP exploit (large instantaneous teleports).

#### How ClickTP Works
The Meteor Client ClickTP module:
1. Raycasts from camera to find clicked block (up to 210 blocks away)
2. Sends optional OnGroundOnly packets (up to 19, based on distance/10)
3. Sends final PositionAndOnGround packet with large displacement
4. Updates client position

#### Detection Strategy
Enhanced `SpeedE.onTeleportHorizontal()`:
- Detects movements >10 blocks as likely ClickTP (typical range: 10-200 blocks)
- Cancels the movement event
- Teleports player back using thread-safe `FoliaUtil.teleportPlayer()`
- Flags violation after 1-tick delay
- Maintains existing 6-10 block detection for other speed hacks

## Improvements Made

### Movement Check Stability Enhancement

The `FoliaUtil.isStable()` check has been improved to better support movement checks on Folia servers.

#### Previous Behavior
The original implementation checked all 30 historical positions and rejected movement events if ANY position showed a 32+ block jump. This was too strict and caused legitimate movement events to be skipped frequently, preventing Speed and Flight checks from working properly.

#### Current Behavior
The improved implementation:
- Checks only the most recent 10 positions instead of all 30
- Allows for one large jump (legitimate teleport) but rejects multiple consecutive large jumps
- This enables movement events to fire more consistently while still preventing issues during actual cross-region teleports

#### Impact
- **SpeedA, SpeedC, FlightB**: Should now work more consistently on Folia as movement events will fire more reliably
- **ElytraB**: Continues to work as before
- **SpeedE (ClickTP)**: Now properly detects ClickTP exploits on both Folia and Spigot/Bukkit with thread-safe teleports
- **FlightA, FlightC**: No longer false flag on elytra rejoin

## Known Limitations

### Region Boundary Crossings

On Folia servers, very rapid movement across multiple region boundaries (>32 blocks every few ticks) may still cause some movement events to be skipped. This is by design to prevent cross-region threading issues.

## Testing Recommendations

When testing on Folia:
1. Test movement hacks both within a single region (< 32 block movements) and across regions
2. Monitor the frequency of movement events being fired vs. being skipped
3. Compare detection rates between Folia and Spigot/Bukkit servers
4. Test with various movement speeds to ensure checks work across different scenarios
5. **Test elytra rejoin**: Disconnect while gliding, reconnect, verify no false Flight flags
6. **Test ClickTP detection**: Use ClickTP to teleport 10-200 blocks, verify it's detected and flagged
7. **Test teleport thread safety**: Monitor logs for IllegalStateException crashes during movement checks

## Future Improvements

If further issues are encountered:
1. Consider implementing packet-level detection for Speed and Flight checks that doesn't rely on `isStable()`
2. Add configurable thresholds for the stability check
3. Implement region-aware movement tracking
4. Add configurable ClickTP distance thresholds

