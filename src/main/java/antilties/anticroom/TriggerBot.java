package antilties.anticroom;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot {
    private final Minecraft mc = Minecraft.getInstance();

    private int hitDelayTimer = 0;
    private int smartDelayTimer = -1;
    private Entity currentTarget = null;
    private int targetTicks = 0;
    private int requiredTargetTicks = 0;

    private Player lockedPlayer = null;
    private int ticksSinceLastHit = 0;
    
    private float lastHealth = -1f;
    private float currentFlickOffset = 0f;
    
    private int fistDelayTimer = 0;

    public void onTick() {
        ConfigManager.ConfigData config = ConfigManager.config;

        if (!config.enabled || mc.player == null || !mc.player.isAlive()) {
            lockedPlayer = null;
            lastHealth = -1f;
            currentFlickOffset = 0f;
            fistDelayTimer = 0;
            return;
        }

        if (fistDelayTimer > 0) {
            fistDelayTimer--;
        }
        
        if (config.pauseOnScreens && mc.screen != null) return;
        if (config.pauseOnShield && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem) return;

        if (config.hitFlick && currentFlickOffset > 0) {
            float recovery = (float) Math.min(config.hitFlickSpeed, currentFlickOffset);
            mc.player.setXRot(mc.player.getXRot() + recovery);
            currentFlickOffset -= recovery;
        } else if (!config.hitFlick) {
            currentFlickOffset = 0f;
        }

        if (config.tbotWeaponOnly && !isWeapon()) {
            lockedPlayer = null;
            currentTarget = null;
            lastHealth = mc.player.getHealth();
            return;
        }

        if (lastHealth == -1f) lastHealth = mc.player.getHealth();
        float currentHealth = mc.player.getHealth();
        
        if (config.panicSwing && currentHealth < lastHealth) {
            handlePanicSwing(config);
        }
        lastHealth = currentHealth;

        if (config.targetLock) {
            if (lockedPlayer != null) {
                ticksSinceLastHit++;
                if (!lockedPlayer.isAlive() || ticksSinceLastHit >= config.targetLockTicks) {
                    lockedPlayer = null;
                }
            }
        } else {
            lockedPlayer = null;
        }

        HitResult hit = mc.hitResult;
        Entity targeted = null;

        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity hitEntity = ((EntityHitResult) hit).getEntity();
            if (isValid(hitEntity, config)) {
                if (config.targetLock && lockedPlayer != null && hitEntity != lockedPlayer) {
                    targeted = null;
                } else {
                    targeted = hitEntity;
                }
            }
        }

        if (targeted != currentTarget) {
            currentTarget = targeted;
            targetTicks = 0;

            if (config.targetDelayEnabled && targeted != null) {
                int min = config.targetDelayMin;
                int max = Math.max(min, config.targetDelayMax);
                requiredTargetTicks = ThreadLocalRandom.current().nextInt(min, max + 1);
            } else {
                requiredTargetTicks = 0;
            }
        }

        if (currentTarget == null) return;

        if (targetTicks < requiredTargetTicks) {
            targetTicks++;
            return;
        }

        if (delayCheck(config)) {
            mc.gameMode.attack(mc.player, currentTarget);
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (config.tbotFistAsSword && !isWeapon()) {
                fistDelayTimer = 13 + (config.smartRandomDelay > 0 ? ThreadLocalRandom.current().nextInt(0, config.smartRandomDelay + 1) : 0);
            }

            if (config.hitFlick) {
                float flick = (float) ThreadLocalRandom.current().nextDouble(config.hitFlickMin, config.hitFlickMax);
                float targetPitch = mc.player.getXRot() - flick;
                
                if (targetPitch < -90f) {
                    flick = mc.player.getXRot() - (-90f);
                    targetPitch = -90f;
                }
                
                mc.player.setXRot(targetPitch);
                currentFlickOffset += flick;
            }

            if (config.targetLock && currentTarget instanceof Player targetAsPlayer) {
                lockedPlayer = targetAsPlayer;
                ticksSinceLastHit = 0;
            }
        }
    }

    private void handlePanicSwing(ConfigManager.ConfigData config) {
        if (mc.level == null) return;
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isValid(entity, config)) {
                double dist = mc.player.distanceTo(entity);
                if (dist > 3.0 && dist <= config.panicSwingRange) {
                    if (getYawFovDistance(entity) <= config.panicSwingFov) {
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            if (config.tbotFistAsSword && !isWeapon()) {
                                fistDelayTimer = 13 + (config.smartRandomDelay > 0 ? ThreadLocalRandom.current().nextInt(0, config.smartRandomDelay + 1) : 0);
                            }
                        }
                        break; 
                    }
                }
            }
        }
    }

    private boolean isWeapon() {
        if (mc.player == null) return false;
        String itemId = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase();
        return itemId.contains("sword") || itemId.contains("axe");
    }

    private double getYawFovDistance(Entity entity) {
        Vec3 targetPos = entity.getBoundingBox().getCenter();
        double dX = targetPos.x - mc.player.getX();
        double dZ = targetPos.z - mc.player.getZ();
        double yawToTarget = Math.toDegrees(Math.atan2(dZ, dX)) - 90.0;
        return Math.abs(Mth.wrapDegrees(yawToTarget - mc.player.getYRot()));
    }

    private boolean delayCheck(ConfigManager.ConfigData config) {
        if (config.tbotFistAsSword && !isWeapon()) {
            return fistDelayTimer <= 0;
        }

        if (config.smartDelay) {
            if (mc.player.getAttackStrengthScale(0.5f) >= 1.0f) {
                if (smartDelayTimer == -1) {
                    smartDelayTimer = config.smartRandomDelay > 0 ? ThreadLocalRandom.current().nextInt(0, config.smartRandomDelay + 1) : 0;
                }

                if (smartDelayTimer > 0) {
                    smartDelayTimer--;
                    return false;
                } else {
                    smartDelayTimer = -1;
                    return true;
                }
            } else {
                smartDelayTimer = -1;
                return false;
            }
        }

        if (hitDelayTimer > 0) {
            hitDelayTimer--;
            return false;
        } else {
            hitDelayTimer = config.hitDelay;
            if (config.randomDelayEnabled && config.randomDelayMax > 0) {
                hitDelayTimer += ThreadLocalRandom.current().nextInt(0, config.randomDelayMax + 1);
            }
            return true;
        }
    }

    private boolean isValid(Entity entity, ConfigManager.ConfigData config) {
        if (!(entity instanceof LivingEntity livingTarget) || !entity.isAlive()) return false;
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        if (config.tbotPlayersOnly && !(entity instanceof Player)) return false;
        if (entity instanceof Player player && player.isCreative()) return false;
        if (config.ignoreShielding && livingTarget.isBlocking()) return false;
        if (entity instanceof Animal animal && !config.babies && animal.isBaby()) return false;

        return true;
    }
}