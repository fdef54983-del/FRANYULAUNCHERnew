package net.kdt.pojavlaunch.features;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.multirt.RuntimeCatalogManager;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;
import net.kdt.pojavlaunch.value.DependentLibrary;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * FranyuSuiteV15: Suite Oficial de Rendimiento, Herramientas, Shaders, Mods y Runtimes Java
 * con diseño de consola de juegos de alta fidelidad y funciones 100% reales.
 */
public final class FranyuSuiteV15 {

    private FranyuSuiteV15() {}

    /**
     * Muestra el menú principal de Franyu Suite v1.5 con diseño visual de consola y animaciones.
     */
    public static void showSuiteDialog(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int p16 = Math.round(16 * density);
        int p12 = Math.round(12 * density);
        int p8 = Math.round(8 * density);
        int p4 = Math.round(4 * density);

        int cBgRoot = Color.parseColor("#080C14");
        int cEmerald = Color.parseColor("#10B981");
        int cCyan = Color.parseColor("#38BDF8");
        int cAmber = Color.parseColor("#F59E0B");
        int cPurple = Color.parseColor("#A855F7");
        int cTextLight = Color.parseColor("#F8FAFC");
        int cTextMuted = Color.parseColor("#94A3B8");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p16, p16, p16, p16);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(cBgRoot);
        rootBg.setCornerRadius(22 * density);
        rootBg.setStroke(Math.round(1.5f * density), Color.parseColor("#059669"));
        root.setBackground(rootBg);

        // Header Top Row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(context);
        badge.setText(" ⚡ FRANYU SUITE v1.5 • OFICIAL ");
        badge.setTextSize(10);
        badge.setTextColor(Color.WHITE);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#059669"));
        badgeBg.setCornerRadius(8 * density);
        badge.setBackground(badgeBg);
        badge.setPadding(p8, p4, p8, p4);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(0, -2, 1f);
        headerRow.addView(badge, badgeLp);

        TextView closeIcon = new TextView(context);
        closeIcon.setText("✕");
        closeIcon.setTextColor(cTextMuted);
        closeIcon.setTextSize(16);
        closeIcon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        closeIcon.setPadding(p8, p4, p8, p4);
        headerRow.addView(closeIcon);
        root.addView(headerRow);

        // Main Title
        TextView title = new TextView(context);
        title.setText("Centro de Rendimiento y Herramientas");
        title.setTextSize(18);
        title.setTextColor(cTextLight);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = p8;
        root.addView(title, titleLp);

        TextView subtitle = new TextView(context);
        subtitle.setText("Optimiza tus FPS, gestiona drivers, shaders, mods y runtimes en 1 toque.");
        subtitle.setTextSize(12);
        subtitle.setTextColor(cTextMuted);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = p4;
        subLp.bottomMargin = p12;
        root.addView(subtitle, subLp);

        // Telemetry Chips Bar
        LinearLayout telemetryBar = new LinearLayout(context);
        telemetryBar.setOrientation(LinearLayout.HORIZONTAL);
        telemetryBar.setGravity(Gravity.CENTER_VERTICAL);

        long freeRam = Tools.getFreeDeviceMemory(context);
        String renderer = LauncherPreferences.PREF_RENDERER == null ? "Default" : LauncherPreferences.PREF_RENDERER;
        int fsrPercent = Math.round(LauncherPreferences.PREF_SCALE_FACTOR * 100);

        addTelemetryChip(context, telemetryBar, "🧠 " + freeRam + " MB RAM", cEmerald, density);
        addTelemetryChip(context, telemetryBar, "🎮 " + renderer.toUpperCase(Locale.ROOT), cCyan, density);
        addTelemetryChip(context, telemetryBar, "🔍 FSR: " + fsrPercent + "%", cAmber, density);

        LinearLayout.LayoutParams telemLp = new LinearLayout.LayoutParams(-1, -2);
        telemLp.bottomMargin = p12;
        root.addView(telemetryBar, telemLp);

