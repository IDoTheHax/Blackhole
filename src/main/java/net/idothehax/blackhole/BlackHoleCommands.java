package net.idothehax.blackhole;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.idothehax.blackhole.config.BlackHoleConfig;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class BlackHoleCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("blackhole")
                .requires(source -> source.hasPermissionLevel(2)) // Only ops can use
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("property", StringArgumentType.string())
                                .executes(context -> {
                                    String property = StringArgumentType.getString(context, "property");
                                    ServerCommandSource source = context.getSource();
                                    String response = getProperty(property);
                                    source.sendFeedback(() -> Text.literal(response), false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("property", StringArgumentType.string())
                                .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            String property = StringArgumentType.getString(context, "property");
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            ServerCommandSource source = context.getSource();
                                            String response = setProperty(property, value);
                                            source.sendFeedback(() -> Text.literal(response), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(CommandManager.literal("togglefollow")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerWorld world = source.getWorld();
                            Vec3d pos = source.getPosition();
                            BlockPos blockPos = BlockPos.ofFloored(pos);

                            BlockEntity entity = world.getBlockEntity(blockPos);
                            if (entity instanceof BlackHoleBlockEntity blackHole) {
                                blackHole.toggleFollowing();
                                source.sendFeedback(() -> Text.literal("Black hole following toggled to " + blackHole.isFollowing()), true);
                                return Command.SINGLE_SUCCESS;
                            } else {
                                source.sendError(Text.literal("No black hole found at " + blockPos));
                                return 0;
                            }
                        })));
    }

    private static String getProperty(String property) {
        switch (property.toLowerCase()) {
            case "maxscale": return "maxScale: " + BlackHoleConfig.getMaxScale();
            case "gravity": return "gravity: " + BlackHoleConfig.getGravity();
            case "playermass": return "playerMass: " + BlackHoleConfig.getPlayerMass();
            case "blockmass": return "blockMass: " + BlackHoleConfig.getBlockMass();
            case "itementitymass": return "itemEntityMass: " + BlackHoleConfig.getItemEntityMass();
            case "animalmass": return "animalMass: " + BlackHoleConfig.getAnimalMass();
            case "chunkloadradius": return "chunkLoadRadius: " + BlackHoleConfig.getChunkLoadRadius();
            case "maxblockspertick": return "maxBlocksPerTick: " + BlackHoleConfig.getMaxBlocksPerTick();
            case "movementspeed": return "movementSpeed: " + BlackHoleConfig.getMovementSpeed();
            case "defaultfollowrange": return "defaultFollowRange: " + BlackHoleConfig.getDefaultFollowRange();
            case "playerdetectioninterval": return "playerDetectionInterval: " + BlackHoleConfig.getPlayerDetectionInterval();
            case "growthrate": return "growthRate: " + BlackHoleConfig.getGrowthRate();
            default: return "Invalid property!";
        }
    }

    private static String setProperty(String property, double value) {
        switch (property.toLowerCase()) {
            case "maxscale": BlackHoleConfig.setMaxScale((float) value); return "maxScale set to " + value;
            case "gravity": BlackHoleConfig.setGravity(value); return "gravity set to " + value;
            case "playermass": BlackHoleConfig.setPlayerMass(value); return "playerMass set to " + value;
            case "blockmass": BlackHoleConfig.setBlockMass(value); return "blockMass set to " + value;
            case "itementitymass": BlackHoleConfig.setItemEntityMass(value); return "itemEntityMass set to " + value;
            case "animalmass": BlackHoleConfig.setAnimalMass(value); return "animalMass set to " + value;
            case "chunkloadradius": BlackHoleConfig.setChunkLoadRadius((int) value); return "chunkLoadRadius set to " + (int) value;
            case "maxblockspertick": BlackHoleConfig.setMaxBlocksPerTick((int) value); return "maxBlocksPerTick set to " + (int) value;
            case "movementspeed": BlackHoleConfig.setMovementSpeed(value); return "movementSpeed set to " + value;
            case "defaultfollowrange": BlackHoleConfig.setDefaultFollowRange(value); return "defaultFollowRange set to " + value;
            case "playerdetectioninterval": BlackHoleConfig.setPlayerDetectionInterval((int) value); return "playerDetectionInterval set to " + (int) value;
            case "growthrate": BlackHoleConfig.setGrowthRate((float) value); return "growthRate set to " + value;
            default: return "Invalid property!";
        }
    }
}