package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private static final long CHECK_INTERVAL = 6L * 60L * 60L * 1000L;
    private static final String RELEASES_URL =
            "https://api.github.com/repos/fdef54983-del/FRANYULAUNCHERnew/releases/latest";
    private static final String PREFS = "franyu_updates";
    private static final String KEY_CHECK = "last_check";
    private static final String KEY_SKIPPED = "skipped_version";
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

    public interface Callback { void onResult(Update update); }

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void check(Callback callback) {
        long last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_CHECK, 0);
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
                        while ((n = in.read(buffer)) != -1) json.append(new String(buffer, 0, n, "UTF-8"));
                    }
                    JsonObject release = new JsonParser().parse(json.toString()).getAsJsonObject();
                    String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                    String version = tag.startsWith("v") ? tag.substring(1) : tag;
                    String installed = BuildConfig.VERSION_NAME == null ? "" : BuildConfig.VERSION_NAME;
                    if (isNewer(version, installed) &&
                            !version.equals(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                    .getString(KEY_SKIPPED, ""))) {
                        String apk = null;
                        if (release.has("assets")) {
                            JsonArray assets = release.getAsJsonArray("assets");
                            for (int i = 0; i < assets.size(); i++) {
                                JsonObject a = assets.get(i).getAsJsonObject();
                                String name = a.get("name").getAsString().toLowerCase();
                                if (name.endsWith(".apk") && !name.contains("noruntime")) {
                                    apk = a.get("browser_download_url").getAsString();
                                    break;
                                }
                            }
                        }
                        if (apk != null) update = new Update(version,
                                release.has("body") ? release.get("body").getAsString() : "", apk);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            Update result = update;
            if (!closed) main.post(() -> { if (!closed) callback.onResult(result); });
        });
    }

    private static boolean isNewer(String remote, String installed) {
        int[] r = numbers(remote), i = numbers(installed);
        for (int n = 0; n < Math.max(r.length, i.length); n++) {
            int rv = n < r.length ? r[n] : 0;
            int iv = n < i.length ? i[n] : 0;
            if (rv != iv) return rv > iv;
        }
        return !remote.equals(installed) && !remote.isEmpty();
    }

    private static int[] numbers(String value) {
        String clean = value == null ? "" : value.replaceAll("[^0-9.].*", "");
        if (clean.isEmpty()) return new int[]{0};
        String[] parts = clean.split("\\.");
        int[] result = new int[parts.length];
        for (int n = 0; n < parts.length; n++) {
            try { result[n] = Integer.parseInt(parts[n]); } catch (Exception e) { result[n] = 0; }
        }
        return result;
    }

    public void skipLatest() {
        // The card supplies the current version through the view; callers use skipVersion below.
    }

    public void skipVersion(String version) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SKIPPED, version).apply();
    }

    public void installLatest(Activity activity) {
        executor.execute(() -> {
            File apk = new File(context.getCacheDir(), "franyulauncher-update.apk");
            // The URL is obtained by check() and held only in the callback in the current UI.
            // This method is replaced by install(Update, Activity) for the concrete release.
        });
    }

    public void install(Update update, Activity activity) {
        executor.execute(() -> {
            try {
                File apk = new File(context.getCacheDir(), "franyulauncher-" + update.version + ".apk");
                HttpURLConnection c = (HttpURLConnection) new URL(update.apkUrl).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(60000);
                c.setRequestProperty("User-Agent", "FranyuLauncher");
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                } finally { c.disconnect(); }
                Uri uri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".updateprovider", apk);
                Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !activity.getPackageManager().canRequestPackageInstalls()) {
                    intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + context.getPackageName()));
                }
                Intent finalIntent = intent;
                main.post(() -> activity.startActivity(finalIntent));
            } catch (Exception ignored) {
            }
        });
    }

    @Override public void close() {
        closed = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
