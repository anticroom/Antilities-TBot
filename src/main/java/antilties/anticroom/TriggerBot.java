package antilties.anticroom;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot {
    private final Minecraft mc = Minecraft.getInstance();

    private int hitDelayTimer = 0;
    private int smartDelayTimer = -1;
    private Entity currentTarget = null;
    private int targetTicks = 0;
    private int requiredTargetTicks = 0;

    public void onTick() {
        ConfigManager.ConfigData config = ConfigManager.config;

        if (!config.enabled || mc.player == null || !mc.player.isAlive()) return;
        if (config.pauseOnScreens && mc.screen != null) return;
        if (config.pauseOnShield && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem) return;

        HitResult hit = mc.hitResult;
        Entity targeted = null;

        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            targeted = ((EntityHitResult) hit).getEntity();
        }

        if (targeted != currentTarget) {
            currentTarget = targeted;
            targetTicks = 0;

            if (config.targetDelayEnabled && targeted != null) {
                int min = config.targetDelayMin;
                int max = Math.max(min, config.targetDelayMax);
                requiredTargetTicks = min + (int) (Math.random() * ((max - min) + 1));
            } else {
                requiredTargetTicks = 0;
            }
        }

        if (currentTarget == null) return;

        if (targetTicks < requiredTargetTicks) {
            targetTicks++;
            return;
        }

        if (delayCheck(config) && isValid(currentTarget, config)) {
            mc.gameMode.attack(mc.player, currentTarget);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private boolean delayCheck(ConfigManager.ConfigData config) {
        if (config.smartDelay) {
            if (mc.player.getAttackStrengthScale(0.5f) >= 1.0f) {
                if (smartDelayTimer == -1) {
                    smartDelayTimer = (int) Math.round(Math.random() * config.smartRandomDelay);
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
            if (config.randomDelayEnabled) {
                hitDelayTimer += (int) Math.round(Math.random() * config.randomDelayMax);
            }
            return true;
        }
    }

private boolean isValid(Entity entity, ConfigManager.ConfigData config) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive()) return false;
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        if (entity instanceof Player player && player.isCreative()) return false;

        LivingEntity livingTarget = (LivingEntity) entity;
        
        if (config.ignoreShielding && livingTarget.isBlocking()) return false;

        if (entity instanceof Animal animal) {
            if (!config.babies && animal.isBaby()) return false;
        }

        return true;
    }
}