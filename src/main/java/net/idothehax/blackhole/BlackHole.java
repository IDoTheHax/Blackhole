package net.idothehax.blackhole;

import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.idothehax.blackhole.config.BlackHoleConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlackHole implements ModInitializer {
    public static final String MOD_ID = "blackhole";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ChunkTicketType<BlockPos> BLACK_HOLE_TICKET_TYPE =
            ChunkTicketType.<BlockPos>create(
                    String.valueOf(Identifier.of(MOD_ID, "blackhole")),
                    // Comparator now knows it's comparing BlockPos objects
                    (pos1, pos2) -> {
                        if (pos1.getX() != pos2.getX()) {
                            return Integer.compare(pos1.getX(), pos2.getX());
                        }
                        if (pos1.getY() != pos2.getY()) {
                            return Integer.compare(pos1.getY(), pos2.getY());
                        }
                        return Integer.compare(pos1.getZ(), pos2.getZ());
                    },
                    300
            );

    public static final Block BLACK_HOLE_BLOCK = Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "black_hole"), new BlackHoleBlock(AbstractBlock.Settings.create()
            .requiresTool()
            .luminance(state -> 3)
            .strength(6.0F, 120.0F)
            .emissiveLighting(Blocks::always)));

    public static final BlockItem BLACK_HOLE_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "black_hole"), new BlackHoleItem(new Item.Settings(), BLACK_HOLE_BLOCK, "black_hole"));

    public static final BlockEntityType<BlackHoleBlockEntity> BLACK_HOLE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MOD_ID, "black_hole_block_entity"),
            BlockEntityType.Builder.create(BlackHoleBlockEntity::new, BLACK_HOLE_BLOCK).build()
    );

    public static final ItemGroup ITEM_GROUP = new ItemGroup.Builder(null, -1)
            .displayName(Text.translatable("blackhole.itemgroup").formatted(Formatting.AQUA))
            .icon(()-> new ItemStack(BLACK_HOLE_ITEM))
            .entries((displayContext, entries) -> Registries.ITEM.streamEntries()
                    .filter(itemReference -> itemReference.getKey().map(key -> key.getValue().getNamespace().equals(MOD_ID)).orElse(false))
                    .forEach(item -> entries.add(new ItemStack(item))))
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("Black-hole Mod is Sucking Up The Minecraft Source Code...");

        // Load Config
        BlackHoleConfig.loadConfig();

        // Register Commando
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BlackHoleCommands.register(dispatcher);
        });

        // Register The Blackhole Block Entity
        PolymerBlockUtils.registerBlockEntity(BLACK_HOLE_BLOCK_ENTITY);

        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();
    }
}
