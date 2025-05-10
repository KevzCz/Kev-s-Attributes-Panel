package net.pixeldreamstudios.attributepanel;

import net.fabricmc.api.ModInitializer;

import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KevsAttributesPanel implements ModInitializer {
	public static final String MOD_ID = "kevs-attributes-panel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AttributesPanelConfig.load();
		LOGGER.info("Initialize Kev's Attributes Panel");
	}
}