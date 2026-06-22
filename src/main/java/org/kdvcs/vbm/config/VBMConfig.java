/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.kdvcs.vbm.VisualBlueprintMaterials;

public class VBMConfig {
    static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BUTTON_X;
    public static final ModConfigSpec.IntValue BUTTON_Y;

    static {
        BUILDER.comment("Button position and behavior").push("button");
        BUTTON_X = BUILDER.comment("X offset from right edge").defineInRange("buttonX", 2, -2000, 2000);
        BUTTON_Y = BUILDER.comment("Y offset from top").defineInRange("buttonY", -69, -2000, 2000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int buttonX, buttonY;

    public static void onLoad(final ModConfigEvent event) {
        buttonX = BUTTON_X.get();
        buttonY = BUTTON_Y.get();
    }
}