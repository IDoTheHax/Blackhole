package net.idothehax.blackhole.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.idothehax.blackhole.BlackHole;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BlackHoleConfig {
    // Configuration fields with default values
    private static float MAX_SCALE = 40.0f;
    private static double GRAVITY = 60.0;
    private static double PLAYER_MASS = 700.0;
    private static double BLOCK_MASS = 10.0;
    private static double ITEM_ENTITY_MASS = 0.1;
    private static double ANIMAL_MASS = 50.0;
    private static int CHUNK_LOAD_RADIUS = 2;
    private static int MAX_BLOCKS_PER_TICK = 500;
    private static double MOVEMENT_SPEED = 1.0;
    private static double DEFAULT_FOLLOW_RANGE = 256.0;
    private static int PLAYER_DETECTION_INTERVAL = 60;
    private static float GROWTH_RATE = 0.04f;
    private static boolean PARTICLES_ENABLED = true;

    private static final String CONFIG_FILE = "config/black_hole.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                if (json.has("maxScale")) MAX_SCALE = json.get("maxScale").getAsFloat();
                if (json.has("gravity")) GRAVITY = json.get("gravity").getAsDouble();
                if (json.has("playerMass")) PLAYER_MASS = json.get("playerMass").getAsDouble();
                if (json.has("blockMass")) BLOCK_MASS = json.get("blockMass").getAsDouble();
                if (json.has("itemEntityMass")) ITEM_ENTITY_MASS = json.get("itemEntityMass").getAsDouble();
                if (json.has("animalMass")) ANIMAL_MASS = json.get("animalMass").getAsDouble();
                if (json.has("chunkLoadRadius")) CHUNK_LOAD_RADIUS = json.get("chunkLoadRadius").getAsInt();
                if (json.has("maxBlocksPerTick")) MAX_BLOCKS_PER_TICK = json.get("maxBlocksPerTick").getAsInt();
                if (json.has("movementSpeed")) MOVEMENT_SPEED = json.get("movementSpeed").getAsDouble();
                if (json.has("defaultFollowRange")) DEFAULT_FOLLOW_RANGE = json.get("defaultFollowRange").getAsDouble();
                if (json.has("playerDetectionInterval"))
                    PLAYER_DETECTION_INTERVAL = json.get("playerDetectionInterval").getAsInt();
                if (json.has("growthRate")) GROWTH_RATE = json.get("growthRate").getAsFloat();
                if (json.has("particlesEnabled")) PARTICLES_ENABLED = json.get("particlesEnabled").getAsBoolean();

                BlackHole.LOGGER.info("Loaded black hole config from " + CONFIG_FILE);
            } catch (IOException e) {
                BlackHole.LOGGER.error("Failed to load black hole config: " + e.getMessage());
            }
        } else {
            BlackHole.LOGGER.info("No black hole config found, creating default config at " + CONFIG_FILE);
            saveConfig();
        }
    }

    public static void saveConfig() {
        Path configDir = Paths.get("config");
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                BlackHole.LOGGER.error("Failed to create config directory: " + e.getMessage());
                return;
            }
        }

        JsonObject json = new JsonObject();
        json.addProperty("maxScale", MAX_SCALE);
        json.addProperty("gravity", GRAVITY);
        json.addProperty("playerMass", PLAYER_MASS);
        json.addProperty("blockMass", BLOCK_MASS);
        json.addProperty("itemEntityMass", ITEM_ENTITY_MASS);
        json.addProperty("animalMass", ANIMAL_MASS);
        json.addProperty("chunkLoadRadius", CHUNK_LOAD_RADIUS);
        json.addProperty("maxBlocksPerTick", MAX_BLOCKS_PER_TICK);
        json.addProperty("movementSpeed", MOVEMENT_SPEED);
        json.addProperty("defaultFollowRange", DEFAULT_FOLLOW_RANGE);
        json.addProperty("playerDetectionInterval", PLAYER_DETECTION_INTERVAL);
        json.addProperty("growthRate", GROWTH_RATE);
        json.addProperty("particlesEnabled", PARTICLES_ENABLED);

        try (Writer writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
            GSON.toJson(json, writer);
            BlackHole.LOGGER.info("Saved black hole config to " + CONFIG_FILE);
        } catch (IOException e) {
            BlackHole.LOGGER.error("Failed to save black hole config: " + e.getMessage());
        }
    }

    // Getters for all config values
    public static float getMaxScale() {
        return MAX_SCALE;
    }

    public static double getGravity() {
        return GRAVITY;
    }

    public static double getPlayerMass() {
        return PLAYER_MASS;
    }

    public static double getBlockMass() {
        return BLOCK_MASS;
    }

    public static double getItemEntityMass() {
        return ITEM_ENTITY_MASS;
    }

    public static double getAnimalMass() {
        return ANIMAL_MASS;
    }

    public static int getChunkLoadRadius() {
        return CHUNK_LOAD_RADIUS;
    }

    public static int getMaxBlocksPerTick() {
        return MAX_BLOCKS_PER_TICK;
    }

    public static double getMovementSpeed() {
        return MOVEMENT_SPEED;
    }

    public static double getDefaultFollowRange() {
        return DEFAULT_FOLLOW_RANGE;
    }

    public static int getPlayerDetectionInterval() {
        return PLAYER_DETECTION_INTERVAL;
    }

    public static float getGrowthRate() {
        return GROWTH_RATE;
    }

    public static boolean areParticlesEnabled() {
        return PARTICLES_ENABLED;
    }

    // Setters for all config values
    public static void setMaxScale(float maxScale) {
        MAX_SCALE = maxScale;
        saveConfig();
    }

    public static void setGravity(double gravity) {
        GRAVITY = gravity;
        saveConfig();
    }

    public static void setPlayerMass(double playerMass) {
        PLAYER_MASS = playerMass;
        saveConfig();
    }

    public static void setBlockMass(double blockMass) {
        BLOCK_MASS = blockMass;
        saveConfig();
    }

    public static void setItemEntityMass(double itemEntityMass) {
        ITEM_ENTITY_MASS = itemEntityMass;
        saveConfig();
    }

    public static void setAnimalMass(double animalMass) {
        ANIMAL_MASS = animalMass;
        saveConfig();
    }

    public static void setChunkLoadRadius(int chunkLoadRadius) {
        CHUNK_LOAD_RADIUS = chunkLoadRadius;
        saveConfig();
    }

    public static void setMaxBlocksPerTick(int maxBlocksPerTick) {
        MAX_BLOCKS_PER_TICK = maxBlocksPerTick;
        saveConfig();
    }

    public static void setMovementSpeed(double movementSpeed) {
        MOVEMENT_SPEED = movementSpeed;
        saveConfig();
    }

    public static void setDefaultFollowRange(double defaultFollowRange) {
        DEFAULT_FOLLOW_RANGE = defaultFollowRange;
        saveConfig();
    }

    public static void setPlayerDetectionInterval(int playerDetectionInterval) {
        PLAYER_DETECTION_INTERVAL = playerDetectionInterval;
        saveConfig();
    }

    public static void setGrowthRate(float growthRate) {
        GROWTH_RATE = growthRate;
        saveConfig();
    }

    public static void setParticlesEnabled(boolean enabled) {
        PARTICLES_ENABLED = enabled;
        saveConfig();
    }

    public static void toggleParticles() {
        PARTICLES_ENABLED = !PARTICLES_ENABLED;
        saveConfig();
    }
}