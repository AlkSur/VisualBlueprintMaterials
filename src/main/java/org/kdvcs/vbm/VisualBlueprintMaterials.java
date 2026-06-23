/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kdvcs.vbm.client.GuiOverlay;
import org.kdvcs.vbm.config.VBMConfig;

@Mod(VisualBlueprintMaterials.MOD_ID)
public class VisualBlueprintMaterials {
    public static final String MOD_ID = "create_qbp";
    public VisualBlueprintMaterials(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VBMConfig.SPEC, MOD_ID + "-client.toml");
        modBus.addListener(this::onRegisterKeys);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(VBMConfig::onLoad);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(GuiOverlay.MOVE_KEY);
        event.register(GuiOverlay.MOVE_UP);
        event.register(GuiOverlay.MOVE_DOWN);
        event.register(GuiOverlay.MOVE_LEFT);
        event.register(GuiOverlay.MOVE_RIGHT);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(new GuiOverlay());
    }
}