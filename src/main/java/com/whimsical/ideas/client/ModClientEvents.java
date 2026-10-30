package com.whimsical.ideas.client;

import com.whimsical.ideas.WhimsicalIdeas;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = WhimsicalIdeas.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    // ==========================================
    // 1. 注册按键 (小键盘 9)
    // ==========================================
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_MUSIC);
    }

    // ==========================================
    // 2. 客户端初始化 (注册配置界面)
    // ==========================================
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册 Mod 列表里的“配置”按钮
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parentScreen) -> new MusicConfigScreen(parentScreen)
                    )
            );
        });
    }

    // ==========================================
    // 3. 监听游戏内按键 (在 FORGE 总线)
    // ==========================================
    @Mod.EventBusSubscriber(modid = WhimsicalIdeas.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            // 如果按下了小键盘 9，就打开音乐界面
            if (KeyBindings.OPEN_MUSIC.consumeClick()) {
                Minecraft.getInstance().setScreen(new MusicScreen());
            }
        }
    }
}