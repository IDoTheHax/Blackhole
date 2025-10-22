package net.idothehax.blackhole;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collections;

public class BlackHoleItem extends BlockItem implements PolymerItem {

    public BlackHoleItem(Item.Settings settings, Block block) {
        super(block, settings);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        ItemStack stack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);

        // Set custom model data using the correct constructor for Minecraft 1.21+
        stack.set(
            DataComponentTypes.CUSTOM_MODEL_DATA,
            new CustomModelDataComponent(
                Collections.singletonList(1.0f), // floats: model data value as float
                Collections.emptyList(),         // flags: no flags
                Collections.emptyList(),         // strings: no strings
                Collections.emptyList()          // colors: no colors
            )
        );

        return stack;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BLACK_DYE;
    }
}
