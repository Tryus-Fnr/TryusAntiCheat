package me.vekster.lightanticheat.check.checks.movement.teleport;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.movement.MovementCheck;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * ClickTP detection - detects teleport hacks that send multiple position packets
 */
public class TeleportA extends MovementCheck implements Listener {
    public TeleportA() {
        super(CheckName.TELEPORT_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, LACPlayer lacPlayer, PlayerCache cache,
                                      boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 1000 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 3000 && time - cache.lastTeleport > 1000 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 400 &&
                time - cache.lastBlockExplosion > 4000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 3000 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 500 && time - cache.lastWasDamaged > 300 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2000 && time - cache.lastStrongAirKbVelocity > 4000 &&
                time - cache.lastFlight > 1000 &&
                time - cache.lastGliding > 1500 && time - cache.lastRiptiding > 2000 &&
                time - cache.lastWindCharge > 500 && time - cache.lastWindChargeReceive > 750 &&
                time - cache.lastWindBurst > 500 && time - cache.lastWindBurstNotVanilla > 1000;
    }

    @EventHandler
    public void onClickTP(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (!isConditionAllowed(player, lacPlayer, event))
            return;

        Location from = event.getFrom();
        Location to = event.getTo();
        Location first = cache.history.onEvent.location.get(HistoryElement.FIRST);

        // Calculate movement deltas
        double horizontalDistance = distanceHorizontal(from, to);
        double verticalDistance = distanceAbsVertical(from, to);
        double totalDistance = distance(from, to);

        // Check for instant position change pattern (ClickTP characteristic)
        // ClickTP sends multiple OnGroundOnly packets followed by a large position jump
        
        // First, detect if this is a sudden position change
        boolean isSuddenChange = totalDistance > 1.5 && horizontalDistance > 1.0;
        
        if (!isSuddenChange) {
            buffer.put("suspiciousCount", 0);
            return;
        }

        // Check if previous movements were minimal (indicating OnGroundOnly packets)
        double previousMovement = distance(first, from);
        boolean hadMinimalPreviousMovement = previousMovement < 0.05;

        // Detect the ClickTP pattern:
        // - Sudden position change (current check passed)
        // - Previous movement was minimal or zero
        // - Not a legitimate teleport cause
        if (hadMinimalPreviousMovement || totalDistance > 2.0) {
            buffer.put("suspiciousCount", buffer.getInt("suspiciousCount") + 1);
            
            // Additional validation: Check if the movement is too precise/instant
            // ClickTP typically results in very clean position changes
            double xDiff = Math.abs(to.getX() - from.getX());
            double zDiff = Math.abs(to.getZ() - from.getZ());
            
            // ClickTP often lands on block centers (X.5, Z.5)
            boolean suspiciousBlockAlignment = 
                (Math.abs((to.getX() % 1) - 0.5) < 0.1 && Math.abs((to.getZ() % 1) - 0.5) < 0.1);
            
            if (suspiciousBlockAlignment || totalDistance > 3.0) {
                buffer.put("suspiciousCount", buffer.getInt("suspiciousCount") + 1);
            }
            
            // Flag if we've seen this pattern multiple times
            if (buffer.getInt("suspiciousCount") >= 2) {
                Scheduler.runTask(true, () -> {
                    callViolationEventIfRepeat(player, lacPlayer, event, buffer, 3000);
                });
                buffer.put("suspiciousCount", 0);
            }
        } else {
            // Reset counter if the pattern doesn't match
            buffer.put("suspiciousCount", Math.max(0, buffer.getInt("suspiciousCount") - 1));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("suspiciousCount", 0);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("suspiciousCount", 0);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("suspiciousCount", 0);
    }
}
