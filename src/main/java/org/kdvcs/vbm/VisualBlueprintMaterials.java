/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.kdvcs.vbm.client.GuiOverlay;
import org.kdvcs.vbm.config.VBMConfig;
import org.slf4j.Logger;

@Mod(VisualBlueprintMaterials.MOD_ID)
public class VisualBlueprintMaterials {
    public static final String MOD_ID = "vbm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VisualBlueprintMaterials() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VBMConfig.SPEC, MOD_ID + "-client.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new GuiOverlay());
    }
}