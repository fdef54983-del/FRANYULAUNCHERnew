package net.kdt.pojavlaunch.multirt;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.downloader.Downloader;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages downloading and provisioning of Java runtimes across different Java major versions
 * (Java 8, 17, 21, 25, etc.) and provides an interactive catalog of runtimes for test & error.
 */
public class RuntimeCatalogManager {

    private static final String TAG = "RuntimeCatalog";

    public static class RemoteRuntimeEntry {
        public final int javaMajor;
        public final String name;
        public final String displayName;
        public final String description;
        public final String downloadUrl;

        public RemoteRuntimeEntry(int javaMajor, String name, String displayName, String description, String downloadUrl) {
            this.javaMajor = javaMajor;
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.downloadUrl = downloadUrl;
        }
    }

    public static final List<RemoteRuntimeEntry> CATALOG = new ArrayList<>();

    static {
        // Build 15+ selectable runtime entries covering various versions and configurations
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Multiarch", "Java 8 (Mojo Multiarch)", "Recomendado para Minecraft 1.12.2 y modloaders antiguos (Forge/OptiFine)", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Legacy", "Java 8 (OpenJDK 8u302)", "Optimizado para versiones clásicas de Minecraft 1.7.10 a 1.16.5", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-OptiFine", "Java 8 (Perfil OptiFine)", "Compatibilidad mejorada con shaders en 1.12.2", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Forge", "Java 8 (Forge 1.12.2)", "Para modpacks pesados de Forge en 1.12.2", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Multiarch", "Java 17 (OpenJDK 17 LTS)", "Recomendado para Minecraft 1.17 hasta 1.20.4", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Fabric", "Java 17 (Fabric 1.18 - 1.20)", "Optimizado para Fabric y Sodium en 1.18/1.19", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Forge", "Java 17 (Modern Forge)", "Para Forge y NeoForge en 1.18 a 1.20.2", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-ZGC", "Java 17 (Low Latency GC)", "Configuración para menor congelamiento de frames", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Multiarch", "Java 21 (OpenJDK 21 LTS)", "Recomendado para Minecraft 1.20.5+ y 1.21+", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Fabric", "Java 21 (Fabric 1.21)", "Rendimiento óptimo con mods modernos en 1.21.x", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-NeoForge", "Java 21 (NeoForge 1.21)", "Para modpacks NeoForge modernos", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-GenerationalZGC", "Java 21 (GenZGC)", "Recolector de basura generacional ultra rápido", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(25, "JRE-25-Experimental", "Java 25 (OpenJDK Early Access)", "Para pruebas experimentales de nuevas versiones de Minecraft", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre25-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(25, "JRE-25-Performance", "Java 25 (Compilador JIT experimental)", "Modo de alto rendimiento en pruebas", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre25-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Testing", "Java 21 (Prueba y Error)", "Perfil secundario aislado para pruebas sin alterar el principal", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
    }

    public static List<RemoteRuntimeEntry> getCatalog() {
        return CATALOG;
    }

    public static RemoteRuntimeEntry getBestRuntimeForJava(int majorVersion) {
        for (RemoteRuntimeEntry entry : CATALOG) {
            if (entry.javaMajor == majorVersion) {
                return entry;
            }
        }
        if (majorVersion <= 8) return CATALOG.get(0);
        if (majorVersion <= 17) return CATALOG.get(4);
        return CATALOG.get(8);
    }

    public interface InstallCallback {
        void onSuccess(String runtimeName);
        void onError(Throwable error);
    }

    /**
     * Downloads and installs a runtime from a Pojav-format ZIP containing universal and architecture tar.xz files.
     */
    public static void downloadAndInstallRuntime(Context context, RemoteRuntimeEntry entry, InstallCallback callback) {
        ProgressLayout.setProgress(ProgressLayout.UNPACK_RUNTIME, 0, R.string.multirt_downloading_runtime, entry.name);

        net.kdt.pojavlaunch.PojavApplication.sExecutorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(entry.downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    url = new URL(newUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                }

                long totalBytes = conn.getContentLength();
                InputStream in = new BufferedInputStream(conn.getInputStream());

                ByteArrayOutputStream universalBaos = new ByteArrayOutputStream();
                ByteArrayOutputStream binBaos = new ByteArrayOutputStream();
                String versionString = "1.0";

                String requiredBinName = "bin-" + Architecture.archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz";

                // Read zip entries directly
                ZipInputStream zis = new ZipInputStream(in);
                ZipEntry ze;
                byte[] buffer = new byte[16384];

                while ((ze = zis.getNextEntry()) != null) {
                    String name = ze.getName();
                    if (name.equals("universal.tar.xz")) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            universalBaos.write(buffer, 0, read);
                        }
                    } else if (name.equals(requiredBinName)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            binBaos.write(buffer, 0, read);
                        }
                    } else if (name.equals("version")) {
                        ByteArrayOutputStream verBaos = new ByteArrayOutputStream();
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            verBaos.write(buffer, 0, read);
                        }
                        versionString = new String(verBaos.toByteArray(), StandardCharsets.UTF_8).trim();
                    }
                    zis.closeEntry();
                }
                zis.close();

                if (universalBaos.size() == 0 || binBaos.size() == 0) {
                    throw new IOException("El archivo descargado no contiene los binarios para la arquitectura: " + requiredBinName);
                }

                MultiRTUtils.installRuntimeNamedBinpack(
                        new ByteArrayInputStream(universalBaos.toByteArray()),
                        new ByteArrayInputStream(binBaos.toByteArray()),
                        entry.name,
                        versionString
                );
                MultiRTUtils.postPrepare(entry.name);

                Tools.runOnUiThread(() -> {
                    ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME);
                    if (context != null) {
                        Toast.makeText(context, context.getString(R.string.multirt_install_success, entry.displayName), Toast.LENGTH_SHORT).show();
                    }
                    if (callback != null) callback.onSuccess(entry.name);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error installing runtime " + entry.name, e);
                Tools.runOnUiThread(() -> {
                    ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME);
                    if (context != null) {
                        Tools.showError(context, e);
                    }
                    if (callback != null) callback.onError(e);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * Opens a selection dialog showing all 15 catalog runtimes, indicating which ones are already installed,
     * and letting the user download and install any of them with one tap.
     */
    public static void showCatalogDialog(Context context, Runnable onDismissOrInstall) {
        List<Runtime> installedRuntimes = MultiRTUtils.getRuntimes();
        String[] displayItems = new String[CATALOG.size()];

        for (int i = 0; i < CATALOG.size(); i++) {
            RemoteRuntimeEntry entry = CATALOG.get(i);
            boolean isInstalled = false;
            for (Runtime r : installedRuntimes) {
                if (r.name.equals(entry.name) && r.versionString != null) {
                    isInstalled = true;
                    break;
                }
            }
            String badge = isInstalled ? " " + context.getString(R.string.multirt_installed_badge) : "";
            displayItems[i] = entry.displayName + badge + "\n" + entry.description;
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.multirt_catalog_title)
                .setItems(displayItems, (d, which) -> {
                    RemoteRuntimeEntry selectedEntry = CATALOG.get(which);
                    ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setMessage(context.getString(R.string.multirt_downloading_runtime, selectedEntry.displayName));
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    downloadAndInstallRuntime(context, selectedEntry, new InstallCallback() {
                        @Override
                        public void onSuccess(String runtimeName) {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (onDismissOrInstall != null) onDismissOrInstall.run();
                        }

                        @Override
                        public void onError(Throwable error) {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (onDismissOrInstall != null) onDismissOrInstall.run();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.show();
    }
}
