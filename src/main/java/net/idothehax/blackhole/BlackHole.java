package net.idothehax.blackhole;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.idothehax.blackhole.config.BlackHoleConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BlackHole implements ModInitializer {
    public static final String MOD_ID = "blackhole";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Register ChunkTicketType for 1.21.5+ (Record-based system)
    public static final ChunkTicketType BLACK_HOLE_TICKET_TYPE =
            Registry.register(
                    Registries.TICKET_TYPE,
                    Identifier.of(MOD_ID, "black_hole"),
                    new ChunkTicketType(300L, true, ChunkTicketType.Use.LOADING_AND_SIMULATION)
            );

    public static BlackHoleBlock BLACK_HOLE_BLOCK;
    public static BlockItem BLACK_HOLE_ITEM;
    public static BlockEntityType<BlackHoleBlockEntity> BLACK_HOLE_BLOCK_ENTITY;

    public static RegistryKey<ItemGroup> ITEM_GROUP_KEY;
    public static ItemGroup ITEM_GROUP;

    @Override
    public void onInitialize() {
        LOGGER.info("Black-hole Mod is Sucking Up The Minecraft Source Code...");

        // Create block
        BLACK_HOLE_BLOCK = new BlackHoleBlock(
                AbstractBlock.Settings.create()
                        .requiresTool()
                        .luminance(state -> 3)
                        .strength(6.0F, 120.0F)
                        .emissiveLighting(Blocks::always)
                        .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "black_hole")))
                        .lootTable(Optional.of(RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(MOD_ID, "blocks/black_hole"))))
        );

        // Initialize Polymer resources BEFORE registration
        BLACK_HOLE_BLOCK.initPolymerResources();

        // Now register the block
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "black_hole"), BLACK_HOLE_BLOCK);

        // Register item with registry key in settings
        BLACK_HOLE_ITEM = new BlackHoleItem(
                new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "black_hole"))),
                BLACK_HOLE_BLOCK
        );
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "black_hole"), BLACK_HOLE_ITEM);

        // Register block entity
        BLACK_HOLE_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(BlackHoleBlockEntity::new, BLACK_HOLE_BLOCK).build(null);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "black_hole_block_entity"), BLACK_HOLE_BLOCK_ENTITY);

        // Register item group
        ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(MOD_ID, "blackhole_group"));
        ITEM_GROUP = ItemGroup.create(ItemGroup.Row.TOP, 0)
                .displayName(Text.translatable("itemGroup.blackhole").formatted(Formatting.AQUA))
                .icon(() -> new ItemStack(BLACK_HOLE_ITEM))
                .entries((displayContext, entries) -> entries.add(BLACK_HOLE_ITEM))
                .build();
        Registry.register(Registries.ITEM_GROUP, ITEM_GROUP_KEY, ITEM_GROUP);

        // Add to vanilla creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.add(BLACK_HOLE_ITEM));

        // Load config
        BlackHoleConfig.loadConfig();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BlackHoleCommands.register(dispatcher);
        });

        // Polymer pack
        PolymerBlockUtils.registerBlockEntity(BLACK_HOLE_BLOCK_ENTITY);
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();
    }
}