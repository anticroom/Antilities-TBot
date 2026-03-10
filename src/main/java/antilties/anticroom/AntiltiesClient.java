package antilties.anticroom;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class AntiltiesClient implements ClientModInitializer {
    public static final TriggerBot TRIGGER_BOT = new TriggerBot();
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

                toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                                "key.anticroom.toggle",
                                GLFW.GLFW_KEY_R,
                                KeyMapping.Category.MISC
                        ));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("-tbot")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    
                    client.execute(() -> {
                        ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(client.screen)
                                .setTitle(Component.literal("Anticroom Settings"));

                        builder.setSavingRunnable(() -> ConfigManager.save());

                        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
                        
                        ConfigCategory combatCategory = builder.getOrCreateCategory(Component.literal("Combat"));
                        ConfigCategory timingCategory = builder.getOrCreateCategory(Component.literal("Timing"));

                        combatCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("TriggerBot Enabled"), ConfigManager.config.enabled)
                                .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.enabled = v).build());
                        
                        combatCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Pause on Screens"), ConfigManager.config.pauseOnScreens)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.pauseOnScreens = v).build());
                                
                        combatCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Pause on Shield"), ConfigManager.config.pauseOnShield)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.pauseOnShield = v).build());
                                
                        combatCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Attack Babies"), ConfigManager.config.babies)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.babies = v).build());
                                combatCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Ignore Shielding Targets"), ConfigManager.config.ignoreShielding)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.ignoreShielding = v).build());
                        timingCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Smart Delay (Vanilla Cooldown)"), ConfigManager.config.smartDelay)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.smartDelay = v).build());
                                
                        timingCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Smart Random Delay"), ConfigManager.config.smartRandomDelay, 0, 10)
                                .setDefaultValue(0).setSaveConsumer(v -> ConfigManager.config.smartRandomDelay = v).build());

                        timingCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Target Delay Enabled"), ConfigManager.config.targetDelayEnabled)
                                .setDefaultValue(true).setSaveConsumer(v -> ConfigManager.config.targetDelayEnabled = v).build());
                                
                        timingCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Target Delay Min"), ConfigManager.config.targetDelayMin, 0, 20)
                                .setDefaultValue(2).setSaveConsumer(v -> ConfigManager.config.targetDelayMin = v).build());
                                
                        timingCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Target Delay Max"), ConfigManager.config.targetDelayMax, 0, 20)
                                .setDefaultValue(5).setSaveConsumer(v -> ConfigManager.config.targetDelayMax = v).build());

                        timingCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Hit Delay (If Smart Delay OFF)"), ConfigManager.config.hitDelay, 0, 60)
                                .setDefaultValue(0).setSaveConsumer(v -> ConfigManager.config.hitDelay = v).build());

                        timingCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Random Delay Enabled"), ConfigManager.config.randomDelayEnabled)
                                .setDefaultValue(false).setSaveConsumer(v -> ConfigManager.config.randomDelayEnabled = v).build());

                        timingCategory.addEntry(entryBuilder.startIntSlider(Component.literal("Random Delay Max"), ConfigManager.config.randomDelayMax, 0, 20)
                                .setDefaultValue(4).setSaveConsumer(v -> ConfigManager.config.randomDelayMax = v).build());

                        client.setScreen(builder.build());
                    });
                    
                    return 1;
                }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                ConfigManager.config.enabled = !ConfigManager.config.enabled;
                ConfigManager.save();
                
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("§7TriggerBot: " + (ConfigManager.config.enabled ? "§aON" : "§cOFF")), true);
                }
            }
            
            TRIGGER_BOT.onTick();
        });
    }
}