        // Scrollable Options Content
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Section 1: Rendimiento y Drivers
        addSectionHeader(context, content, "🚀 RENDIMIENTO Y DRIVERS GRÁFICOS", cCyan, density);
        addOptionCard(context, content, "🎮 Gestor de Drivers Reales", "Configura GL4ES (Exynos/Mali), Zink Vulkan o VirGL LTW.", cCyan, density, () -> showDriversManager(context));
        addOptionCard(context, content, "⚡ Optimizador de Memoria y GC", "Ajusta automáticamente el recolector según tu RAM libre.", cEmerald, density, () -> applyAutoGcOptimizer(context));
        addOptionCard(context, content, "🔍 Reescalado Espacial FSR (Nitidez)", "Multiplica tus FPS reduciendo resolución interna con alta nitidez.", cAmber, density, () -> showFsrResolutionDialog(context));
        addOptionCard(context, content, "🧹 Limpiador de Caché y Logs", "Libera almacenamiento borrando archivos temporales y reportes de crash.", cEmerald, density, () -> cleanTextureAndChunkCache(context));

        // Section 2: Shaders y Mods
        addSectionHeader(context, content, "🎨 SHADERS, MODS Y MODPACKS", cPurple, density);
        addOptionCard(context, content, "✨ Instalador de Shaders Reales", "Descarga packs legítimos (MakeUp, BSL, Complementary) de Modrinth.", cPurple, density, () -> showShaderInstaller(context));
        addOptionCard(context, content, "📦 Verificador de Mods en Modrinth", "Consulta hashes en la API de Modrinth para comprobar compatibilidad.", cCyan, density, () -> checkModsUpdates(context));
        addOptionCard(context, content, "🌐 Importador Directo de Modpacks por URL", "Descarga e instala paquetes .mrpack o .zip desde enlaces directos.", cAmber, density, () -> importModpackFromUrl(context));
        addOptionCard(context, content, "🛡️ Comprobador de Integridad SHA-1", "Compara los hashes de librerías y cliente contra el JSON oficial.", cEmerald, density, () -> checkGameFilesIntegrity(context));
        addOptionCard(context, content, "📁 Explorador de Archivos de Instancia", "Acceso directo a carpetas saves, mods, shaderpacks y screenshots.", cTextMuted, density, () -> openInternalFileExplorer(context));

        // Section 3: Runtimes Java
        addSectionHeader(context, content, "☕ RUNTIMES JAVA Y SISTEMA", cEmerald, density);
        addOptionCard(context, content, "☕ Catálogo Oficial de Java (8, 17, 21)", "Descarga o repara tus entornos OpenJDK multi-arquitectura.", cEmerald, density, () -> RuntimeCatalogManager.showCatalogDialog(context, null));
        addOptionCard(context, content, "👤 Skins Personalizadas Offline", "Instala tu skin personalizada en formato PNG para juego sin conexión.", cCyan, density, () -> setupOfflineSkin(context));

