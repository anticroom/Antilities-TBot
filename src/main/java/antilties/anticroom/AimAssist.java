package antilties.anticroom;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public class AimAssist {
    private final Minecraft mc = Minecraft.getInstance();
    public static Vec3 currentAimPoint = null;
    private Entity currentTarget = null;
    private long driftTimer = 0;
    
    private long targetAcquiredTime = 0;

    public void onTick() {
        ConfigManager.ConfigData config = ConfigManager.config;
        
        if (!config.aimAssistEnabled || mc.player == null || mc.level == null) return;
        if (config.pauseOnScreens && mc.screen != null) return;
        
        if (config.aimAssistWeaponOnly) {
            String itemId = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase();
            boolean holdingWeapon = itemId.contains("sword") || itemId.contains("axe");
            if (!holdingWeapon) {
                currentTarget = null;
                return;
            }
        }

        driftTimer++;

        Entity best = getBestTarget(config);
        if (best != currentTarget) {
            targetAcquiredTime = System.currentTimeMillis();
        }
        currentTarget = best;

        if (currentTarget != null && currentAimPoint != null && config.renderTargetLine) {
            for (double yOffset = -1.0; yOffset <= 1.0; yOffset += 0.2) {
                mc.level.addParticle(
                        DustParticleOptions.REDSTONE,
                        currentAimPoint.x, currentAimPoint.y + yOffset, currentAimPoint.z,
                        0, 0, 0
                );
            }
        }
    }

    public void onFrame(float tickDelta) {
        ConfigManager.ConfigData config = ConfigManager.config;
        currentAimPoint = null;

        if (!config.aimAssistEnabled || mc.player == null || mc.level == null || currentTarget == null) return;
        if (config.pauseOnScreens && mc.screen != null) return;

        if (config.aimAssistWeaponOnly) {
            String itemId = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase();
            boolean holdingWeapon = itemId.contains("sword") || itemId.contains("axe");
            if (!holdingWeapon) return;
        }

        if (!currentTarget.isAlive() || !mc.player.hasLineOfSight(currentTarget) || mc.player.distanceTo(currentTarget) > config.aimAssistMaxDistance) {
            currentTarget = null;
            return;
        }

        if (System.currentTimeMillis() - targetAcquiredTime < config.aimAssistDelay) {
            return;
        }

        Vec3 aimPoint = calculateMidPoint(currentTarget, tickDelta, config);
        currentAimPoint = aimPoint;

        faceTargetSmoothed(aimPoint, currentTarget, config, tickDelta);
    }

    private Entity getBestTarget(ConfigManager.ConfigData config) {
        Iterable<Entity> entities = mc.level.entitiesForRendering();
        
        return StreamSupport.stream(entities.spliterator(), false)
            .filter(e -> e instanceof LivingEntity && e != mc.player && e.isAlive())
            .filter(e -> mc.player.distanceTo(e) <= config.aimAssistMaxDistance)
            .filter(e -> {
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
                return mc.player.hasLineOfSight(e);
            })
            .filter(e -> getFovDistance(e) <= config.aimAssistFov)
            .min(Comparator.comparingDouble(this::getFovDistance))
            .orElse(null);
    }

    private double getFovDistance(Entity entity) {
        Vec3 targetPos = entity.getBoundingBox().getCenter();
        double dX = targetPos.x - mc.player.getX();
        double dY = targetPos.y - mc.player.getEyeY();
        double dZ = targetPos.z - mc.player.getZ();
        
        double yawToTarget = Math.toDegrees(Math.atan2(dZ, dX)) - 90.0;
        double pitchToTarget = -Math.toDegrees(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)));
        
        double yawDiff = Mth.wrapDegrees(yawToTarget - mc.player.getYRot());
        double pitchDiff = Mth.wrapDegrees(pitchToTarget - mc.player.getXRot());
        
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private double getLayeredNoise(double time, double offset) {
        double t = time + offset;
        double noise = Math.sin(t) 
                     + 0.5 * Math.sin(t * 2.189 + offset) 
                     + 0.25 * Math.sin(t * 4.343 + offset)
                     + 0.125 * Math.sin(t * 7.912 + offset);
        return noise / 1.875;
    }

    private Vec3 calculateMidPoint(Entity target, float tickDelta, ConfigManager.ConfigData config) {
        Vec3 eye = mc.player.getEyePosition(tickDelta);
        
        double x = Mth.lerp(tickDelta, target.xo, target.getX());
        double y = Mth.lerp(tickDelta, target.yo, target.getY());
        double z = Mth.lerp(tickDelta, target.zo, target.getZ());
        
        double width = target.getBbWidth() / 2.0;
        double height = target.getBbHeight();
        
        AABB box = new AABB(x - width, y, z - width, x + width, y + height, z + width);
        Vec3 center = box.getCenter();
        
        double offsetY = height * config.aimAssistVerticalOffset;
        
        double driftX = 0;
        double driftY = 0;
        double driftZ = 0;

        if (config.aimAssistDrift) {
            double time = (driftTimer + tickDelta) * 0.1;
            double amp = config.aimAssistDriftAmplitude;
            driftX = Math.sin(time) * amp;
            driftY = Math.cos(time * 0.8) * amp;
            driftZ = Math.sin(time * 1.2) * amp;
        }

        double noiseX = 0;
        double noiseY = 0;
        double noiseZ = 0;

        if (config.aimAssistVariance > 0) {
            double timeNoise = (driftTimer + tickDelta) * 0.2;
            double amp = config.aimAssistVariance;
            noiseX = getLayeredNoise(timeNoise, 0.0) * amp;
            noiseY = getLayeredNoise(timeNoise, 43.123) * amp;
            noiseZ = getLayeredNoise(timeNoise, 89.541) * amp;
        }

        Vec3 closest = new Vec3(
            Mth.clamp(eye.x, box.minX, box.maxX),
            Mth.clamp(eye.y, box.minY, box.maxY),
            Mth.clamp(eye.z, box.minZ, box.maxZ)
        );

        double basePointX = Mth.lerp(config.aimAssistHitboxEdge, center.x, closest.x);
        double basePointY = Mth.lerp(config.aimAssistHitboxEdge, center.y, closest.y);
        double basePointZ = Mth.lerp(config.aimAssistHitboxEdge, center.z, closest.z);
        
        return new Vec3(
            basePointX + driftX + noiseX,
            basePointY + offsetY + driftY + noiseY,
            basePointZ + driftZ + noiseZ
        );
    }

    private void faceTargetSmoothed(Vec3 targetPoint, Entity target, ConfigManager.ConfigData config, float tickDelta) {
        Vec3 playerPos = mc.player.getPosition(tickDelta);
        double eyeY = playerPos.y + mc.player.getEyeHeight();

        double dX = targetPoint.x - playerPos.x;
        double dY = targetPoint.y - eyeY;
        double dZ = targetPoint.z - playerPos.z;

        double desiredYaw = Math.toDegrees(Math.atan2(dZ, dX)) - 90.0;
        double desiredPitch = -Math.toDegrees(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)));

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        double yawDiff = Mth.wrapDegrees(desiredYaw - currentYaw);
        double pitchDiff = Mth.wrapDegrees(desiredPitch - currentPitch);

        double smoothing = Math.max(1.0, config.aimAssistSmoothing * 10.0);

        if (config.aimAssistFovFalloff && config.aimAssistFov > 0) {
            double fovDist = getFovDistance(target);
            double ratio = Mth.clamp(fovDist / config.aimAssistFov, 0.0, 1.0);
            smoothing *= (1.0 + Math.pow(ratio, 2) * 4.0);
        }

        double yawStep = 0;
        double pitchStep = 0;

        switch (config.smoothingMode) {
            case LINEAR:
                double speed = 200.0 / smoothing;
                yawStep = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), speed);
                pitchStep = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), speed);
                break;
            case EXPONENTIAL:
            default:
                yawStep = yawDiff / smoothing;
                pitchStep = pitchDiff / smoothing;
                break;
        }

        if (!config.aimAssistYOnly) {
            mc.player.setYRot(currentYaw + (float) yawStep);
        }
        if (!config.aimAssistXOnly) {
            mc.player.setXRot(currentPitch + (float) pitchStep);
        }
    }
}