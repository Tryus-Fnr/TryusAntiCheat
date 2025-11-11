# Folia Compatibility Notes

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
- **TeleportA (ClickTP)**: Works on both Folia and Spigot/Bukkit as it detects sudden position changes

## Known Limitations

### Region Boundary Crossings

On Folia servers, very rapid movement across multiple region boundaries (>32 blocks every few ticks) may still cause some movement events to be skipped. This is by design to prevent cross-region threading issues.

## Testing Recommendations

When testing on Folia:
1. Test movement hacks both within a single region (< 32 block movements) and across regions
2. Monitor the frequency of movement events being fired vs. being skipped
3. Compare detection rates between Folia and Spigot/Bukkit servers
4. Test with various movement speeds to ensure checks work across different scenarios

## Future Improvements

If further issues are encountered:
1. Consider implementing packet-level detection for Speed and Flight checks that doesn't rely on `isStable()`
2. Add configurable thresholds for the stability check
3. Implement region-aware movement tracking

