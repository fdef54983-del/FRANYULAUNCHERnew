package net.kdt.pojavlaunch.features;

import android.content.Context;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central diagnostic facade for FranyuLauncher 1.4.
 * Existing checks remain in their original systems; this class composes them
 * instead of creating parallel implementations.
 */
public final class FranyuDiagnosticsCenter {
    public static final class Result {
        public final boolean storageAccessible;
        public final boolean instanceAccessible;
        public final long usableBytes;
        public final int freeRamMb;
        public final boolean compatibleJavaInstalled;

        Result(boolean storageAccessible, boolean instanceAccessible, long usableBytes,
               int freeRamMb, boolean compatibleJavaInstalled) {
            this.storageAccessible = storageAccessible;
            this.instanceAccessible = instanceAccessible;
            this.usableBytes = usableBytes;
            this.freeRamMb = freeRamMb;
            this.compatibleJavaInstalled = compatibleJavaInstalled;
        }

        public boolean isHealthy() {
            return storageAccessible && instanceAccessible
                    && usableBytes != 0 && freeRamMb >= 384
                    && compatibleJavaInstalled;
        }
    }

    public static Result inspect(Context context, Instance instance, int javaVersion) {
        boolean storage = false;
        try {
            storage = Tools.checkStorageRoot(context);
        } catch (Throwable ignored) {
        }

        File probe = instance == null ? null : instance.getGameDirectory();
        while (probe != null && !probe.exists()) probe = probe.getParentFile();

        boolean instanceAccessible = probe != null && probe.canRead() && probe.canWrite();
        long usable = probe == null ? 0 : probe.getUsableSpace();
        int freeRam = Tools.getFreeDeviceMemory(context);

        boolean java = false;
        try {
            final int required = javaVersion;
            java = MultiRTUtils.getRuntimes().stream()
                    .anyMatch(runtime -> runtime.javaVersion >= required);
        } catch (Throwable ignored) {
        }

        return new Result(storage, instanceAccessible, usable, freeRam, java);
    }

    public static String summarize(Context context, Instance instance, int javaVersion) {
        Result result = inspect(context, instance, javaVersion);
        List<String> issues = new ArrayList<>();
        if (!result.storageAccessible) issues.add("almacenamiento");
        if (!result.instanceAccessible) issues.add("carpeta de instancia");
        if (result.usableBytes > 0 && result.usableBytes < 1024L * 1024L * 1024L) {
            issues.add("espacio libre");
        }
        if (result.freeRamMb < 384) issues.add("RAM libre");
        if (!result.compatibleJavaInstalled) issues.add("Java compatible");

        if (issues.isEmpty()) return "Diagnóstico: no se detectaron problemas básicos.";
        return "Diagnóstico: revisar " + join(issues) + ".";
    }

    private static String join(List<String> values) {
        if (values.isEmpty()) return "";
        if (values.size() == 1) return values.get(0);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) result.append(i == values.size() - 1 ? " y " : ", ");
            result.append(values.get(i));
        }
        return result.toString();
    }

    private FranyuDiagnosticsCenter() {
    }
}
