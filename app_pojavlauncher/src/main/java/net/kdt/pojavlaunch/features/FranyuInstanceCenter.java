package net.kdt.pojavlaunch.features;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Foundation facade for instance-centric features in FranyuLauncher 1.4.
 * InstanceManager remains the source of truth for persistence and selection.
 */
public final class FranyuInstanceCenter {
    public static List<Instance> recentInstances(int limit) {
        List<Instance> instances = new ArrayList<>(InstanceManager.getImmutableInstanceList());
        Collections.sort(instances, Comparator.comparingLong((Instance i) -> i.lastPlayedAt).reversed());
        if (limit <= 0 || instances.size() <= limit) return instances;
        return new ArrayList<>(instances.subList(0, limit));
    }

    public static List<Instance> allInstances() {
        return new ArrayList<>(InstanceManager.getImmutableInstanceList());
    }

    public static int modCount(Instance instance) {
        if (instance == null) return 0;
        File mods = new File(instance.getGameDirectory(), "mods");
        File[] files = mods.isDirectory()
                ? mods.listFiles((dir, name) -> name != null
                && (name.toLowerCase().endsWith(".jar")
                || name.toLowerCase().endsWith(".zip")))
                : null;
        return files == null ? 0 : files.length;
    }

    private FranyuInstanceCenter() {
    }
}
