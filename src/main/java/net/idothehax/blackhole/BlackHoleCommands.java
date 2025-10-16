package net.idothehax.blackhole;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.idothehax.blackhole.config.BlackHoleConfig;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class BlackHoleCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("blackhole")
                .requires(source -> source.hasPermissionLevel(2)) // Only ops can use
                // Max Scale commands
                .then(CommandManager.literal("getmaxscale")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("maxScale: " + BlackHoleConfig.getMaxScale()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setmaxscale")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setMaxScale((float) value);
                                    source.sendFeedback(() -> Text.literal("maxScale set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Gravity commands
                .then(CommandManager.literal("getgravity")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("gravity: " + BlackHoleConfig.getGravity()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setgravity")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setGravity(value);
                                    source.sendFeedback(() -> Text.literal("gravity set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Player Mass commands
                .then(CommandManager.literal("getplayermass")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("playerMass: " + BlackHoleConfig.getPlayerMass()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setplayermass")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setPlayerMass(value);
                                    source.sendFeedback(() -> Text.literal("playerMass set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Block Mass commands
                .then(CommandManager.literal("getblockmass")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("blockMass: " + BlackHoleConfig.getBlockMass()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setblockmass")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setBlockMass(value);
                                    source.sendFeedback(() -> Text.literal("blockMass set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Item Entity Mass commands
                .then(CommandManager.literal("getitementitymass")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("itemEntityMass: " + BlackHoleConfig.getItemEntityMass()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setitementitymass")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setItemEntityMass(value);
                                    source.sendFeedback(() -> Text.literal("itemEntityMass set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Animal Mass commands
                .then(CommandManager.literal("getanimalmass")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("animalMass: " + BlackHoleConfig.getAnimalMass()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setanimalmass")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setAnimalMass(value);
                                    source.sendFeedback(() -> Text.literal("animalMass set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Chunk Load Radius commands
                .then(CommandManager.literal("getchunkloadradius")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("chunkLoadRadius: " + BlackHoleConfig.getChunkLoadRadius()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setchunkloadradius")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setChunkLoadRadius((int) value);
                                    source.sendFeedback(() -> Text.literal("chunkLoadRadius set to " + (int) value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Max Blocks Per Tick commands
                .then(CommandManager.literal("getmaxblockspertick")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("maxBlocksPerTick: " + BlackHoleConfig.getMaxBlocksPerTick()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setmaxblockspertick")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setMaxBlocksPerTick((int) value);
                                    source.sendFeedback(() -> Text.literal("maxBlocksPerTick set to " + (int) value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Movement Speed commands
                .then(CommandManager.literal("getmovementspeed")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("movementSpeed: " + BlackHoleConfig.getMovementSpeed()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setmovementspeed")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setMovementSpeed(value);
                                    source.sendFeedback(() -> Text.literal("movementSpeed set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Default Follow Range commands
                .then(CommandManager.literal("getdefaultfollowrange")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("defaultFollowRange: " + BlackHoleConfig.getDefaultFollowRange()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setdefaultfollowrange")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setDefaultFollowRange(value);
                                    source.sendFeedback(() -> Text.literal("defaultFollowRange set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Player Detection Interval commands
                .then(CommandManager.literal("getplayerdetectioninterval")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("playerDetectionInterval: " + BlackHoleConfig.getPlayerDetectionInterval()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setplayerdetectioninterval")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setPlayerDetectionInterval((int) value);
                                    source.sendFeedback(() -> Text.literal("playerDetectionInterval set to " + (int) value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Growth Rate commands
                .then(CommandManager.literal("getgrowthrate")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("growthRate: " + BlackHoleConfig.getGrowthRate()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("setgrowthrate")
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    ServerCommandSource source = context.getSource();
                                    BlackHoleConfig.setGrowthRate((float) value);
                                    source.sendFeedback(() -> Text.literal("growthRate set to " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Particles commands
                .then(CommandManager.literal("toggleparticles")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            BlackHoleConfig.toggleParticles();
                            boolean enabled = BlackHoleConfig.areParticlesEnabled();
                            source.sendFeedback(() -> Text.literal("Particles " + (enabled ? "enabled" : "disabled")), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("getparticles")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            boolean enabled = BlackHoleConfig.areParticlesEnabled();
                            source.sendFeedback(() -> Text.literal("Particles are currently " + (enabled ? "enabled" : "disabled")), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                );
    }

    private static BlackHoleBlockEntity findNearestBlackHole(ServerWorld world, Vec3d position) {
        List<BlackHoleBlockEntity> blackHoles = new ArrayList<>();

        // Get the chunk manager
        var chunkManager = world.getChunkManager();

        // Get the player's chunk position as a starting point
        ChunkPos playerChunkPos = new ChunkPos(new BlockPos((int)position.x, (int)position.y, (int)position.z));

        // Search radius in chunks (adjust as needed, this is a reasonable default)
        int searchRadius = 32; // 32 chunks radius = 512 blocks

        // Iterate through nearby chunks
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkPos.x + dx, playerChunkPos.z + dz);

                // Check if chunk is loaded
                if (chunkManager.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                    Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
                    if (chunk instanceof WorldChunk worldChunk) {  // Cast to WorldChunk
                        // Get all block entities in this chunk
                        for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                            if (blockEntity instanceof BlackHoleBlockEntity blackHole) {
                                blackHoles.add(blackHole);
                            }
                        }
                    }
                }
            }
        }

        BlackHoleBlockEntity nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (BlackHoleBlockEntity blackHole : blackHoles) {
            double distanceSq = position.squaredDistanceTo(Vec3d.ofCenter(blackHole.getPosition()));
            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                nearest = blackHole;
            }
        }

        return nearest;
    }
}

