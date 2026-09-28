package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import git.artdeell.mojo.BuildConfig;

public class UpdateChecker implements AutoCloseable {
    private static final String TAG = "UpdateChecker";
    private static final String RELEASES_URL =
            "https://api.github.com/repos/fdef54983-del/FRANYULAUNCHERnew/releases/latest";
    private static final String PREFS = "franyu_updates";
    private static final String KEY_SKIPPED = "skipped_version";
    private static final String KEY_PENDING_APK = "pending_apk";
    private static final String KEY_LAST_SEEN_RELEASE_UPDATED = "last_seen_release_updated";
    private static final String KEY_LAST_SEEN_ASSET_UPDATED = "last_seen_asset_updated";
    private static final String KEY_CURRENT_INSTALLED_BUILD = "current_installed_build";

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean closed;

    @FunctionalInterface
    public interface Callback {
        void onUpdate(Update update);
    }

    public interface DownloadProgressCallback {
        void onProgress(int percent, long currentBytes, long totalBytes);
        void onSuccess(File apkFile);
        void onError(Exception e);
    }

    public static class Update {
        public final String version;
        public final String apkUrl;
        public final String releaseUpdatedAt;
        public final String assetUpdatedAt;
        public final String body;
        public final long size;
        public final boolean isSameVersionPatch;

        public Update(String version, String apkUrl, String releaseUpdatedAt,
                      String assetUpdatedAt, String body, long size, boolean isSameVersionPatch) {
            this.version = version;
            this.apkUrl = apkUrl;
            this.releaseUpdatedAt = releaseUpdatedAt == null ? "" : releaseUpdatedAt;
            this.assetUpdatedAt = assetUpdatedAt == null ? "" : assetUpdatedAt;
            this.body = body;
            this.size = size;
            this.isSameVersionPatch = isSameVersionPatch;
        }

