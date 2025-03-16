package net.idothehax.blackhole;

import com.mojang.brigadier.context.CommandContext;
import net.idothehax.blackhole.config.BlackHoleConfig;
import net.idothehax.blackhole.mixin.DisplayEntityAccessor;
import net.idothehax.blackhole.mixin.ItemDisplayEntityInvoker;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class BlackHoleBlockEntity extends BlockEntity {
    public float scale = 1.0f;
    public boolean isGrowing = true;
    @Nullable private DisplayEntity.ItemDisplayEntity itemDisplayEntity;
    private ChunkPos chunkPos;
    private boolean chunksLoaded = false;
    private int recreateAttempts = 0;
    private static final int MAX_RECREATE_ATTEMPTS = 3;
    private boolean isMarkedForRemoval = false;
    private int tickCounter = 0;
    private double currentBreakRadius = 0.0;
    private boolean shouldFollow = true; // Instance-specific
    private double followRange = BlackHoleConfig.getDefaultFollowRange();

    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlackHole.BLACK_HOLE_BLOCK_ENTITY, pos, state);
        this.chunkPos = new ChunkPos(pos);
        this.shouldFollow = true; // Default to following
    }

    public void startGrowth() {
        if (isMarkedForRemoval) {
            BlackHole.LOGGER.warn("Attempted to start growth on marked-for-removal block entity at " + this.pos);
            return;
        }

        if (this.world != null && !this.world.getBlockState(this.pos).isOf(BlackHole.BLACK_HOLE_BLOCK)) {
            BlackHole.LOGGER.warn("Cannot start growth at " + this.pos + " - block is not a black hole block");
            return;
        }

        this.isGrowing = true;
        if (this.world instanceof ServerWorld serverWorld) {
            if (this.itemDisplayEntity != null && !this.itemDisplayEntity.isRemoved()) {
                BlackHole.LOGGER.info("Discarding existing display entity at " + this.itemDisplayEntity.getPos());
                this.itemDisplayEntity.discard();
            }
            this.itemDisplayEntity = null;

            DisplayEntity.ItemDisplayEntity itemDisplay = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, serverWorld);
            Vec3d positionOfHole = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
            itemDisplay.refreshPositionAndAngles(positionOfHole.x, positionOfHole.y, positionOfHole.z, 0, 0);
            itemDisplay.setNoGravity(true);
            ((ItemDisplayEntityInvoker) itemDisplay).invokeSetItemStack(new ItemStack(BlackHole.BLACK_HOLE_ITEM));
            ((ItemDisplayEntityInvoker) itemDisplay).invokeSetTransformationMode(ModelTransformationMode.GROUND);
            ((DisplayEntityAccessor) itemDisplay).invokeSetBillboardMode(DisplayEntity.BillboardMode.CENTER);

            setScale(itemDisplay, new Vector3f(this.scale), positionOfHole);

            ((DisplayEntityAccessor) itemDisplay).invokeSetInterpolationDuration(20);
            ((DisplayEntityAccessor) itemDisplay).invokeSetStartInterpolation(0);

            NbtCompound nbt = new NbtCompound();
            itemDisplay.writeNbt(nbt);
            nbt.putBoolean("PersistenceRequired", true);
            itemDisplay.readNbt(nbt);

            boolean spawned = serverWorld.spawnEntity(itemDisplay);
            BlackHole.LOGGER.info("Black hole display entity spawned: " + spawned + " at " + positionOfHole + ", UUID: " + itemDisplay.getUuid());
            if (!spawned) {
                BlackHole.LOGGER.warn("Failed to spawn black hole display entity at " + positionOfHole);
                return;
            }

            this.itemDisplayEntity = itemDisplay;
            this.recreateAttempts = 0;

            serverWorld.scheduleBlockTick(this.pos, this.getCachedState().getBlock(), 1);
        }
    }

    @Override
    public void markRemoved() {
        if (isMarkedForRemoval) {
            BlackHole.LOGGER.debug("Already marked for removal at " + this.pos);
            return;
        }
        isMarkedForRemoval = true;
        BlackHole.LOGGER.info("Marking BlackHoleBlockEntity for removal at " + this.pos);

        if (this.world instanceof ServerWorld serverWorld) {
            try {
                ServerChunkManager chunkManager = serverWorld.getChunkManager();
                chunkManager.removeTicket(BlackHole.BLACK_HOLE_TICKET_TYPE, this.chunkPos, BlackHoleConfig.getChunkLoadRadius(), this.pos);
                for (int dx = -BlackHoleConfig.getChunkLoadRadius(); dx <= BlackHoleConfig.getChunkLoadRadius(); dx++) {
                    for (int dz = -BlackHoleConfig.getChunkLoadRadius(); dz <= BlackHoleConfig.getChunkLoadRadius(); dz++) {
                        ChunkPos chunkPos = new ChunkPos(this.chunkPos.x + dx, this.chunkPos.z + dz);
                        chunkManager.setChunkForced(chunkPos, false);
                    }
                }
                this.chunksLoaded = false;
                BlackHole.LOGGER.debug("Removed chunk tickets for " + this.chunkPos);
            } catch (Exception e) {
                BlackHole.LOGGER.error("Error removing chunk tickets: " + e.getMessage());
            }
        }

        if (this.itemDisplayEntity != null && !this.itemDisplayEntity.isRemoved()) {
            try {
                BlackHole.LOGGER.info("Discarding display entity during markRemoved");
                this.itemDisplayEntity.discard();
            } catch (Exception e) {
                BlackHole.LOGGER.error("Error discarding display entity: " + e.getMessage());
            }
        }
        this.itemDisplayEntity = null;
        super.markRemoved();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.scale = nbt.getFloat("scale");
        this.isGrowing = nbt.getBoolean("isGrowing");
        this.shouldFollow = nbt.getBoolean("shouldFollow"); // Instance-specific
        this.followRange = nbt.getDouble("followRange");

        if (nbt.containsUuid("entity") && this.world instanceof ServerWorld serverWorld) {
            UUID entityUuid = nbt.getUuid("entity");
            Entity entity = serverWorld.getEntity(entityUuid);
            if (entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay) {
                this.itemDisplayEntity = itemDisplay;
                BlackHole.LOGGER.info("Loaded display entity from NBT with UUID: " + entityUuid);
            } else {
                BlackHole.LOGGER.warn("Failed to load display entity from NBT. UUID: " + entityUuid);
                this.itemDisplayEntity = null;
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putFloat("scale", this.scale);
        nbt.putBoolean("isGrowing", this.isGrowing);
        nbt.putBoolean("shouldFollow", this.shouldFollow);
        nbt.putDouble("followRange", this.followRange);

        if (this.itemDisplayEntity != null && !this.itemDisplayEntity.isRemoved()) {
            nbt.putUuid("entity", this.itemDisplayEntity.getUuid());
            BlackHole.LOGGER.debug("Saved display entity UUID: " + this.itemDisplayEntity.getUuid());
        }
    }

    private void moveBlackHole(ServerWorld serverWorld, BlockPos newBlockPos) {
        ChunkPos newChunkPos = new ChunkPos(newBlockPos);
        try {
            if (!serverWorld.isChunkLoaded(newChunkPos.x, newChunkPos.z)) {
                serverWorld.getChunk(newChunkPos.x, newChunkPos.z, ChunkStatus.FULL);
            }
        } catch (Exception e) {
            BlackHole.LOGGER.error("Failed to load chunk at " + newChunkPos + ": " + e.getMessage());
            return;
        }

        BlockState currentState = serverWorld.getBlockState(this.pos);
        if (currentState.isOf(BlackHole.BLACK_HOLE_BLOCK)) {
            serverWorld.setBlockState(this.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            serverWorld.setBlockState(newBlockPos, BlackHole.BLACK_HOLE_BLOCK.getDefaultState(), Block.NOTIFY_ALL);

            if (serverWorld.getBlockEntity(newBlockPos) instanceof BlackHoleBlockEntity newEntity) {
                newEntity.scale = this.scale;
                newEntity.isGrowing = this.isGrowing;
                newEntity.shouldFollow = this.shouldFollow;
                newEntity.followRange = this.followRange;
                newEntity.chunkPos = newChunkPos;
                newEntity.chunksLoaded = false;
                newEntity.startGrowth();

                Vec3d fromPos = Vec3d.ofCenter(this.pos);
                Vec3d toPos = Vec3d.ofCenter(newBlockPos);
                Vec3d direction = toPos.subtract(fromPos).normalize();
                for (int i = 0; i < 10; i++) {
                    double progress = i / 10.0;
                    Vec3d particlePos = fromPos.add(direction.multiply(progress));
                    serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.01);
                }

                BlackHole.LOGGER.info("Black hole moved from " + this.pos + " to " + newBlockPos);
            }
        }
    }

    public void grow() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d positionOfHole = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);

        if (this.itemDisplayEntity != null && !this.itemDisplayEntity.isRemoved()) {
            if (this.scale < BlackHoleConfig.getMaxScale()) {
                this.scale += BlackHoleConfig.getGrowthRate();
            }
            ((DisplayEntityAccessor) this.itemDisplayEntity).invokeSetInterpolationDuration(20);
            ((DisplayEntityAccessor) this.itemDisplayEntity).invokeSetStartInterpolation(0);
            ((DisplayEntityAccessor) this.itemDisplayEntity).invokeSetDisplayWidth(this.scale);
            ((DisplayEntityAccessor) this.itemDisplayEntity).invokeSetDisplayHeight(this.scale);
            setScale(this.itemDisplayEntity, new Vector3f(this.scale), positionOfHole);

            this.itemDisplayEntity.setPosition(positionOfHole);

            serverWorld.scheduleBlockTick(this.pos, getCachedState().getBlock(), 1);
        } else {
            BlackHole.LOGGER.warn("No item display entity found or it was removed. Attempting to recreate it.");
            startGrowth();
        }
    }

    public boolean isBlackHoleGrowing() {
        return this.isGrowing;
    }

    public void toggleBlackHoleGrowth() {
        this.isGrowing = !this.isGrowing;
        if (this.isGrowing && this.world instanceof ServerWorld serverWorld) {
            serverWorld.scheduleBlockTick(this.pos, getCachedState().getBlock(), 1);
        }
    }

    public void toggleFollowing() {
        this.shouldFollow = !this.shouldFollow;
        BlackHole.LOGGER.info("Black hole at " + this.pos + " following players: " + this.shouldFollow);
    }

    public boolean isFollowing() {
        return this.shouldFollow;
    }

    public void setFollowing(boolean shouldFollow) {
        this.shouldFollow = shouldFollow;
        BlackHole.LOGGER.info("Black hole at " + this.pos + " following players set to: " + this.shouldFollow);
    }

    public BlockPos getPosition() {
        return this.pos;
    }

    public void tick(World world, BlockPos pos, BlockState blockState) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!blockState.isOf(BlackHole.BLACK_HOLE_BLOCK)) {
            BlackHole.LOGGER.warn("Black hole block entity exists but block is " + blockState + " at " + pos);
            return;
        }

        if (isMarkedForRemoval) {
            BlackHole.LOGGER.warn("Attempted to tick marked-for-removal block entity at " + this.pos);
            return;
        }

        if (!chunksLoaded || tickCounter % 100 == 0) {
            forceLoadChunks(serverWorld);
            chunksLoaded = true;
        }

        if (this.isGrowing) {
            this.grow();
        }

        if (this.itemDisplayEntity == null || this.itemDisplayEntity.isRemoved()) {
            if (recreateAttempts < MAX_RECREATE_ATTEMPTS) {
                BlackHole.LOGGER.warn("Black hole display entity is null or removed during tick at " + this.pos +
                        ". Attempt " + (recreateAttempts + 1) + " of " + MAX_RECREATE_ATTEMPTS + " to recreate.");
                recreateAttempts++;
                startGrowth();
                return;
            } else {
                BlackHole.LOGGER.error("Failed to recreate black hole display entity after " + MAX_RECREATE_ATTEMPTS +
                        " attempts at " + this.pos + ". Giving up.");
                this.isGrowing = false;
                return;
            }
        }

        recreateAttempts = 0;

        Vec3d expectedPos = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
        if (!this.itemDisplayEntity.getPos().equals(expectedPos)) {
            BlackHole.LOGGER.warn("Display entity position mismatch at " + this.pos +
                    ". Expected: " + expectedPos + ", Actual: " + this.itemDisplayEntity.getPos());
            this.itemDisplayEntity.setPosition(expectedPos);
        }

        double blackHoleMass = (4.0 / 3.0) * Math.PI * this.scale * this.scale * this.scale;
        double effectRadius = this.scale;
        Box areaOfEffect = new Box(this.pos).expand(effectRadius);

        tickCounter++;

        if (this.shouldFollow && tickCounter % BlackHoleConfig.getPlayerDetectionInterval() == 0) {
            BlackHole.LOGGER.debug("Checking for player to follow at " + this.pos + ", shouldFollow: " + this.shouldFollow);
            PlayerEntity closestPlayer = null;
            double closestDistance = followRange * followRange;

            for (PlayerEntity player : serverWorld.getPlayers()) {
                if (player.isCreative() || player.isSpectator()) {
                    BlackHole.LOGGER.debug("Skipping player " + player.getName().getString() + " (creative/spectator)");
                    continue;
                }

                double distance = player.squaredDistanceTo(Vec3d.ofCenter(this.pos));
                BlackHole.LOGGER.debug("Player " + player.getName().getString() + " at distance " + Math.sqrt(distance));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }

            if (closestPlayer != null) {
                BlackHole.LOGGER.debug("Closest player: " + closestPlayer.getName().getString() + " at " + closestPlayer.getPos());
                Vec3d currentPosition = Vec3d.ofCenter(this.pos);
                Vec3d playerPosition = closestPlayer.getPos();
                Vec3d movementDirection = playerPosition.subtract(currentPosition).normalize();
                Vec3d newPosition = currentPosition.add(movementDirection.multiply(BlackHoleConfig.getMovementSpeed()));
                BlockPos newBlockPos = BlockPos.ofFloored(newPosition);

                BlackHole.LOGGER.debug("Calculated new position: " + newPosition + ", new block pos: " + newBlockPos);
                if (!newBlockPos.equals(this.pos)) {
                    ChunkPos newChunkPos = new ChunkPos(newBlockPos);
                    try {
                        if (!serverWorld.isChunkLoaded(newChunkPos.x, newChunkPos.z)) {
                            BlackHole.LOGGER.debug("Loading chunk " + newChunkPos);
                            serverWorld.getChunk(newChunkPos.x, newChunkPos.z, ChunkStatus.FULL);
                        }
                        BlockState stateAtNewPos = serverWorld.getBlockState(newBlockPos);
                        BlackHole.LOGGER.debug("State at new pos: " + stateAtNewPos);
                        if (stateAtNewPos.isAir() || stateAtNewPos.isReplaceable()) {
                            BlackHole.LOGGER.info("Moving black hole to " + newBlockPos);
                            moveBlackHole(serverWorld, newBlockPos);
                            return;
                        } else {
                            if (stateAtNewPos.getHardness(serverWorld, newBlockPos) >= 0) {
                                serverWorld.setBlockState(newBlockPos, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS | Block.FORCE_STATE);
                                BlackHole.LOGGER.debug("Broke block at " + newBlockPos + " to allow movement");
                                moveBlackHole(serverWorld, newBlockPos);
                                return;
                            } else {
                                BlackHole.LOGGER.debug("Cannot move to " + newBlockPos + " - unbreakable block");
                            }
                        }
                    } catch (Exception e) {
                        BlackHole.LOGGER.error("Failed to move black hole to " + newChunkPos + ": " + e.getMessage());
                        return;
                    }
                } else {
                    BlackHole.LOGGER.debug("New position same as current: " + this.pos);
                }
            } else {
                BlackHole.LOGGER.debug("No eligible players found within range " + followRange);
            }
        }

        breakBlocksInRadius(serverWorld, areaOfEffect);

        List<Entity> affectedEntities = serverWorld.getEntitiesByClass(Entity.class, areaOfEffect, entity ->
                entity != this.itemDisplayEntity &&
                        entity.getPos().distanceTo(this.pos.toCenterPos()) < this.scale &&
                        !(entity instanceof PlayerEntity player && player.isCreative()));

        for (Entity entity : affectedEntities) {
            double distanceToBlackHole = entity.getPos().distanceTo(Vec3d.ofCenter(pos));

            if (distanceToBlackHole <= this.scale / 2 && !(entity instanceof PlayerEntity player && player.isCreative())) {
                if (entity instanceof ItemEntity) {
                    entity.discard();
                } else {
                    entity.damage(this.world.getDamageSources().outOfWorld(), Float.MAX_VALUE);
                }
            } else {
                double mass;
                if (entity instanceof PlayerEntity) {
                    mass = BlackHoleConfig.getPlayerMass();
                } else if (entity instanceof FallingBlockEntity) {
                    mass = BlackHoleConfig.getBlockMass();
                } else if (entity instanceof ItemEntity) {
                    mass = BlackHoleConfig.getItemEntityMass();
                } else if (!(entity instanceof LivingEntity)) {
                    continue;
                } else {
                    mass = BlackHoleConfig.getAnimalMass();
                }
                applyGravitationalPull(entity.getPos(), entity, blackHoleMass, mass, serverWorld);
            }
        }

        emitParticles(serverWorld);
    }

    private void breakBlocksInRadius(ServerWorld serverWorld, Box areaOfEffect) {
        Vec3d center = this.pos.toCenterPos();
        double maxBreakRadius = this.scale;
        double gravitationalRadius = this.scale * 2;

        double radiusIncrement = 0.5;
        if (tickCounter % 5 == 0) {
            currentBreakRadius = Math.min(currentBreakRadius + radiusIncrement, maxBreakRadius);
        }

        int minX = MathHelper.floor(center.x - gravitationalRadius);
        int maxX = MathHelper.ceil(center.x + gravitationalRadius);
        int minY = MathHelper.floor(center.y - gravitationalRadius);
        int maxY = MathHelper.ceil(center.y + gravitationalRadius);
        int minZ = MathHelper.floor(center.z - gravitationalRadius);
        int maxZ = MathHelper.ceil(center.z + gravitationalRadius);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        Random random = serverWorld.getRandom();
        int blocksProcessed = 0;

        for (int i = 0; i < BlackHoleConfig.getMaxBlocksPerTick() && blocksProcessed < BlackHoleConfig.getMaxBlocksPerTick(); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double theta = random.nextDouble() * Math.PI;
            double r = random.nextDouble() * gravitationalRadius;

            int x = MathHelper.floor(center.x + r * Math.sin(theta) * Math.cos(angle));
            int y = MathHelper.floor(center.y + r * Math.cos(theta));
            int z = MathHelper.floor(center.z + r * Math.sin(theta) * Math.sin(angle));

            if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
                continue;
            }

            mutablePos.set(x, y, z);
            if (mutablePos.equals(this.pos)) {
                continue;
            }

            double distanceToCenter = center.distanceTo(Vec3d.ofCenter(mutablePos));
            BlockState blockState = serverWorld.getBlockState(mutablePos);

            if (blockState.isAir()) {
                continue;
            }

            if (distanceToCenter <= currentBreakRadius) {
                if (blockState.getBlock() instanceof FluidBlock || blockState.getFluidState().isOf(Fluids.WATER) || blockState.getFluidState().isOf(Fluids.LAVA)) {
                    serverWorld.setBlockState(mutablePos, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS | Block.FORCE_STATE);
                    serverWorld.spawnParticles(ParticleTypes.SPLASH, mutablePos.getX() + 0.5, mutablePos.getY() + 0.5, mutablePos.getZ() + 0.5,
                            5, 0.2, 0.2, 0.2, 0.01);
                    blocksProcessed++;
                } else if (blockState.getHardness(serverWorld, mutablePos) >= 0) {
                    breakBlock(serverWorld, mutablePos, blockState);
                    blocksProcessed++;
                }
            } else if (distanceToCenter <= gravitationalRadius) {
                if (blockState.getBlock() instanceof FluidBlock || blockState.getFluidState().isOf(Fluids.WATER)) {
                    double probability = 0.05 * (gravitationalRadius - distanceToCenter) / gravitationalRadius;
                    if (random.nextDouble() < probability) {
                        serverWorld.removeBlock(mutablePos, false);
                        serverWorld.spawnParticles(ParticleTypes.SPLASH, mutablePos.getX() + 0.5, mutablePos.getY() + 0.5, mutablePos.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0.01);
                        blocksProcessed++;
                    }
                } else if (blockState.getHardness(serverWorld, mutablePos) >= 0) {
                    double probability = 0.05 * (gravitationalRadius - distanceToCenter) / gravitationalRadius;
                    if (random.nextDouble() < probability) {
                        createFallingBlock(serverWorld, mutablePos, blockState);
                        blocksProcessed++;
                    }
                }
            }
        }

        if (blocksProcessed >= BlackHoleConfig.getMaxBlocksPerTick()) {
            BlackHole.LOGGER.debug("Reached block processing limit of " + BlackHoleConfig.getMaxBlocksPerTick() + " at " + this.pos);
        }
    }

    private void breakBlock(ServerWorld serverWorld, BlockPos pos, BlockState blockState) {
        if (shouldCreateFallingBlock(blockState) && serverWorld.random.nextFloat() < 0.3f) {
            createFallingBlock(serverWorld, pos, blockState);
        } else {
            serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS | Block.FORCE_STATE);
            serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private boolean shouldCreateFallingBlock(BlockState blockState) {
        Block block = blockState.getBlock();
        return block != Blocks.BEDROCK &&
                block != Blocks.AIR &&
                !(block instanceof BlockEntityProvider) &&
                !(block instanceof FluidBlock) &&
                blockState.getHardness(world, pos) >= 0 &&
                blockState.getHardness(world, pos) < 50;
    }

    private void createFallingBlock(ServerWorld serverWorld, BlockPos pos, BlockState blockState) {
        if (!blockState.isAir() && blockState.getHardness(serverWorld, pos) >= 0) {
            FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(serverWorld, pos, blockState);
            if (fallingBlock != null) {
                Vec3d direction = this.pos.toCenterPos().subtract(pos.toCenterPos()).normalize();
                fallingBlock.setVelocity(direction.multiply(0.1));
                fallingBlock.velocityModified = true;
                serverWorld.removeBlock(pos, false);
            }
        }
    }

    private void forceLoadChunks(ServerWorld serverWorld) {
        ServerChunkManager chunkManager = serverWorld.getChunkManager();
        ChunkPos currentChunkPos = new ChunkPos(this.pos);

        if (!currentChunkPos.equals(this.chunkPos)) {
            chunkManager.removeTicket(BlackHole.BLACK_HOLE_TICKET_TYPE, this.chunkPos, BlackHoleConfig.getChunkLoadRadius(), this.pos);
            for (int dx = -BlackHoleConfig.getChunkLoadRadius(); dx <= BlackHoleConfig.getChunkLoadRadius(); dx++) {
                for (int dz = -BlackHoleConfig.getChunkLoadRadius(); dz <= BlackHoleConfig.getChunkLoadRadius(); dz++) {
                    ChunkPos oldChunkPos = new ChunkPos(this.chunkPos.x + dx, this.chunkPos.z + dz);
                    chunkManager.setChunkForced(oldChunkPos, false);
                }
            }
            this.chunkPos = currentChunkPos;
            BlackHole.LOGGER.debug("Removed chunk tickets for old position: " + this.chunkPos);
        }

        chunkManager.addTicket(BlackHole.BLACK_HOLE_TICKET_TYPE, this.chunkPos, BlackHoleConfig.getChunkLoadRadius(), this.pos);
        for (int dx = -BlackHoleConfig.getChunkLoadRadius(); dx <= BlackHoleConfig.getChunkLoadRadius(); dx++) {
            for (int dz = -BlackHoleConfig.getChunkLoadRadius(); dz <= BlackHoleConfig.getChunkLoadRadius(); dz++) {
                ChunkPos loadChunkPos = new ChunkPos(this.chunkPos.x + dx, this.chunkPos.z + dz);
                try {
                    serverWorld.getChunk(loadChunkPos.x, loadChunkPos.z, ChunkStatus.FULL);
                    chunkManager.setChunkForced(loadChunkPos, true);
                } catch (Exception e) {
                    BlackHole.LOGGER.error("Failed to force load chunk " + loadChunkPos + ": " + e.getMessage());
                    this.chunksLoaded = false;
                    return;
                }
            }
        }
        this.chunksLoaded = true;
        BlackHole.LOGGER.debug("Forced loaded chunks around " + this.chunkPos);
    }

    private void emitParticles(ServerWorld serverWorld) {
        double particleRadius = this.scale * 1.2;
        int particleCount = Math.min(200, (int)(this.scale * 30));

        if (particleRadius <= 0) {
            return;
        }

        Vec3d center = this.pos.toCenterPos();
        Random random = serverWorld.getRandom();

        for (int i = 0; i < particleCount; i++) {
            double angle = i * Math.PI * 2 / particleCount;
            double spiralRadius = particleRadius * (0.5 + 0.5 * random.nextDouble());
            double heightVariation = (random.nextDouble() - 0.5) * particleRadius;

            double xPos = center.getX() + spiralRadius * Math.cos(angle);
            double yPos = center.getY() + heightVariation;
            double zPos = center.getZ() + spiralRadius * Math.sin(angle);

            if (random.nextFloat() < 0.7) {
                serverWorld.spawnParticles(ParticleTypes.FLAME, xPos, yPos, zPos, 1, 0, 0, 0, 0);
            } else if (random.nextFloat() < 0.5) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, xPos, yPos, zPos, 1, 0, 0, 0, 0);
            } else {
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, xPos, yPos, zPos, 1, 0, 0, 0, 0);
            }
        }
    }

    private void applyGravitationalPull(Vec3d entityPos, Entity entity, double blackHoleMass, double entityMass, ServerWorld serverWorld) {
        Vec3d blackHolePos = this.pos.toCenterPos();
        double distanceToBlackHole = entityPos.distanceTo(blackHolePos);

        if (distanceToBlackHole > 0.1) {
            Vec3d directionFromEntityToHole = blackHolePos.subtract(entityPos).normalize();
            double forceMagnitude = BlackHoleConfig.getGravity() * entityMass * blackHoleMass / (distanceToBlackHole * distanceToBlackHole);
            forceMagnitude = Math.min(forceMagnitude, 5.0);
            Vec3d velocityToAdd = directionFromEntityToHole.multiply(forceMagnitude / 1000);
            entity.addVelocity(velocityToAdd.x, velocityToAdd.y, velocityToAdd.z);
            entity.velocityModified = true;
        }
    }

    private void setScale(DisplayEntity.ItemDisplayEntity itemDisplayEntity, Vector3f scale, Vec3d pos) {
        itemDisplayEntity.getDataTracker().set(DisplayEntityAccessor.getScale(), scale);
        itemDisplayEntity.setPosition(pos);
    }

    public void setScale(float size) {
        this.scale = size;
        if (this.world instanceof ServerWorld serverWorld && this.itemDisplayEntity != null) {
            Vec3d positionOfHole = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
            setScale(this.itemDisplayEntity, new Vector3f(this.scale), positionOfHole);
        }
    }
}