        scroll.addView(content);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, Math.round(440 * density));
        root.addView(scroll, scrollLp);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeIcon.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void addTelemetryChip(Context context, LinearLayout parent, String text, int color, float density) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTextColor(color);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        int p6 = Math.round(6 * density);
        int p3 = Math.round(3 * density);
        chip.setPadding(p6, p3, p6, p3);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(6 * density);
        bg.setStroke(Math.round(1 * density), color);
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = Math.round(6 * density);
        parent.addView(chip, lp);
    }

    private static void addSectionHeader(Context context, LinearLayout parent, String title, int color, float density) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(11);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Math.round(14 * density);
        lp.bottomMargin = Math.round(6 * density);
        parent.addView(tv, lp);
    }

    private static void addOptionCard(Context context, LinearLayout parent, String title, String desc, int accentColor, float density, Runnable onClick) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int p12 = Math.round(12 * density);
        card.setPadding(p12, p12, p12, p12);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0B132B"));
        bg.setCornerRadius(14 * density);
        bg.setStroke(Math.round(1 * density), Color.parseColor("#1E293B"));
        card.setBackground(bg);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(Color.parseColor("#F8FAFC"));
        tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvDesc = new TextView(context);
        tvDesc.setText(desc);
        tvDesc.setTextSize(11);
        tvDesc.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2);
        descLp.topMargin = Math.round(3 * density);
        card.addView(tvDesc, descLp);

        card.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                if (onClick != null) onClick.run();
            }).start();
        });

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = Math.round(8 * density);
        parent.addView(card, cardLp);
    }

    /** 1.1 Gestor de Drivers Reales con IDs válidos y persistencia */
    private static void showDriversManager(Context context) {
        String[] driverLabels = new String[]{
                "GL4ES 1.1.4 (Estándar OpenGL 2.1 - Recomendado Exynos, Mali y Gama Baja)",
                "Holy GL4ES (OpenGL ES 3.0 - Optimizado para shaders ligeros)",
                "Zink (OpenGL sobre Vulkan - Dispositivos potentes con Vulkan 1.1+)",
                "VirGL / LTW (Requerido para Minecraft 1.21.5+)"
        };
        String[] driverIds = new String[]{
                "opengles2",
                "opengles3",
                "vulkan_zink",
                "opengles3_ltw"
        };

        new AlertDialog.Builder(context)
                .setTitle("Gestor de Renderizadores Gráficos")
                .setItems(driverLabels, (d, which) -> {
                    String selectedId = driverIds[which];
                    LauncherPreferences.PREF_RENDERER = selectedId;
                    LauncherPreferences.DEFAULT_PREF.edit().putString("renderer", selectedId).apply();
                    Toast.makeText(context, "Renderizador global guardado: " + driverLabels[which].split(" \\(")[0], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 1.2 Optimizador Automático de Garbage Collector (GC) */
    private static void applyAutoGcOptimizer(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        long freeRamMb = Tools.getFreeDeviceMemory(context);
        String gcFlag;
        String explanation;

        if (freeRamMb >= 4000) {
            gcFlag = "-XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive -XX:+UnlockExperimentalVMOptions";
            explanation = "Shenandoah Generacional (Baja latencia, óptimo para 4GB+ RAM libre)";
        } else if (freeRamMb >= 2000) {
            gcFlag = "-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+ParallelRefProcEnabled";
            explanation = "G1GC Equilibrado (Excelente rendimiento en 2GB a 4GB RAM libre)";
        } else {
            gcFlag = "-XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10";
            explanation = "SerialGC Ultra-Ligero (Ahorro de memoria para dispositivos con poca RAM)";
        }

        instance.jvmArgs = (instance.jvmArgs != null && !instance.jvmArgs.isEmpty())
                ? instance.jvmArgs + " " + gcFlag
                : gcFlag;
        try {
            instance.write();
            new AlertDialog.Builder(context)
                    .setTitle("Optimizador de Garbage Collector (GC)")
                    .setMessage("Configuración de memoria aplicada con éxito:\n\n"
                            + "• RAM Libre Detectada: " + freeRamMb + " MB\n"
                            + "• Algoritmo Seleccionado: " + explanation + "\n"
                            + "• Flags JVM: " + gcFlag)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Exception e) {
            Tools.showError(context, e);
        }
    }

    /** 1.3 Reescalado Espacial FSR (Ajuste de Resolución) */
    private static void showFsrResolutionDialog(Context context) {
        String[] scaleOptions = new String[]{
                "FSR Rendimiento Ultra: 50% (Doble de FPS - Gama Baja)",
                "FSR Rendimiento: 65% (Recomendado para modpacks pesados)",
                "FSR Equilibrado: 75% (Balance ideal nitidez / FPS)",
                "FSR Calidad: 85% (Alta fidelidad visual)",
                "Nativo: 100% (Resolución original de pantalla)"
        };
        int[] scaleValues = new int[]{50, 65, 75, 85, 100};

        new AlertDialog.Builder(context)
                .setTitle("Reescalado Espacial FSR / Nitidez")
                .setItems(scaleOptions, (d, which) -> {
                    int val = scaleValues[which];
                    LauncherPreferences.PREF_SCALE_FACTOR = val / 100f;
                    LauncherPreferences.DEFAULT_PREF.edit().putFloat("scale_factor", LauncherPreferences.PREF_SCALE_FACTOR).apply();
                    Toast.makeText(context, "Escalado FSR configurado al " + val + "%", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 1.4 Limpiador Automático de Caché y Logs */
    private static void cleanTextureAndChunkCache(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        File gameDir = instance.getGameDirectory();
        File[] targets = new File[]{
                new File(gameDir, "logs"),
                new File(gameDir, "crash-reports"),
                new File(gameDir, ".cache"),
                new File(gameDir, ".fabric"),
                new File(gameDir, ".mixin.out")
        };

        long freedBytes = 0;
        int filesDeleted = 0;

        for (File target : targets) {
            if (target.exists()) {
                File[] files = target.listFiles();
                if (files != null) {
                    for (File f : files) {
                        freedBytes += f.length();
                        if (f.delete()) filesDeleted++;
                    }
                }
            }
        }

        long freedMb = freedBytes / (1024 * 1024);
        new AlertDialog.Builder(context)
                .setTitle("Limpiador de Caché y Registros")
                .setMessage("¡Limpieza completada con éxito!\n\n"
                        + "• Archivos temporales eliminados: " + filesDeleted + "\n"
                        + "• Espacio liberado: " + (freedMb > 0 ? freedMb + " MB" : freedBytes + " Bytes") + "\n"
                        + "Tus mundos y partidas guardadas permanecen intactos.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** 2.1 Instalador de Shaders Reales desde Modrinth API */
    private static void showShaderInstaller(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] shaderTitles = new String[]{
                "MakeUp - Ultra Fast (Máximo Rendimiento Móvil)",
                "BSL Shaders (Iluminación cálida y agua realista)",
                "Complementary Reimagined (Alta fidelidad optimizada)",
                "Solas Shader (Efectos atmosféricos ligeros)"
        };
        String[] projectSlugs = new String[]{
                "makeup-ultra-fast-shaders",
                "bsl-shaders",
                "complementary-reimagined",
                "solas-shader"
        };

        new AlertDialog.Builder(context)
                .setTitle("Instalador de Shaders Oficiales (Modrinth)")
                .setItems(shaderTitles, (d, which) -> {
                    String slug = projectSlugs[which];
                    String titleName = shaderTitles[which].split(" \\(")[0];

                    ProgressDialog pd = new ProgressDialog(context);
                    pd.setTitle("Descargando Shaderpack");
                    pd.setMessage("Consultando Modrinth API para " + titleName + "...");
                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pd.setMax(100);
                    pd.setCancelable(false);
                    pd.show();

                    PojavApplication.sExecutorService.execute(() -> {
                        try {
                            String apiUrl = "https://api.modrinth.com/v2/project/" + slug + "/version";
                            String jsonStr = httpGet(apiUrl);
                            JSONArray versions = new JSONArray(jsonStr);
                            if (versions.length() == 0) throw new IOException("No se encontraron versiones disponibles en Modrinth.");

                            JSONObject latestVersion = versions.getJSONObject(0);
                            JSONArray files = latestVersion.getJSONArray("files");
                            if (files.length() == 0) throw new IOException("El shaderpack no contiene archivos descargables.");

                            JSONObject fileObj = files.getJSONObject(0);
                            String downloadUrl = fileObj.getString("url");
                            String fileName = fileObj.getString("filename");

                            File shaderpacksDir = new File(instance.getGameDirectory(), "shaderpacks");
                            if (!shaderpacksDir.exists()) shaderpacksDir.mkdirs();

                            File destFile = new File(shaderpacksDir, fileName);
                            downloadFileWithProgress(downloadUrl, destFile, pd, "Descargando " + fileName + "...");

                            Tools.runOnUiThread(() -> {
                                if (pd.isShowing()) pd.dismiss();
                                Toast.makeText(context, "¡Shaderpack " + fileName + " instalado correctamente en shaderpacks/!", Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception e) {
                            Tools.runOnUiThread(() -> {
                                if (pd.isShowing()) pd.dismiss();
                                Tools.showError(context, e);
                            });
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 2.2 Verificador Real de Mods en Modrinth */
    private static void checkModsUpdates(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        File modsDir = new File(instance.getGameDirectory(), "mods");
        if (!modsDir.exists() || modsDir.listFiles() == null) {
            Toast.makeText(context, "No hay carpeta mods/ o está vacía.", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] mods = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (mods == null || mods.length == 0) {
            Toast.makeText(context, "No se encontraron mods .jar en la carpeta mods/.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Analizando hashes SHA-1 de " + mods.length + " mods...");
        pd.setCancelable(false);
        pd.show();

        PojavApplication.sExecutorService.execute(() -> {
            try {
                JSONArray hashesArray = new JSONArray();
                for (File mod : mods) {
                    String hash = computeSha1(mod);
                    if (!hash.isEmpty()) {
                        hashesArray.put(hash);
                    }
                }

                JSONObject req = new JSONObject();
                req.put("hashes", hashesArray);
                req.put("algorithm", "sha1");

                String resp = httpPostJson("https://api.modrinth.com/v2/version_files", req.toString());
                JSONObject resultObj = new JSONObject(resp);

                int matched = resultObj.length();
                int total = mods.length;

                Tools.runOnUiThread(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle("Reporte de Mods (Modrinth API)")
                            .setMessage("Resultados de la comprobación en línea:\n\n"
                                    + "• Mods locales analizados: " + total + "\n"
                                    + "• Mods verificados en Modrinth: " + matched + "\n"
                                    + "• Mods externos/privados: " + (total - matched) + "\n\n"
                                    + (matched > 0
                                    ? "Los mods reconocidos corresponden a versiones legítimas registradas en la base de datos de Modrinth."
                                    : "Los mods instalados son de fuentes personalizadas o paquetes independientes."))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    Toast.makeText(context, "No se pudo conectar con Modrinth: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /** 2.3 Importador Directo de Modpacks por URL (.mrpack o .zip) */
    private static void importModpackFromUrl(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(context);
        input.setHint("https://cdn.modrinth.com/.../modpack.mrpack o .zip");

        new AlertDialog.Builder(context)
                .setTitle("Importar Modpack por URL")
                .setMessage("Introduce el enlace directo al archivo .mrpack o .zip:")
                .setView(input)
                .setPositiveButton("Descargar e Instalar", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(context, "Introduce una URL HTTP/HTTPS válida.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ProgressDialog pd = new ProgressDialog(context);
                    pd.setTitle("Descargando Modpack");
                    pd.setMessage("Conectando con el servidor...");
                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pd.setMax(100);
                    pd.setCancelable(false);
                    pd.show();

                    PojavApplication.sExecutorService.execute(() -> {
                        try {
                            File cacheDir = Tools.DIR_CACHE;
                            if (!cacheDir.exists()) cacheDir.mkdirs();

                            boolean isMrpack = url.toLowerCase(Locale.ROOT).contains(".mrpack");
                            File tempFile = new File(cacheDir, "downloaded_modpack_" + System.currentTimeMillis() + (isMrpack ? ".mrpack" : ".zip"));

                            downloadFileWithProgress(url, tempFile, pd, "Descargando archivo del modpack...");

                            Tools.runOnUiThread(() -> pd.setMessage("Descomprimiendo archivos en la instancia..."));

                            File gameDir = instance.getGameDirectory();
                            if (!gameDir.exists()) gameDir.mkdirs();

                            try (ZipFile zip = new ZipFile(tempFile)) {
                                if (zip.getEntry("overrides/") != null) {
                                    ZipUtils.zipExtract(zip, "overrides/", gameDir);
                                }
                                if (zip.getEntry("client-overrides/") != null) {
                                    ZipUtils.zipExtract(zip, "client-overrides/", gameDir);
                                }
                                // Extract root files if it's a standard zip without overrides prefix
                                if (zip.getEntry("overrides/") == null && zip.getEntry("modrinth.index.json") == null) {
                                    ZipUtils.zipExtract(zip, "", gameDir);
                                }
                            } finally {
                                tempFile.delete();
                            }

                            Tools.runOnUiThread(() -> {
                                if (pd.isShowing()) pd.dismiss();
                                new AlertDialog.Builder(context)
                                        .setTitle("¡Modpack Instalado con Éxito!")
                                        .setMessage("Los archivos y carpetas del modpack se han extraído correctamente en:\n\n" + gameDir.getAbsolutePath())
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            });
                        } catch (Exception e) {
                            Tools.runOnUiThread(() -> {
                                if (pd.isShowing()) pd.dismiss();
                                Tools.showError(context, e);
                            });
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 2.4 Comprobador de Integridad SHA-1 Real */
    private static void checkGameFilesIntegrity(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Verificando hashes oficiales de librerías y cliente...");
        pd.setCancelable(false);
        pd.show();

        PojavApplication.sExecutorService.execute(() -> {
            try {
                JMinecraftVersionList.Version versionInfo = Tools.getVersionInfo(instance.versionId);
                int totalChecked = 0;
                int healthy = 0;
                int corrupted = 0;
                int missing = 0;

                // 1. Check client jar
                if (versionInfo.downloads != null && versionInfo.downloads.containsKey("client")) {
                    net.kdt.pojavlaunch.value.MinecraftClientInfo clientInfo = versionInfo.downloads.get("client");
                    String expectedSha1 = clientInfo != null ? clientInfo.sha1 : null;
                    File clientJar = new File(Tools.DIR_HOME_VERSION + "/" + instance.versionId + "/" + instance.versionId + ".jar");
                    if (clientJar.exists()) {
                        totalChecked++;
                        String realSha1 = computeSha1(clientJar);
                        if (expectedSha1 != null && expectedSha1.equalsIgnoreCase(realSha1)) healthy++;
                        else corrupted++;
                    } else {
                        missing++;
                    }
                }

                // 2. Check libraries with declared sha1
                if (versionInfo.libraries != null) {
                    for (DependentLibrary lib : versionInfo.libraries) {
                        if (lib.downloads != null && lib.downloads.artifact != null && lib.downloads.artifact.sha1 != null) {
                            String expected = lib.downloads.artifact.sha1;
                            String relPath = Tools.artifactToPath(lib);
                            File libFile = new File(Tools.DIR_HOME_LIBRARY + "/" + relPath);
                            totalChecked++;
                            if (libFile.exists()) {
                                String actual = computeSha1(libFile);
                                if (expected.equalsIgnoreCase(actual)) healthy++;
                                else corrupted++;
                            } else {
                                missing++;
                            }
                        }
                    }
                }

                final int fTotal = totalChecked;
                final int fHealthy = healthy;
                final int fCorrupted = corrupted;
                final int fMissing = missing;

                Tools.runOnUiThread(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    String message = String.format(Locale.ROOT,
                            "Resultados de la comprobación criptográfica:\n\n"
                                    + "• Archivos verificados: %d\n"
                                    + "• Archivos 100%% íntegros: %d\n"
                                    + "• Archivos dañados / alterados: %d\n"
                                    + "• Archivos ausentes / pendientes: %d\n\n"
                                    + "%s",
                            fTotal, fHealthy, fCorrupted, fMissing,
                            (fCorrupted == 0 && fMissing == 0)
                                    ? "✅ Todos los archivos verificados coinciden con los hashes SHA-1 oficiales de Mojang."
                                    : "⚠️ Se detectaron archivos modificados o faltantes. El launcher los reparará automáticamente al iniciar.");

                    new AlertDialog.Builder(context)
                            .setTitle("Integridad Criptográfica SHA-1")
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    Tools.showError(context, e);
                });
            }
        });
    }

    /** 2.5 Explorador de Archivos Interno */
    private static void openInternalFileExplorer(Context context) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        if (instance == null) {
            Toast.makeText(context, "Selecciona una instancia primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        File gameDir = instance.getGameDirectory();
        String[] folders = new String[]{
                "📁 saves/ (Mundos y partidas guardadas)",
                "📁 mods/ (Mods instalados)",
                "📁 resourcepacks/ (Paquetes de texturas)",
                "📁 shaderpacks/ (Packs de shaders)",
                "📁 screenshots/ (Capturas de pantalla F2)",
                "📁 config/ (Configuraciones de mods)"
        };

        File[] targetDirs = new File[]{
                new File(gameDir, "saves"),
                new File(gameDir, "mods"),
                new File(gameDir, "resourcepacks"),
                new File(gameDir, "shaderpacks"),
                new File(gameDir, "screenshots"),
                new File(gameDir, "config")
        };

        new AlertDialog.Builder(context)
                .setTitle("Explorador de Archivos Interno")
                .setItems(folders, (d, which) -> {
                    File dir = targetDirs[which];
                    if (!dir.exists()) dir.mkdirs();
                    Tools.openPath(context, dir, false);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 3.1 Soporte de Skins Personalizadas para Cuentas Offline */
    private static void setupOfflineSkin(Context context) {
        File skinsDir = new File(Tools.DIR_GAME_HOME, "skins");
        if (!skinsDir.exists()) skinsDir.mkdirs();

        new AlertDialog.Builder(context)
                .setTitle("Skins para Cuentas Offline")
                .setMessage("Las cuentas offline pueden mostrar su skin personalizada.\n\n"
                        + "Coloca tu skin en formato PNG (64x64 o 64x32) con el nombre exacto de tu usuario en:\n"
                        + skinsDir.getAbsolutePath() + "/<tu_nombre>.png\n\n"
                        + "¿Deseas abrir la carpeta de skins ahora?")
                .setPositiveButton("Abrir Carpeta de Skins", (d, w) -> Tools.openPath(context, skinsDir, false))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String computeSha1(File file) {
        if (file == null || !file.isFile() || file.length() == 0) return "";
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "FranyuLauncher/1.5");
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP Error " + code + " al consultar " + urlStr);
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static String httpPostJson(String urlStr, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "FranyuLauncher/1.5");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP Error " + code + " en POST a " + urlStr);
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static void downloadFileWithProgress(String urlStr, File destination, ProgressDialog pd, String statusMessage) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "FranyuLauncher/1.5");
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP Error " + code + " al descargar " + urlStr);

        long contentLength = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buf = new byte[8192];
            long totalRead = 0;
            int n;
            int lastProgress = -1;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                totalRead += n;
                if (contentLength > 0 && pd != null) {
                    int progress = (int) ((totalRead * 100) / contentLength);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        final int p = progress;
                        final long cur = totalRead;
                        final long tot = contentLength;
                        Tools.runOnUiThread(() -> {
                            pd.setProgress(p);
                            pd.setMessage(statusMessage + " (" + (cur / 1024) + " KB / " + (tot / 1024) + " KB)");
                        });
                    }
                }
            }
        } finally {
            conn.disconnect();
        }
    }
}
