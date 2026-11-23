package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.RuinedPortal;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mcfeature.structure.Village;
import com.seedfinding.mcseed.rand.JRand;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates the nearest villages, strongholds and ruined portals for a known world seed.
 * The calculation runs on a background thread, the result is kept in memory.
 */
public class StructureLocator {

    public record Found(String type, int x, int z, int distance) {
        public String format() {
            return type + ": X " + x + ", Z " + z + " (" + distance + ")";
        }
    }

    private static final int REGION_RADIUS = 5;
    private static final int PER_TYPE = 3;

    private static volatile List<Found> lastResult = List.of();
    private static volatile long lastSeed = 0L;
    private static volatile boolean hasResult = false;
    private static volatile boolean running = false;
    private static volatile String error = null;

    public static List<Found> getResult() {
        return lastResult;
    }

    public static boolean hasResult() {
        return hasResult;
    }

    public static boolean isRunning() {
        return running;
    }

    public static String getError() {
        return error;
    }

    public static long getSeed() {
        return lastSeed;
    }

    /**
     * Starts the calculation in the background. Does nothing if it already runs.
     *
     * @param onDone called on the background thread when the calculation is finished
     */
    public static synchronized void compute(long worldSeed, int playerX, int playerZ, Runnable onDone) {
        if (running) return;
        running = true;
        error = null;

        Thread thread = new Thread(() -> {
            try {
                lastResult = calculate(worldSeed, playerX, playerZ);
                lastSeed = worldSeed;
                hasResult = true;
            } catch (Throwable t) {
                SeedCracker.LOGGER.warn("Could not calculate the structures", t);
                error = String.valueOf(t);
            } finally {
                running = false;
            }
            if (onDone != null) {
                try {
                    onDone.run();
                } catch (Throwable t) {
                    SeedCracker.LOGGER.warn("Structure callback failed", t);
                }
            }
        }, "SeedCrackerX-StructureLocator");
        thread.setDaemon(true);
        thread.start();
    }

    private static List<Found> calculate(long worldSeed, int playerX, int playerZ) {
        MCVersion version = Config.get().getVersion();
        BiomeSource source = BiomeSource.of(Dimension.OVERWORLD, version, worldSeed);
        List<Found> result = new ArrayList<>();

        result.addAll(findRegionStructures("Деревня", new Village(version), worldSeed, source, playerX, playerZ));
        result.addAll(findRegionStructures("Руины портала", new RuinedPortal(Dimension.OVERWORLD, version), worldSeed, source, playerX, playerZ));
        result.addAll(findStrongholds(version, source, playerX, playerZ));

        return result;
    }

    private static List<Found> findRegionStructures(String name, RegionStructure<?, ?> structure, long worldSeed,
                                                    BiomeSource source, int playerX, int playerZ) {
        int regionBlocks = structure.getSpacing() * 16;
        int centerRegionX = Math.floorDiv(playerX, regionBlocks);
        int centerRegionZ = Math.floorDiv(playerZ, regionBlocks);
        ChunkRand rand = new ChunkRand();
        List<Found> found = new ArrayList<>();

        for (int rx = centerRegionX - REGION_RADIUS; rx <= centerRegionX + REGION_RADIUS; rx++) {
            for (int rz = centerRegionZ - REGION_RADIUS; rz <= centerRegionZ + REGION_RADIUS; rz++) {
                CPos pos = structure.getInRegion(worldSeed, rx, rz, rand);
                if (pos == null) continue;
                try {
                    if (!structure.canSpawn(pos.getX(), pos.getZ(), source)) continue;
                } catch (Throwable t) {
                    continue;
                }
                int bx = pos.getX() * 16 + 8;
                int bz = pos.getZ() * 16 + 8;
                found.add(new Found(name, bx, bz, distance(bx, bz, playerX, playerZ)));
            }
        }

        found.sort(Comparator.comparingInt(Found::distance));
        return found.size() > PER_TYPE ? new ArrayList<>(found.subList(0, PER_TYPE)) : found;
    }

    private static List<Found> findStrongholds(MCVersion version, BiomeSource source, int playerX, int playerZ) {
        List<Found> found = new ArrayList<>();
        Stronghold stronghold = new Stronghold(version);
        CPos[] starts = stronghold.getStarts(source, PER_TYPE, new JRand(0L));
        for (CPos pos : starts) {
            if (pos == null) continue;
            int bx = pos.getX() * 16 + 8;
            int bz = pos.getZ() * 16 + 8;
            found.add(new Found("Крепость", bx, bz, distance(bx, bz, playerX, playerZ)));
        }
        found.sort(Comparator.comparingInt(Found::distance));
        return found;
    }

    private static int distance(int x1, int z1, int x2, int z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * The whole list as plain text, one structure per line.
     */
    public static String asText() {
        StringBuilder sb = new StringBuilder();
        for (Found f : lastResult) {
            sb.append(f.format()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
