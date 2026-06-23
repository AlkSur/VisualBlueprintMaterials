/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class VBMConfig {
    static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BUTTON_X;
    public static final ModConfigSpec.IntValue BUTTON_Y;
    public static final ModConfigSpec.BooleanValue MOVE_PANEL_X;
    public static final ModConfigSpec.BooleanValue MOVE_PANEL_Y;

    static {
        BUILDER.comment("Button position and behavior").push("button");
        BUTTON_X = BUILDER.comment("X offset from right edge").defineInRange("buttonX", 2, -2000, 2000);
        BUTTON_Y = BUILDER.comment("Y offset from top").defineInRange("buttonY", -69, -2000, 2000);
        MOVE_PANEL_X = BUILDER.comment("Moving button left/right also moves panel").define("movePanelX", false);
        MOVE_PANEL_Y = BUILDER.comment("Moving button up/down also moves panel").define("movePanelY", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int buttonX, buttonY;
    public static boolean movePanelX, movePanelY;

    public static void onLoad(final ModConfigEvent event) {
        buttonX = BUTTON_X.get();
        buttonY = BUTTON_Y.get();
        movePanelX = MOVE_PANEL_X.get();
        movePanelY = MOVE_PANEL_Y.get();
    }
}