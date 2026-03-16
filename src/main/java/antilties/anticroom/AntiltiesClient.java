package antilties.anticroom;

import org.lwjgl.glfw.GLFW;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AntiltiesClient implements ClientModInitializer {
    public static final TriggerBot TRIGGER_BOT = new TriggerBot();
    public static final AimAssist AIM_ASSIST = new AimAssist();
    
    private static KeyMapping toggleKey;
    private static KeyMapping toggleAimAssistKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.anticroom.toggle",
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.MISC
        ));

        toggleAimAssistKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.anticroom.toggle_aimassist",
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.MISC
        ));

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String trimmed = message.trim();
            if (trimmed.equals("-tbot")) {
                openTbotMenu();
                return false; 
            } else if (trimmed.equals("-aimassist")) {
                openAimAssistMenu();
                return false; 
            }
            return true;
        });

        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String trimmed = command.trim();
            if (trimmed.equals("-tbot")) {
                openTbotMenu();
                return false; 
            } else if (trimmed.equals("-aimassist")) {
                openAimAssistMenu();
                return false; 
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                ConfigManager.config.enabled = !ConfigManager.config.enabled;
                ConfigManager.save();
                if (client.player != null && ConfigManager.config.showToggleNotifs) {
                    client.player.displayClientMessage(Component.literal("§7TriggerBot: " + (ConfigManager.config.enabled ? "§aON" : "§cOFF")), true);
                }
            }

            while (toggleAimAssistKey.consumeClick()) {
                ConfigManager.config.aimAssistEnabled = !ConfigManager.config.aimAssistEnabled;
                ConfigManager.save();
                if (client.player != null && ConfigManager.config.showToggleNotifs) {
                    client.player.displayClientMessage(Component.literal("§7Aim Assist: " + (ConfigManager.config.aimAssistEnabled ? "§aON" : "§cOFF")), true);
                }
            }
            
            TRIGGER_BOT.onTick();
            AIM_ASSIST.onTick();
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            float partialTicks = tickCounter.getGameTimeDeltaPartialTick(true);
            AIM_ASSIST.onFrame(partialTicks);

            if (!ConfigManager.config.aimAssistEnabled || !ConfigManager.config.renderFovCircle) return;
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            
            double fovRadius = (ConfigManager.config.aimAssistFov / client.options.fov().get()) * (screenWidth / 2.0);

            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;
            int color = 0x88FFFFFF;

            int points = (int) (2 * Math.PI * fovRadius); 

            for (int i = 0; i < points; i++) { 
                double angle = (i * 2 * Math.PI) / points;
                int x = centerX + (int) Math.round(Math.cos(angle) * fovRadius);
                int y = centerY + (int) Math.round(Math.sin(angle) * fovRadius);
                
                guiGraphics.fill(x, y, x + 1, y + 1, color);
            }
        });
    }

    private void openTbotMenu() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(client.screen)
                    .setTitle(Component.literal("TriggerBot Settings"));

            builder.setSavingRunnable(ConfigManager::save);
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            
            ConfigCategory generalCategory = builder.getOrCreateCategory(Component.literal("General"));
            ConfigCategory conditionsCategory = builder.getOrCreateCategory(Component.literal("Targeting Conditions"));
            ConfigCategory delaysCategory = builder.getOrCreateCategory(Component.literal("Hit Delays"));

            generalCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable TriggerBot"), ConfigManager.config.enabled)
                    .setTooltip(Component.literal("Toggles the TriggerBot module on or off."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.enabled = v).build());
                    
            generalCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Target Lock"), ConfigManager.config.targetLock)
                    .setTooltip(Component.literal("Forces the bot to only attack your current target until the lock expires or they die."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.targetLock = v).build());

            generalCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Target Lock Duration (Ticks)"), ConfigManager.config.targetLockTicks, 0, 100)
                    .setTooltip(Component.literal("How many ticks to keep the target locked after the last hit (20 ticks = 1 second)."))
                    .setDefaultValue(60).setSaveConsumer(v -> ConfigManager.config.targetLockTicks = v).build());

            generalCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Chat Notifications"), ConfigManager.config.showToggleNotifs)
                    .setTooltip(Component.literal("Shows a client-side chat message when you toggle the module via keybind."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.showToggleNotifs = v).build());

            generalCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Hit Flick"), ConfigManager.config.hitFlick)
                    .setTooltip(Component.literal("Simulates a small, sudden vertical mouse flick upon hitting to look more human on recordings."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.hitFlick = v).build());

            generalCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Hit Flick Min"), ConfigManager.config.hitFlickMin)
                    .setTooltip(Component.literal("Minimum degrees the camera will flick upwards on a hit."))
                    .setDefaultValue(0.2).setMin(0.0).setMax(5.0).setSaveConsumer(v -> ConfigManager.config.hitFlickMin = v).build());

            generalCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Hit Flick Max"), ConfigManager.config.hitFlickMax)
                    .setTooltip(Component.literal("Maximum degrees the camera will flick upwards on a hit."))
                    .setDefaultValue(0.3).setMin(0.0).setMax(5.0).setSaveConsumer(v -> ConfigManager.config.hitFlickMax = v).build());

            generalCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Hit Flick Speed"), ConfigManager.config.hitFlickSpeed)
                    .setTooltip(Component.literal("How fast the camera recovers back down to its original pitch after a flick."))
                    .setDefaultValue(0.05).setMin(0.01).setMax(1.0).setSaveConsumer(v -> ConfigManager.config.hitFlickSpeed = v).build());
            
            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Weapon Only"), ConfigManager.config.tbotWeaponOnly)
                    .setTooltip(Component.literal("Only allows the TriggerBot to attack if you are holding a sword or an axe."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.tbotWeaponOnly = v).build());

            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Players Only"), ConfigManager.config.tbotPlayersOnly)
                    .setTooltip(Component.literal("Ignores all mobs and animals, only attacking other players."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.tbotPlayersOnly = v).build());

            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Pause while in Menus"), ConfigManager.config.pauseOnScreens)
                    .setTooltip(Component.literal("Stops attacking while you have an inventory, chest, or other GUI open."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.pauseOnScreens = v).build());
                    
            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Pause while Shielding"), ConfigManager.config.pauseOnShield)
                    .setTooltip(Component.literal("Stops attacking if you are currently holding right-click with a shield."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.pauseOnShield = v).build());
                    
            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Ignore Shielding Enemies"), ConfigManager.config.ignoreShielding)
                    .setTooltip(Component.literal("Prevents the bot from attacking enemies who are actively blocking with a shield."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.ignoreShielding = v).build());

            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Attack Baby Animals"), ConfigManager.config.babies)
                    .setTooltip(Component.literal("Allows the bot to attack baby animal variants."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.babies = v).build());

            conditionsCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Panic Swing (When Comboed)"), ConfigManager.config.panicSwing)
                    .setTooltip(Component.literal("Randomly clicks when you take damage from an enemy just outside your reach to simulate a panicking human."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.panicSwing = v).build());

            conditionsCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Panic Swing Max Range"), ConfigManager.config.panicSwingRange)
                    .setTooltip(Component.literal("Maximum distance the enemy can be from you to trigger a panic swing."))
                    .setDefaultValue(5.0).setMin(3.0).setMax(10.0).setSaveConsumer(v -> ConfigManager.config.panicSwingRange = v).build());

            conditionsCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Panic Swing FOV"), ConfigManager.config.panicSwingFov)
                    .setTooltip(Component.literal("How close to your crosshair the out-of-reach enemy must be to trigger a panic swing."))
                    .setDefaultValue(30.0).setMin(1.0).setMax(180.0).setSaveConsumer(v -> ConfigManager.config.panicSwingFov = v).build());

            delaysCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Use Vanilla 1.9+ Cooldown"), ConfigManager.config.smartDelay)
                    .setTooltip(Component.literal("Waits for the weapon's attack speed indicator to fully recharge before swinging."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.smartDelay = v).build());
                    
            delaysCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Fist Sword-Delay"), ConfigManager.config.tbotFistAsSword)
                    .setTooltip(Component.literal("Forces empty hands or non-weapons to use a sword's 1.9+ attack delay to prevent spam clicking."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.tbotFistAsSword = v).build());

            delaysCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Vanilla Cooldown Randomizer"), ConfigManager.config.smartRandomDelay, 0, 10)
                    .setTooltip(Component.literal("Adds random extra ticks of delay to the 1.9+ cooldown to appear less robotic to anticheats."))
                    .setDefaultValue(0).setSaveConsumer(v -> ConfigManager.config.smartRandomDelay = v).build());

            delaysCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Delay Before First Hit"), ConfigManager.config.targetDelayEnabled)
                    .setTooltip(Component.literal("Waits a brief moment before swinging when you aim at a new target."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.targetDelayEnabled = v).build());
                    
            delaysCategory.addEntry(entryBuilder.startIntSlider(Component.literal("First Hit Min Delay"), ConfigManager.config.targetDelayMin, 0, 20)
                    .setTooltip(Component.literal("Minimum ticks to wait before the first attack on a new target."))
                    .setDefaultValue(2).setSaveConsumer(v -> ConfigManager.config.targetDelayMin = v).build());
                    
            delaysCategory.addEntry(entryBuilder.startIntSlider(Component.literal("First Hit Max Delay"), ConfigManager.config.targetDelayMax, 0, 20)
                    .setTooltip(Component.literal("Maximum ticks to wait before the first attack on a new target."))
                    .setDefaultValue(5).setSaveConsumer(v -> ConfigManager.config.targetDelayMax = v).build());

            delaysCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Manual Hit Delay (For 1.8 pvp)"), ConfigManager.config.hitDelay, 0, 60)
                    .setTooltip(Component.literal("Raw tick delay between hits. Only active if Vanilla Cooldown is OFF (useful for 1.8 PvP servers)."))
                    .setDefaultValue(0).setSaveConsumer(v -> ConfigManager.config.hitDelay = v).build());

            delaysCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Randomize Manual Delay"), ConfigManager.config.randomDelayEnabled)
                    .setTooltip(Component.literal("Adds random variation to the manual 1.8 hit delay."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.randomDelayEnabled = v).build());

            delaysCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Manual Randomizer Max"), ConfigManager.config.randomDelayMax, 0, 20)
                    .setTooltip(Component.literal("Maximum extra random ticks added to the manual 1.8 hit delay."))
                    .setDefaultValue(4).setSaveConsumer(v -> ConfigManager.config.randomDelayMax = v).build());

            client.setScreen(builder.build());
        });
    }

    private void openAimAssistMenu() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(client.screen)
                    .setTitle(Component.literal("Aim Assist Settings"));

            builder.setSavingRunnable(ConfigManager::save);
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            
            ConfigCategory mainCategory = builder.getOrCreateCategory(Component.literal("General"));
            ConfigCategory advancedCategory = builder.getOrCreateCategory(Component.literal("Advanced Tweaks"));
            ConfigCategory visualCategory = builder.getOrCreateCategory(Component.literal("Visuals & Misc"));

            mainCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Aim Assist"), ConfigManager.config.aimAssistEnabled)
                    .setTooltip(Component.literal("Toggles the Aim Assist module on or off."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.aimAssistEnabled = v).build());

            mainCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Weapon Only"), ConfigManager.config.aimAssistWeaponOnly)
                    .setTooltip(Component.literal("Only activates Aim Assist when you are actively holding a sword or axe."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.aimAssistWeaponOnly = v).build());

            mainCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Aim Delay (ms)"), ConfigManager.config.aimAssistDelay, 0, 500)
                    .setTooltip(Component.literal("Delay in milliseconds to simulate human reaction time before aiming at a new target."))
                    .setDefaultValue(100).setSaveConsumer(v -> ConfigManager.config.aimAssistDelay = v).build());

            mainCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Max Lock Distance (Blocks)"), ConfigManager.config.aimAssistMaxDistance)
                    .setTooltip(Component.literal("Maximum distance in blocks to lock onto a target."))
                    .setDefaultValue(4.0).setMin(1.0).setMax(100.0).setSaveConsumer(v -> ConfigManager.config.aimAssistMaxDistance = v).build());

            mainCategory.addEntry(entryBuilder.startDoubleField(Component.literal("FOV Targeting Range"), ConfigManager.config.aimAssistFov)
                    .setTooltip(Component.literal("How close to your crosshair an enemy must be (in degrees) to be targeted."))
                    .setDefaultValue(20.0).setMin(1.0).setMax(180.0).setSaveConsumer(v -> ConfigManager.config.aimAssistFov = v).build());

            advancedCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Dynamic FOV Falloff"), ConfigManager.config.aimAssistFovFalloff)
                    .setTooltip(Component.literal("Aim assist gradually gets stronger and sticks more the closer the target is to the center of your screen."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.aimAssistFovFalloff = v).build());

            advancedCategory.addEntry(entryBuilder.startEnumSelector(Component.literal("Smoothing Algorithm"), ConfigManager.SmoothingMode.class, ConfigManager.config.smoothingMode)
                    .setTooltip(Component.literal("Mathematical method used to move the camera (EXPONENTIAL is smoother, LINEAR is more direct)."))
                    .setDefaultValue(ConfigManager.SmoothingMode.EXPONENTIAL).setSaveConsumer(v -> ConfigManager.config.smoothingMode = v).build());

            advancedCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Smoothing Speed (Higher = Slower)"), ConfigManager.config.aimAssistSmoothing)
                    .setTooltip(Component.literal("How heavily the camera movement is smoothed. Higher values mean slower, less snapping aim."))
                    .setDefaultValue(1.5).setMin(1.0).setMax(50.0).setSaveConsumer(v -> ConfigManager.config.aimAssistSmoothing = v).build());

            advancedCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Vertical Aim Offset"), ConfigManager.config.aimAssistVerticalOffset)
                    .setTooltip(Component.literal("Adjusts where on the target's body to aim. 0 = feet, positive values aim higher up the body."))
                    .setDefaultValue(0.2).setMin(-0.5).setMax(0.5).setSaveConsumer(v -> ConfigManager.config.aimAssistVerticalOffset = v).build());

            advancedCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Hitbox Edge Preference (0=Center, 1=Edge)"), ConfigManager.config.aimAssistHitboxEdge)
                    .setTooltip(Component.literal("Blends aiming between the absolute center (0.0) and the closest edge (1.0) of the target's hitbox."))
                    .setDefaultValue(0.5).setMin(0.0).setMax(1.0).setSaveConsumer(v -> ConfigManager.config.aimAssistHitboxEdge = v).build());

            advancedCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Aim Variance / Micro-noise"), ConfigManager.config.aimAssistVariance)
                    .setTooltip(Component.literal("Adds randomized micro-noise to the aiming point to simulate imperfect human tracking."))
                    .setDefaultValue(0.05).setMin(0.0).setMax(0.5).setSaveConsumer(v -> ConfigManager.config.aimAssistVariance = v).build());

            advancedCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Smooth Sine Drift"), ConfigManager.config.aimAssistDrift)
                    .setTooltip(Component.literal("Adds a subtle, slowly shifting sine-wave drift to the target point so it doesn't look completely static."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.aimAssistDrift = v).build());

            advancedCategory.addEntry(entryBuilder.startDoubleField(Component.literal("Drift Amplitude (Size)"), ConfigManager.config.aimAssistDriftAmplitude)
                    .setTooltip(Component.literal("How wide the smooth sine drift spins. Lower = tighter optimal aim, Higher = wider tracking."))
                    .setDefaultValue(0.05).setMin(0.0).setMax(1.0).setSaveConsumer(v -> ConfigManager.config.aimAssistDriftAmplitude = v).build());

            advancedCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("X-Axis (Yaw) Only"), ConfigManager.config.aimAssistXOnly)
                    .setTooltip(Component.literal("Only tracks targets left and right, completely ignoring vertical movement."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.aimAssistXOnly = v).build());

            advancedCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Y-Axis (Pitch) Only"), ConfigManager.config.aimAssistYOnly)
                    .setTooltip(Component.literal("Only tracks targets up and down, completely ignoring horizontal movement."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.aimAssistYOnly = v).build());

            visualCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Render FOV Circle"), ConfigManager.config.renderFovCircle)
                    .setTooltip(Component.literal("Draws a circle on your screen showing the active FOV targeting area."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.renderFovCircle = v).build());

            visualCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Render Target Line"), ConfigManager.config.renderTargetLine)
                    .setTooltip(Component.literal("Draws a particle line showing exactly where the Aim Assist is trying to lock onto."))
                    .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.renderTargetLine = v).build());

            visualCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Chat Notifications"), ConfigManager.config.showToggleNotifs)
                    .setTooltip(Component.literal("Shows a client-side chat message when you toggle the module via keybind."))
                    .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.showToggleNotifs = v).build());

            client.setScreen(builder.build());
        });
    }
}