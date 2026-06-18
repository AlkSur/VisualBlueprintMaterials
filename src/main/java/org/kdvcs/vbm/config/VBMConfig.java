/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.kdvcs.vbm.VisualBlueprintMaterials;

@Mod.EventBusSubscriber(modid = VisualBlueprintMaterials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VBMConfig {
    static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BUTTON_X;
    public static final ForgeConfigSpec.IntValue BUTTON_Y;

    static {
        BUILDER.comment("Button position and behavior").push("button");
        BUTTON_X = BUILDER.comment("X offset from right edge").defineInRange("buttonX", -20, -2000, 2000);
        BUTTON_Y = BUILDER.comment("Y offset from top").defineInRange("buttonY", 60, -2000, 2000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int buttonX, buttonY;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        buttonX = BUTTON_X.get();
        buttonY = BUTTON_Y.get();
    }
}