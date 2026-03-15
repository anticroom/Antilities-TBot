package antilties.anticroom;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Antilties implements ModInitializer {
	public static final String MOD_ID = "antilties";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("[LOG] Antilities Loaded!");
	}
}