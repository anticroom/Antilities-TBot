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
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "anticroom.json");

    public static class ConfigData {
        public boolean enabled = false;
        public boolean pauseOnScreens = true;
        public boolean pauseOnShield = true;
        public boolean ignoreShielding = true;
        public boolean babies = true;

        public boolean smartDelay = true;
        public int smartRandomDelay = 0;
        public int hitDelay = 0;
        public boolean randomDelayEnabled = false;
        public int randomDelayMax = 4;

        public boolean targetDelayEnabled = true;
        public int targetDelayMin = 2;
        public int targetDelayMax = 5;
    }

    public static ConfigData config = new ConfigData();

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                config = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                System.err.println("[Anticroom] Failed to load config!");
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[Anticroom] Failed to save config!");
            e.printStackTrace();
        }
    }
}