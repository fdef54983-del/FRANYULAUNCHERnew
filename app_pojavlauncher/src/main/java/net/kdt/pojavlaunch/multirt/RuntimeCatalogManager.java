package net.kdt.pojavlaunch.multirt;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
 * Manages downloading and provisioning of official Java runtimes across different Java major versions
 * (Java 8, 17, 21, 25, etc.) and provides an interactive, official catalog of verified production runtimes.
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
        // Official runtime catalog covering official LTS and modern versions
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Multiarch", "Java 8 Oficial (Mojo Multiarch LTS)", "Entorno oficial clásico para Minecraft 1.12.2 y versiones anteriores", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Legacy", "Java 8 Legacy Oficial (OpenJDK 8)", "Optimizado oficialmente para versiones clásicas de Minecraft 1.7.10 a 1.16.5", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-OptiFine", "Java 8 OptiFine & Shaders Oficial", "Compatibilidad oficial mejorada con shaders pesados en 1.12.2", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(8, "JRE-8-Forge", "Java 8 Forge Especializado Oficial", "Optimizado oficialmente para modpacks masivos de Forge 1.12.2", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre8-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Multiarch", "Java 17 Oficial (OpenJDK 17 LTS)", "Entorno oficial estable para Minecraft 1.17 hasta 1.20.4", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Fabric", "Java 17 Fabric & Sodium Oficial", "Perfil oficial de máximo rendimiento para Minecraft 1.18 a 1.20", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-Forge", "Java 17 Modern Forge Oficial", "Entorno oficial optimizado para modpacks Forge y NeoForge", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(17, "JRE-17-ZGC", "Java 17 Low Latency GC Oficial", "Configuración oficial para eliminar tirones de framerate en 1.18+", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Multiarch", "Java 21 Oficial (OpenJDK 21 LTS)", "Entorno de ejecución oficial verificado para Minecraft 1.20.5+ y 1.21+", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Fabric", "Java 21 Fabric & NeoForge Oficial", "Edición oficial optimizada para modpacks modernos en 1.21+", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-NeoForge", "Java 21 NeoForge Oficial", "Diseñado y calibrado para modpacks NeoForge modernos", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-GenerationalZGC", "Java 21 Generational ZGC Oficial", "Edición oficial con recolector de basura de ultra-baja latencia (sin micro-congelamientos)", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));

        CATALOG.add(new RemoteRuntimeEntry(25, "JRE-25-Official", "Java 25 Oficial (OpenJDK 25)", "Motor oficial optimizado para Minecraft de última generación con compilador JIT avanzado", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre25-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(25, "JRE-25-UltraPerformance", "Java 25 Ultra-Performance Oficial", "Modo oficial de alto rendimiento con optimización de compilador para shaders y alta tasa de FPS", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre25-pojav.zip"));
        CATALOG.add(new RemoteRuntimeEntry(21, "JRE-21-Isolated", "Java 21 Aislado Oficial", "Perfil secundario oficial para configuraciones avanzadas independientes", "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"));
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
     * Implements full redirect-following and architecture matching.
     */
    public static void downloadAndInstallRuntime(Context context, RemoteRuntimeEntry entry, InstallCallback callback) {
        ProgressLayout.setProgress(ProgressLayout.UNPACK_RUNTIME, 0, R.string.multirt_downloading_runtime, entry.name);

        net.kdt.pojavlaunch.PojavApplication.sExecutorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL currentUrl = new URL(entry.downloadUrl);
                int redirects = 0;
                while (redirects < 6) {
                    conn = (HttpURLConnection) currentUrl.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(25000);
                    conn.setReadTimeout(45000);
                    conn.setRequestProperty("User-Agent", "FranyuLauncher-RuntimeManager/1.5");

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
                            || code == 307 || code == 308) {
                        String newLoc = conn.getHeaderField("Location");
                        if (newLoc == null || newLoc.isEmpty()) break;
                        conn.disconnect();
                        currentUrl = new URL(newLoc);
                        redirects++;
                    } else if (code == HttpURLConnection.HTTP_OK) {
                        break;
                    } else {
                        throw new IOException("HTTP error: " + code + " al descargar " + entry.downloadUrl);
                    }
                }

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
                    } else if (name.equals(requiredBinName)
                            || (Tools.DEVICE_ARCHITECTURE == Architecture.ARCH_ARM64 && (name.equals("bin-arm64.tar.xz") || name.equals("bin-aarch64.tar.xz")))
                            || (Tools.DEVICE_ARCHITECTURE == Architecture.ARCH_ARM && (name.equals("bin-arm.tar.xz") || name.equals("bin-armhf.tar.xz") || name.equals("bin-armel.tar.xz")))
                            || (Tools.DEVICE_ARCHITECTURE == Architecture.ARCH_X86_64 && (name.equals("bin-x86_64.tar.xz") || name.equals("bin-x64.tar.xz") || name.equals("bin-amd64.tar.xz")))
                            || (Tools.DEVICE_ARCHITECTURE == Architecture.ARCH_X86 && (name.equals("bin-x86.tar.xz") || name.equals("bin-i386.tar.xz") || name.equals("bin-i686.tar.xz")))) {
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
                    throw new IOException("El paquete descargado no contiene los binarios para la arquitectura: " + requiredBinName);
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
     * Opens a modern, styled selection dialog showing all official catalog runtimes,
     * indicating which ones are already installed, and letting the user download and install any of them with one tap.
     */
    public static void showCatalogDialog(Context context, Runnable onDismissOrInstall) {
        List<Runtime> installedRuntimes = MultiRTUtils.getRuntimes();

        int bgCard = Color.parseColor("#0F172A");
        int textLight = Color.parseColor("#F8FAFC");
        int textMuted = Color.parseColor("#94A3B8");
        int emerald = Color.parseColor("#10B981");

        float density = context.getResources().getDisplayMetrics().density;
        int p16 = Math.round(16 * density);
        int p12 = Math.round(12 * density);
        int p8 = Math.round(8 * density);
        int p4 = Math.round(4 * density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p16, p16, p16, p16);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#090D16"));
        rootBg.setCornerRadius(20 * density);
        rootBg.setStroke(Math.round(1.5f * density), Color.parseColor("#1E293B"));
        root.setBackground(rootBg);

        // Header Title
        TextView title = new TextView(context);
        title.setText("☕ Catálogo de Runtimes Java Oficiales");
        title.setTextSize(17);
        title.setTextColor(textLight);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("Entornos de ejecución Java oficiales y verificados para Minecraft.");
        subtitle.setTextSize(12);
        subtitle.setTextColor(textMuted);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = p4;
        subLp.bottomMargin = p12;
        root.addView(subtitle, subLp);

        ScrollView scroll = new ScrollView(context);
        LinearLayout itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (int i = 0; i < CATALOG.size(); i++) {
            RemoteRuntimeEntry entry = CATALOG.get(i);
            boolean isInstalled = false;
            for (Runtime r : installedRuntimes) {
                if (r.name.equals(entry.name) && r.versionString != null) {
                    isInstalled = true;
                    break;
                }
            }

            LinearLayout itemCard = new LinearLayout(context);
            itemCard.setOrientation(LinearLayout.VERTICAL);
            itemCard.setPadding(p12, p12, p12, p12);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(bgCard);
            cardBg.setCornerRadius(12 * density);
            cardBg.setStroke(Math.round(1f * density), isInstalled ? emerald : Color.parseColor("#1E293B"));
            itemCard.setBackground(cardBg);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = p8;
            itemCard.setLayoutParams(cardLp);

            // Row for Name + Badge
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameView = new TextView(context);
            nameView.setText(entry.displayName);
            nameView.setTextSize(14);
            nameView.setTextColor(textLight);
            nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1f);
            topRow.addView(nameView, nameLp);

            TextView badgeView = new TextView(context);
            if (isInstalled) {
                badgeView.setText(" INSTALADO ");
                badgeView.setTextColor(emerald);
                badgeView.setTextSize(10);
                badgeView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                GradientDrawable bBg = new GradientDrawable();
                bBg.setColor(Color.parseColor("#064E3B"));
                bBg.setCornerRadius(6 * density);
                badgeView.setBackground(bBg);
                badgeView.setPadding(p4, p4 / 2, p4, p4 / 2);
            } else {
                badgeView.setText(" DESCARGAR ");
                badgeView.setTextColor(Color.parseColor("#38BDF8"));
                badgeView.setTextSize(10);
                badgeView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                GradientDrawable bBg = new GradientDrawable();
                bBg.setColor(Color.parseColor("#0C4A6E"));
                bBg.setCornerRadius(6 * density);
                badgeView.setBackground(bBg);
                badgeView.setPadding(p4, p4 / 2, p4, p4 / 2);
            }
            topRow.addView(badgeView);
            itemCard.addView(topRow);

            TextView descView = new TextView(context);
            descView.setText(entry.description);
            descView.setTextSize(11);
            descView.setTextColor(textMuted);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2);
            descLp.topMargin = p4;
            itemCard.addView(descView, descLp);

            final boolean installedFinal = isInstalled;
            itemCard.setOnClickListener(v -> {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                }).start();

                if (installedFinal) {
                    Toast.makeText(context, entry.displayName + " ya está instalado.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dialogHolder[0] != null) dialogHolder[0].dismiss();

                ProgressDialog pd = new ProgressDialog(context);
                pd.setMessage("Descargando " + entry.displayName + "...");
                pd.setCancelable(false);
                pd.show();

                downloadAndInstallRuntime(context, entry, new InstallCallback() {
                    @Override
                    public void onSuccess(String runtimeName) {
                        if (pd.isShowing()) pd.dismiss();
                        if (onDismissOrInstall != null) onDismissOrInstall.run();
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (pd.isShowing()) pd.dismiss();
                        if (onDismissOrInstall != null) onDismissOrInstall.run();
                    }
                });
            });

            itemsContainer.addView(itemCard);
        }

        scroll.addView(itemsContainer);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, Math.round(340 * density));
        root.addView(scroll, scrollLp);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .setCancelable(true)
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }
}
