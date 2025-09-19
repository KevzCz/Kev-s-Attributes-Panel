package net.pixeldreamstudios.attributepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
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
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(
					Identifier.of(MOD_ID, "kap_minimal_dark"),
					modContainer,
					Text.literal("KAP Minimal Dark"),
					ResourcePackActivationType.NORMAL
			);
			ResourceManagerHelper.registerBuiltinResourcePack(
					Identifier.of(MOD_ID, "kevs_attributes_panel_old"),
					modContainer,
					Text.literal("Old Panel Icons"),
					ResourcePackActivationType.NORMAL
			);
		});
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