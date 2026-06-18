/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class JeiBridge implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation("create-qbp", "jei_bridge");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Called via reflection from GuiOverlay â€?safe because Class.forName only runs when JEI is loaded */
    public static void showRecipe(ItemStack stack, int key) {
        if (runtime == null) return;
        RecipeIngredientRole role = (key == org.lwjgl.glfw.GLFW.GLFW_KEY_R)
                ? RecipeIngredientRole.OUTPUT   // R key â†?how to craft this
                : RecipeIngredientRole.INPUT;   // U key â†?what this is used for
        IIngredientType<ItemStack> type = VanillaTypes.ITEM_STACK;
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        runtime.getRecipesGui().show(List.of(
                focusFactory.createFocus(role, type, stack)
        ));
    }
}