        public String getFingerprint() {
            return version + "#" + releaseUpdatedAt + "#" + assetUpdatedAt;
        }
    }

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
        syncInstalledBuildMetadata();
    }

    private void syncInstalledBuildMetadata() {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedInstalled = sp.getString(KEY_CURRENT_INSTALLED_BUILD, "");
        String currentInstalled = BuildConfig.VERSION_NAME == null ? "" : BuildConfig.VERSION_NAME;
        if (!currentInstalled.equals(savedInstalled)) {
            sp.edit()
                    .putString(KEY_CURRENT_INSTALLED_BUILD, currentInstalled)
                    .remove(KEY_LAST_SEEN_RELEASE_UPDATED)
                    .remove(KEY_LAST_SEEN_ASSET_UPDATED)
                    .remove(KEY_PENDING_APK)
                    .apply();
        }
    }

    public void check(Callback callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(RELEASES_URL).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                c.setRequestProperty("User-Agent", "FranyuLauncher/1.5");
                c.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int code = c.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new java.io.IOException("HTTP " + code + " fetching releases");
                }

                String json;
                try (InputStream in = c.getInputStream()) {
                    json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } finally {
                    c.disconnect();
                }

                if (closed) return;

                JsonObject release = JsonParser.parseString(json).getAsJsonObject();
                if (release.has("draft") && release.get("draft").getAsBoolean()) {
                    notifyUpToDate(callback);
                    return;
                }

                String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                String version = tag.startsWith("v") ? tag.substring(1) : tag;
                String releaseUpdatedAt = release.has("updated_at")
                        ? release.get("updated_at").getAsString()
                        : (release.has("published_at") ? release.get("published_at").getAsString() : "");

                String body = release.has("body") && !release.get("body").isJsonNull()
                        ? release.get("body").getAsString() : "";

                String installed = BuildConfig.VERSION_NAME == null ? "" : BuildConfig.VERSION_NAME;

                SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String skippedFingerprint = sp.getString(KEY_SKIPPED, "");
                String lastSeenReleaseUpdated = sp.getString(KEY_LAST_SEEN_RELEASE_UPDATED, "");
                String lastSeenAssetUpdated = sp.getString(KEY_LAST_SEEN_ASSET_UPDATED, "");

                JsonArray assets = release.has("assets") ? release.getAsJsonArray("assets") : null;
                String apkUrl = null;
                String assetUpdatedAt = "";
                long size = 0;
                if (assets != null) {
                    for (JsonElement e : assets) {
                        JsonObject a = e.getAsJsonObject();
                        String name = a.has("name") ? a.get("name").getAsString() : "";
                        if (name.endsWith(".apk") && !name.contains("noruntime")) {
                            apkUrl = a.get("browser_download_url").getAsString();
                            size = a.has("size") ? a.get("size").getAsLong() : 0;
                            if (a.has("updated_at") && !a.get("updated_at").isJsonNull()) {
                                assetUpdatedAt = a.get("updated_at").getAsString();
                            }
                            break;
                        }
                    }
                }

                if (apkUrl != null) {
                    boolean newerVersion = isNewer(version, installed);
                    boolean sameVersion = isSameOrEquivalentVersion(version, installed);

                    boolean assetModified = !assetUpdatedAt.isEmpty()
                            && !assetUpdatedAt.equals(lastSeenAssetUpdated);
                    boolean releaseModified = !releaseUpdatedAt.isEmpty()
                            && !releaseUpdatedAt.equals(lastSeenReleaseUpdated);

                    boolean contentChangedOnSameVersion = sameVersion && (assetModified || releaseModified);

                    if (newerVersion || contentChangedOnSameVersion) {
                        boolean isSameVersionPatch = sameVersion && !newerVersion;
                        Update update = new Update(version, apkUrl, releaseUpdatedAt,
                                assetUpdatedAt, body, size, isSameVersionPatch);

                        if (!update.getFingerprint().equals(skippedFingerprint)) {
                            notifyFound(callback, update);
                            return;
                        }
                    }
                }
                notifyUpToDate(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    private void notifyFound(Callback cb, Update u) {
        if (!closed && cb != null) main.post(() -> cb.onUpdate(u));
    }

    private void notifyUpToDate(Callback cb) {
        if (!closed && cb != null) main.post(() -> cb.onUpdate(null));
    }

    private void notifyError(Callback cb, Exception e) {
        if (!closed && cb != null) main.post(() -> cb.onUpdate(null));
    }

    public void skipUpdate(Update update) {
        if (update == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_SKIPPED, update.getFingerprint())
                .apply();
    }

    public void markUpdateObserved(Update update) {
        if (update == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_SEEN_RELEASE_UPDATED, update.releaseUpdatedAt)
                .putString(KEY_LAST_SEEN_ASSET_UPDATED, update.assetUpdatedAt)
                .apply();
    }

    public void install(Update update, Activity activity, DownloadProgressCallback progressCallback) {
        if (update == null || activity == null || closed) return;
        executor.execute(() -> {
            String safeVersion = update.version.replaceAll("[^a-zA-Z0-9._-]", "_");
            File apk = new File(context.getCacheDir(), "franyulauncher-" + safeVersion + ".apk");
            try {
                downloadWithProgress(update.apkUrl, apk, update.size, progressCallback);
                validateApk(apk);
                markUpdateObserved(update);
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_PENDING_APK, apk.getAbsolutePath())
                        .apply();
                main.post(() -> {
                    if (progressCallback != null) progressCallback.onSuccess(apk);
                    launchInstaller(activity, apk);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to download/install update", e);
                if (!closed) {
                    main.post(() -> {
                        if (progressCallback != null) progressCallback.onError(e);
                        Toast.makeText(activity,
                                "No se puede instalar la actualización: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void downloadWithProgress(String url, File destination, long expectedSize, DownloadProgressCallback callback) throws Exception {
        if (destination.exists() && destination.length() == expectedSize && expectedSize > 1024L) {
            if (callback != null) main.post(() -> callback.onProgress(100, expectedSize, expectedSize));
            return;
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "FranyuLauncher/1.5");
        connection.setInstanceFollowRedirects(true);
        int response = connection.getResponseCode();
        if (response != HttpURLConnection.HTTP_OK) throw new java.io.IOException("HTTP " + response);

        long totalBytes = connection.getContentLengthLong();
        if (totalBytes <= 0) totalBytes = expectedSize;

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination, false)) {
            byte[] buffer = new byte[32768];
            int n;
            long downloaded = 0;
            int lastPercent = -1;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                downloaded += n;
                if (totalBytes > 0 && callback != null) {
                    int percent = (int) ((downloaded * 100) / totalBytes);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        final long cur = downloaded;
                        final long tot = totalBytes;
                        main.post(() -> callback.onProgress(percent, cur, tot));
                    }
                }
            }
            out.getFD().sync();
        } finally {
            connection.disconnect();
        }
    }

    private void validateApk(File apk) throws Exception {
        if (!apk.isFile() || !apk.canRead() || apk.length() < 1024L) {
            throw new java.io.IOException("El archivo APK está incompleto o dañado.");
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null) {
            throw new java.io.IOException("El paquete APK descargado no es válido para este dispositivo.");
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

    public void launchInstaller(Activity activity, File apk) {
        if (apk == null || !apk.isFile()) {
            clearPendingInstall();
            return;
        }

        try {
            apk.setReadable(true, false);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(activity, "Permite a FranyuLauncher instalar aplicaciones para continuar.", Toast.LENGTH_LONG).show();
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            activity.startActivity(settings);
            return;
        }

        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".updateprovider", apk);
        } catch (Exception e) {
            Log.e(TAG, "FileProvider error, fallback to Uri.fromFile", e);
            uri = Uri.fromFile(apk);
        }

        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        viewIntent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        viewIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        viewIntent.setClipData(ClipData.newRawUri("FranyuLauncher APK", uri));

        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resInfoList != null) {
            for (ResolveInfo resolveInfo : resInfoList) {
                if (resolveInfo.activityInfo != null) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }

        try {
            activity.startActivity(viewIntent);
            clearPendingInstall();
            return;
        } catch (Exception e) {
            Log.e(TAG, "ACTION_VIEW failed, attempting ACTION_INSTALL_PACKAGE fallback", e);
        }

        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.setClipData(ClipData.newRawUri("FranyuLauncher APK", uri));
        try {
            activity.startActivity(installIntent);
            clearPendingInstall();
        } catch (Exception e2) {
            Log.e(TAG, "All installer intents failed", e2);
            Toast.makeText(activity, "Error al abrir el instalador de APK: " + e2.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isSameOrEquivalentVersion(String remote, String installed) {
        if (remote == null || installed == null) return false;
        int[] r = numbers(remote);
        int[] i = numbers(installed);
        if (r.length == 0 || i.length == 0) return remote.trim().equalsIgnoreCase(installed.trim());
        int len = Math.max(r.length, i.length);
        for (int k = 0; k < len; k++) {
            int a = k < r.length ? r[k] : 0;
            int b = k < i.length ? i[k] : 0;
            if (a != b) return false;
        }
        return true;
    }

    public static boolean isNewer(String remote, String installed) {
        if (remote == null || remote.trim().isEmpty()) return false;
        if (installed == null || installed.trim().isEmpty() || installed.startsWith("LOCAL-")) return true;
        int[] r = numbers(remote);
        int[] i = numbers(installed);
        int len = Math.max(r.length, i.length);
        for (int k = 0; k < len; k++) {
            int a = k < r.length ? r[k] : 0;
            int b = k < i.length ? i[k] : 0;
            if (a > b) return true;
            if (a < b) return false;
        }
        return false;
    }

    private static int[] numbers(String v) {
        String clean = v.replaceAll("[^0-9.]", " ").trim();
        if (clean.isEmpty()) return new int[0];
        String[] parts = clean.split("[. ]+");
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (String p : parts) {
            try {
                if (!p.isEmpty()) list.add(Integer.parseInt(p));
            } catch (NumberFormatException ignored) {}
        }
        int[] out = new int[list.size()];
        for (int k = 0; k < out.length; k++) out[k] = list.get(k);
        return out;
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
