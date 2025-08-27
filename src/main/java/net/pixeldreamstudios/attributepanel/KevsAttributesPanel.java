package net.pixeldreamstudios.attributepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
		ServerNetwork.register();
		trackVisibleAttributes();
	}

	public void trackVisibleAttributes() {
		var config = AttributesPanelConfig.INSTANCE;
		if (config.forceTrackVisibleAttributes) {
			for (var section : config.compact.headers) {
				for (var attribute : section.attributes) {
					var id = Identifier.tryParse(attribute.id);
					if (id == null) {
						continue;
					}
					var entry = Registries.ATTRIBUTE.getEntry(id);
					if (entry.isEmpty()) {
						continue;
					}
					entry.get().value().setTracked(true);
				}
			}
		}
	}
}