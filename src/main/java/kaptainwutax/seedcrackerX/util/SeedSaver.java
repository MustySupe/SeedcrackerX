package kaptainwutax.seedcrackerX.util;

import kaptainwutax.seedcrackerX.SeedCracker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * Saves every found seed to config/seedcrackerx/found_seeds.txt
 * and plays a sound so the player notices the result.
 */
public class SeedSaver {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALREADY_SAVED = new HashSet<>();
    private static long lastSoundTime = 0;

    /**
     * Called for every seed that gets printed to the chat.
     *
     * @return true if the seed was saved to the file, false if it was ignored or the save failed
     */
    public static synchronized boolean onSeed(String translateKey, long seed) {
        String kind = kindOf(translateKey);
        if (kind == null) return false;

        // The same seed is often reported several times, only handle it once
        if (!ALREADY_SAVED.add(kind + ":" + seed)) return false;

        playSound();

        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("seedcrackerx");
            Files.createDirectories(dir);
            Path file = dir.resolve("found_seeds.txt");
            String line = LocalDateTime.now().format(TIME) + "\t" + kind + "\t" + seed + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            SeedCracker.LOGGER.warn("Could not save the seed to a file", e);
            return false;
        }
    }

    public static Path getFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("seedcrackerx").resolve("found_seeds.txt");
    }

    private static String kindOf(String translateKey) {
        if (translateKey.contains("WorldSeed")) return "world";
        if (translateKey.contains("StructureSeed") || translateKey.equals("crossCompare")) return "structure";
        return null;
    }

    private static void playSound() {
        long now = System.currentTimeMillis();
        if (now - lastSoundTime < 3000) return;
        lastSoundTime = now;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F)));
    }
}
