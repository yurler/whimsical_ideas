package com.whimsical.ideas;

import com.mojang.logging.LogUtils;
import com.whimsical.ideas.client.MusicManager;
import com.whimsical.ideas.network.ModNetwork;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(WhimsicalIdeas.MOD_ID)
public class WhimsicalIdeas {

    public static final String MOD_ID = "whimsical_ideas";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    // ---------- 方块与物品注册 ----------
    public static final RegistryObject<Block> PLUSHIE_BLOCK = BLOCKS.register("plushie_doll",
            () -> new PlushieBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.5F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)
            ));

    public static final RegistryObject<Block> PLUSHIE_CRAFTING_TABLE_BLOCK =
            BLOCKS.register("plushie_crafting_table",
                    () -> new PlushieCraftingTableBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)
                    ));

    public static final RegistryObject<Item> PLUSHIE_ITEM = ITEMS.register("plushie_doll",
            () -> new PlushieItem(PLUSHIE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> BLANK_PLUSHIE_ITEM = ITEMS.register("blank_plushie",
            () -> new BlankPlushieItem(new Item.Properties()));

    public static final RegistryObject<Item> PLUSHIE_CRAFTING_TABLE_ITEM =
            ITEMS.register("plushie_crafting_table",
                    () -> new BlockItem(PLUSHIE_CRAFTING_TABLE_BLOCK.get(), new Item.Properties()));

    // ---------- 构造函数 ----------
    public WhimsicalIdeas(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        // 注册基础内容
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModNetwork.register();
        MusicManager.init();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, Config.SPEC);
    }

    // ---------- 客户端事件 ----------
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ModModelLayers.PLUSHIE_LAYER, PlushieModel::createLayer);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.PLUSHIE_BE.get(), PlushieRenderer::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // 注册玩偶制作台 GUI
                MenuScreens.register(
                        ModMenuTypes.PLUSHIE_CRAFTING_TABLE.get(),
                        PlushieCraftingTableScreen::new
                );
            });
        }
    }
}