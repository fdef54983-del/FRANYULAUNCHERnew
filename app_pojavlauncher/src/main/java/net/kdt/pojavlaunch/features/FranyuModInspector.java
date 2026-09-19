package net.kdt.pojavlaunch.features;

import net.kdt.pojavlaunch.instances.Instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FranyuModInspector {
    public static final class Conflict {
        public final String id;
        public final List<String> files;

        public Conflict(String id, List<String> files) {
            this.id = id;
            this.files = files;
        }
    }

    public static List<File> modFiles(Instance instance) {
        File dir = new File(instance.getGameDirectory(), "mods");
        File[] files = dir.listFiles((d, n) -> n != null && n.toLowerCase().endsWith(".jar"));
        List<File> result = new ArrayList<>();
        if (files != null) Collections.addAll(result, files);
        return result;
    }

    public static List<Conflict> findConflicts(Instance instance) {
        Map<String, List<String>> ids = new HashMap<>();
        for (File file : modFiles(instance)) {
            String id = readModId(file);
            if (id == null) {
                id = file.getName()
                        .replaceFirst("(?i)\\.jar$", "")
                        .replaceAll("-[0-9].*$", "")
                        .toLowerCase();
            }
            ids.computeIfAbsent(id, k -> new ArrayList<>()).add(file.getName());
        }

        List<Conflict> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ids.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(new Conflict(entry.getKey(), entry.getValue()));
            }
        }
        return conflicts;
    }

    private static String readModId(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            String[] names = {
                    "fabric.mod.json",
                    "quilt.mod.json",
                    "META-INF/mods.toml",
                    "mcmod.info"
            };

            for (String name : names) {
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) continue;

                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(zip.getInputStream(entry), "UTF-8"))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count++ < 300) {
                        content.append(line).append('\n');
                    }
                }

                Matcher jsonId = Pattern.compile(
                        "\\"id\\"\\s*:\\s*\\"([^\\"]+)\\"")
                        .matcher(content);
                if (jsonId.find()) return jsonId.group(1).toLowerCase();

                Matcher tomlId = Pattern.compile(
                        "modId\\s*=\\s*\\"([^\\"]+)\\"")
                        .matcher(content);
                if (tomlId.find()) return tomlId.group(1).toLowerCase();

                Matcher legacyId = Pattern.compile(
                        "\\"modid\\"\\s*:\\s*\\"([^\\"]+)\\"")
                        .matcher(content);
                if (legacyId.find()) return legacyId.group(1).toLowerCase();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String suspectModsForRamSpike(Instance instance, int previousMb, int currentMb) {
        int delta = currentMb - previousMb;
        if (delta < 128) return null;

        List<File> mods = modFiles(instance);
        if (mods.isEmpty()) return null;

        StringBuilder result = new StringBuilder();
        int count = Math.min(3, mods.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) result.append(", ");
            result.append(mods.get(i).getName());
        }

        return "RAM elevada entre sesiones (+" + delta + " MB). Mods correlacionados: "
                + result + ". Esto es una correlación, no una prueba de fuga.";
    }

    private FranyuModInspector() {
    }
}
