# Folia Compatibility Notes

## Known Issues

### Movement Check Behavior Differences

On Folia servers, some movement checks (SpeedA, SpeedC, FlightB) may not trigger as frequently as on Spigot/Bukkit servers due to the way Folia's regionized threading works.

#### Root Cause
Folia uses regionized threading where different chunks/regions run on different threads. When a player moves across region boundaries (every 32 blocks), the `FoliaUtil.isStable()` check returns false to prevent cross-region issues. This causes movement events (`LACAsyncPlayerMoveEvent`) to be skipped during region transitions.

#### Impact
- **SpeedA, SpeedC, FlightB**: These checks rely on consistent movement event streams to accumulate violations. When events are skipped during region transitions, the checks don't accumulate enough violations to flag the player.
- **ElytraB**: Still works because elytra flight tends to stay within region boundaries more consistently.
- **TeleportA (ClickTP)**: Should work on both Folia and Spigot/Bukkit as it detects sudden position changes which are less affected by incremental event skipping.

#### Workarounds
1. The `isStable()` check in `LACEventCaller.callMovementEvents()` could be made less restrictive
2. Movement checks could be modified to tolerate gaps in the event stream
3. Alternative packet-level detection could supplement movement event detection

## Testing Recommendations

When testing on Folia:
1. Test movement hacks both within a single region (< 32 block movements) and across regions
2. Monitor the frequency of movement events being fired vs. being skipped
3. Consider the player's movement patterns - frequent long-distance movement will trigger more region transitions

## Future Improvements

Consider implementing packet-level detection for Speed and Flight checks that doesn't rely on `isStable()`, similar to how TimerA uses packet events directly.
