package antilties.anticroom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "antilities.json");

    public enum SmoothingMode {
        EXPONENTIAL, LINEAR
    }
 
    public static class ConfigData {
        public boolean enabled = false;
        public boolean pauseOnScreens = true;
        public boolean pauseOnShield = true;
        public boolean ignoreShielding = true;
        public boolean babies = true;
        
        public boolean tbotWeaponOnly = false;
        public boolean tbotPlayersOnly = false;

        public boolean smartDelay = true;
        public int smartRandomDelay = 1;
        public int hitDelay = 0;
        public boolean randomDelayEnabled = false;
        public int randomDelayMax = 4;
        public boolean tbotFistAsSword = true;

        public boolean targetDelayEnabled = true;
        public int targetDelayMin = 2;
        public int targetDelayMax = 5;

        public boolean targetLock = true;
        public int targetLockTicks = 60;
        public boolean showToggleNotifs = true;

        public boolean panicSwing = true;
        public double panicSwingRange = 5.0;
        public double panicSwingFov = 30.0;

        public boolean hitFlick = true;
        public double hitFlickMin = 0.2;
        public double hitFlickMax = 0.3;
        public double hitFlickSpeed = 0.05;

        public boolean aimAssistEnabled = false;
        public boolean aimAssistWeaponOnly = true;
        public int aimAssistDelay = 100;
        public double aimAssistMaxDistance = 4.0; 
        public double aimAssistFov = 20.0;
        public boolean aimAssistFovFalloff = true;
        public double aimAssistSmoothing = 1.5;
        public SmoothingMode smoothingMode = SmoothingMode.EXPONENTIAL;
        public boolean aimAssistXOnly = false;
        public boolean aimAssistYOnly = false;
        
        public boolean aimAssistDrift = true;
        public double aimAssistDriftAmplitude = 0.05;
        public double aimAssistVerticalOffset = 0.2;
        public double aimAssistHitboxEdge = 0.5;
        public double aimAssistVariance = 0.05;
        
        public boolean renderFovCircle = false;
        public boolean renderTargetLine = false;
    }

    public static ConfigData config = new ConfigData();

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                config = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
        }
    }
}