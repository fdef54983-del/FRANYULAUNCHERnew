package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import git.artdeell.mojo.BuildConfig;

public class UpdateChecker implements AutoCloseable {
    private static final long CHECK_INTERVAL = 60L * 60L * 1000L;
    private static final String RELEASES_URL =
            "https://api.github.com/repos/fdef54983-del/FRANYULAUNCHERnew/releases/latest";
    private static final String PREFS = "franyu_updates";
    private static final String KEY_CHECK = "last_check";
    private static final String KEY_SKIPPED = "skipped_version";
    private static final String KEY_PENDING_APK = "pending_apk";
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean closed;

    public static final class Update {
        public final String version;
        public final String body;
        public final String apkUrl;

        public Update(String version, String body, String apkUrl) {
            this.version = version;
            this.body = body;
            this.apkUrl = apkUrl;
        }
    }

    public interface Callback {
        void onResult(Update update);
    }

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void check(Callback callback) {
        long last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_CHECK, 0);
        if (System.currentTimeMillis() - last < CHECK_INTERVAL) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_CHECK, System.currentTimeMillis()).apply();

        executor.execute(() -> {
            Update update = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(RELEASES_URL).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "FranyuLauncher");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder json = new StringBuilder();
                    try (InputStream in = connection.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            json.append(new String(buffer, 0, n, "UTF-8"));
                        }
                    }
                    JsonObject release = new JsonParser().parse(json.toString()).getAsJsonObject();
                    String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                    String version = tag.startsWith("v") ? tag.substring(1) : tag;
                    String installed = BuildConfig.VERSION_NAME == null ? "" : BuildConfig.VERSION_NAME;
                    String skipped = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .getString(KEY_SKIPPED, "");
                    if (isNewer(version, installed) && !version.equals(skipped)) {
                        String apk = null;
                        if (release.has("assets")) {
                            JsonArray assets = release.getAsJsonArray("assets");
                            for (int i = 0; i < assets.size(); i++) {
                                JsonObject asset = assets.get(i).getAsJsonObject();
                                String name = asset.get("name").getAsString().toLowerCase();
                                if (name.equals("franyulauncher-1.3.apk") ||
                                        (name.endsWith(".apk") && !name.contains("noruntime"))) {
                                    apk = asset.get("browser_download_url").getAsString();
                                    if (name.equals("franyulauncher-1.3.apk")) break;
                                }
                            }
                        }
                        if (apk != null) {
                            update = new Update(version,
                                    release.has("body") ? release.get("body").getAsString() : "", apk);
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            Update result = update;
            if (!closed) main.post(() -> {
                if (!closed && callback != null) callback.onResult(result);
            });
        });
    }

    private static boolean isNewer(String remote, String installed) {
        int[] r = numbers(remote);
        int[] i = numbers(installed);
        for (int n = 0; n < Math.max(r.length, i.length); n++) {
            int rv = n < r.length ? r[n] : 0;
            int iv = n < i.length ? i[n] : 0;
            if (rv != iv) return rv > iv;
        }
        return false;
    }

    private static int[] numbers(String value) {
        String clean = value == null ? "" : value.replaceAll("[^0-9.].*", "");
        if (clean.isEmpty()) return new int[]{0};
        String[] parts = clean.split("\\.");
        int[] result = new int[parts.length];
        for (int n = 0; n < parts.length; n++) {
            try {
                result[n] = Integer.parseInt(parts[n]);
            } catch (Exception e) {
                result[n] = 0;
            }
        }
        return result;
    }

    public void skipVersion(String version) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_SKIPPED, version == null ? "" : version).apply();
    }

    public void install(Update update, Activity activity) {
        if (update == null || activity == null || closed) return;
        executor.execute(() -> {
            File apk = new File(context.getCacheDir(), "franyulauncher-" + update.version + ".apk");
            try {
                if (!apk.isFile() || apk.length() < 1024) {
                    if (apk.exists() && !apk.delete()) {
                        throw new java.io.IOException("No se puede reemplazar la APK temporal");
                    }
                    download(update.apkUrl, apk);
                }
                validateApk(apk);
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_PENDING_APK, apk.getAbsolutePath()).apply();
                main.post(() -> launchInstaller(activity, apk));
            } catch (Exception e) {
                if (!closed) main.post(() -> android.widget.Toast.makeText(activity,
                        "La actualización no es válida o no se pudo descargar. Comprueba la conexión e inténtalo de nuevo.",
                        android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }

    private void download(String url, File destination) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "FranyuLauncher");
        connection.setInstanceFollowRedirects(true);
        int response = connection.getResponseCode();
        if (response != HttpURLConnection.HTTP_OK) {
            throw new java.io.IOException("HTTP " + response);
        }
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination, false)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            out.getFD().sync();
        } finally {
            connection.disconnect();
        }
    }

    private void validateApk(File apk) throws Exception {
        if (!apk.isFile() || !apk.canRead() || apk.length() < 1024) {
            throw new java.io.IOException("APK inexistente o incompleta");
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null || !context.getPackageName().equals(info.packageName)) {
            throw new java.io.IOException("La APK no pertenece a FranyuLauncher");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            if (info.signingInfo.hasMultipleSigners()) {
                if (info.signingInfo.getApkContentsSigners().length == 0) {
                    throw new java.io.IOException("Firma APK inválida");
                }
            } else if (info.signingInfo.getSigningCertificateHistory().length == 0) {
                throw new java.io.IOException("Firma APK inválida");
            }
        }
    }

    public void resumePendingInstall(Activity activity) {
        if (activity == null || closed) return;
        String path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PENDING_APK, "");
        if (path == null || path.isEmpty()) return;
        File apk = new File(path);
        try {
            validateApk(apk);
        } catch (Exception e) {
            clearPendingInstall();
            return;
        }
        main.post(() -> launchInstaller(activity, apk));
    }

    private void launchInstaller(Activity activity, File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            activity.startActivity(settings);
            return;
        }
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".updateprovider", apk);
        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            activity.startActivity(installIntent);
            clearPendingInstall();
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(viewIntent);
            clearPendingInstall();
        } catch (Exception e) {
            android.widget.Toast.makeText(activity,
                    "Android no encontró un instalador de APK disponible.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void clearPendingInstall() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_PENDING_APK).apply();
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
