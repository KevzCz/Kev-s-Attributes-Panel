package net.pixeldreamstudios.attributepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import net.pixeldreamstudios.attributepanel.network.ClientNetwork;
import net.pixeldreamstudios.attributepanel.network.ServerNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KevsAttributesPanel implements ModInitializer {
	public static final String MOD_ID = "kevs-attributes-panel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AttributesPanelConfig.load();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			ClientNetwork.register();
		}
		ServerNetwork.register();
		AttributeSnapshotCommand.register();
	}
}