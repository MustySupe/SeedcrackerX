package kaptainwutax.seedcrackerX.ui;

import com.seedfinding.mcfeature.Feature;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.cracker.storage.TimeMachine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the "what to look for next" text from the data that is collected so far.
 */
public class Hints {

    /** Simple class name of the feature -> name shown to the player. */
    private static final Map<String, String> USEFUL = new LinkedHashMap<>();

    static {
        USEFUL.put("DesertPyramid", "пустынный храм");
        USEFUL.put("JunglePyramid", "джунглевый храм");
        USEFUL.put("SwampHut", "хижина ведьмы");
        USEFUL.put("Igloo", "иглу");
        USEFUL.put("Monument", "подводный монумент");
        USEFUL.put("Shipwreck", "кораблекрушение");
        USEFUL.put("Dungeon", "комната со спаунером в пещере");
    }

    public static List<String> build(DataStorage storage) {
        List<String> lines = new ArrayList<>();
        TimeMachine tm = storage.getTimeMachine();
        int worldSeeds = tm.worldSeeds.size();
        int structureSeeds = tm.structureSeeds.size();

        Set<String> foundTypes = new TreeSet<>();
        int entries = 0;
        for (DataStorage.Entry<Feature.Data<?>> e : storage.baseSeedData) {
            entries++;
            foundTypes.add(e.data.feature.getClass().getSimpleName());
        }

        if (worldSeeds == 1) {
            lines.add("Готово: мировой сид найден. Нажмите «Копировать сид».");
            return lines;
        }

        if (worldSeeds > 1) {
            lines.add("Найдено несколько мировых сидов (" + worldSeeds + "). Найдите комнату со спаунером или пройдите по новым биомам, чтобы оставить один.");
            return lines;
        }

        if (structureSeeds > 0) {
            lines.add("Структурный сид найден, осталось найти мировой.");
            lines.add("Ищите комнаты со спаунерами в пещерах. Если их нет, включите «Биомы» в настройках и пройдите по 7 и более разным биомам.");
            return lines;
        }

        if (entries == 0) {
            lines.add("Данных пока нет. Ходите по миру и подходите близко к постройкам.");
            lines.add("Лучше всего: " + listNames(new TreeSet<>(), 5) + ".");
            return lines;
        }

        double lifting = storage.getLiftingBits();
        double missing = Math.max(0, 40 - lifting);
        if (missing > 0) {
            lines.add("Не хватает примерно " + (int) Math.ceil(missing) + " бит из 40.");
        } else {
            lines.add("Данных достаточно, идёт расчёт. Подождите немного.");
        }

        String next = listNames(foundTypes, 4);
        if (!next.isEmpty() && missing > 0) {
            lines.add("Ищите ещё: " + next + ".");
        }
        return lines;
    }

    /** Names of useful structures that are not found yet. */
    private static String listNames(Set<String> alreadyFound, int max) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> entry : USEFUL.entrySet()) {
            if (alreadyFound.contains(entry.getKey())) continue;
            names.add(entry.getValue());
            if (names.size() >= max) break;
        }
        return String.join(", ", names);
    }
}
