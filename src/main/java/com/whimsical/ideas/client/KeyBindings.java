package com.whimsical.ideas.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    // 小键盘 9 的 GLFW 键码
    public static final KeyMapping OPEN_MUSIC = new KeyMapping(
            "key.whimsical_ideas.open_music",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            "key.categories.whimsical_ideas"
    );
}