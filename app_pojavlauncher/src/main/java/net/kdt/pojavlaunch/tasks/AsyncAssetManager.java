package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.Architecture.archAsString;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncAssetManager {

    private static volatile Future<?> sComponentsFuture = null;
    private static volatile Future<?> sSingleFilesFuture = null;
    private static final Object sUnpackLock = new Object();

    private AsyncAssetManager(){}

    /**
     * Retrieves the Future tracking the background components unpacking process.
     * @return Future or null if not yet initiated
     */
    public static Future<?> getComponentsUnpackFuture() {
        return sComponentsFuture;
    }

    /**
     * Retrieves the Future tracking the single files unpacking process.
     * @return Future or null if not yet initiated
     */
    public static Future<?> getSingleFilesUnpackFuture() {
        return sSingleFilesFuture;
    }

    /**
     * Waits for component unpacking to finish with the specified timeout.
     */
    public static void waitForComponentsUnpack(long timeout, TimeUnit unit) {
        Future<?> future = sComponentsFuture;
        if (future != null) {
            try {
                future.get(timeout, unit);
            } catch (Exception e) {
                Log.e("AsyncAssetManager", "Timeout or error waiting for components unpack", e);
            }
        }
    }

    /**
     * Waits for single files unpacking to finish with the specified timeout.
     */
    public static void waitForSingleFilesUnpack(long timeout, TimeUnit unit) {
        Future<?> future = sSingleFilesFuture;
        if (future != null) {
            try {
                future.get(timeout, unit);
            } catch (Exception e) {
                Log.e("AsyncAssetManager", "Timeout or error waiting for single files unpack", e);
            }
        }
    }

    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    public static void unpackRuntime(AssetManager am) {
        /* Check if JRE is included */
        String rt_version = null;
        String current_rt_version = MultiRTUtils.readInternalRuntimeVersion("Internal");
        try {
            rt_version = Tools.read(am.open("components/jre/version"));
        } catch (IOException e) {
            Log.e("JREAuto", "JRE was not included on this APK.", e);
        }
        String exactJREName = MultiRTUtils.getExactJreName(8);
        if(current_rt_version == null && exactJREName != null && !exactJREName.equals("Internal")/*this clause is for when the internal runtime is goofed*/) return;
        if(rt_version == null) return;
        if(rt_version.equals(current_rt_version)) return;

        // Install the runtime in an async manner, hope for the best
        String finalRt_version = rt_version;
        sExecutorService.execute(() -> {

            try {
                MultiRTUtils.installRuntimeNamedBinpack(
                        am.open("components/jre/universal.tar.xz"),
                        am.open("components/jre/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                        "Internal", finalRt_version);
                MultiRTUtils.postPrepare("Internal");
            }catch (IOException e) {
                Log.e("JREAuto", "Internal JRE unpack failed", e);
            }
        });
    }

    /** Unpack single files, with no regard to version tracking */
    public static Future<?> unpackSingleFiles(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
        Future<?> future = sExecutorService.submit(() -> {
            try {
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);
                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx,"resolv.conf",Tools.DIR_DATA, false);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !", e);
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
        });
        sSingleFilesFuture = future;
        return future;
    }

    public static Future<?> unpackComponents(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
        Future<?> future = sExecutorService.submit(() -> {
            tryUnpackComponent(ctx, "caciocavallo", false);
            tryUnpackComponent(ctx, "caciocavallo17", false);
            tryUnpackComponent(ctx, "lwjgl3", false);

            tryUnpackComponent(ctx, "security", true);
            tryUnpackComponent(ctx, "arc_dns_injector", true);
            tryUnpackComponent(ctx, "forge_installer", true);
            tryUnpackComponent(ctx, "authlib-injector", true);
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
        });
        sComponentsFuture = future;
        return future;
    }

    private static String readInstalledComponentVersion(File componentRoot) {
        File localVersionFile = new File(componentRoot, "version");
        try(FileInputStream fileInputStream = new FileInputStream(localVersionFile)) {
            return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static String readBuiltinComponentVersion(AssetManager assetManager, String componentName) {
        String componentVersionLocation = "components/"+componentName+"/version";
        try (InputStream inputStream = assetManager.open(componentVersionLocation)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static void tryUnpackComponent(Context ctx, String component, boolean privateDirectory) {
        try {
            unpackComponent(ctx, component, privateDirectory);
        }catch (IOException e) {
            Log.e("AssetUnpacker", "Failed to unpack component "+component, e);
        }
    }

    public static void forceUnpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        unpackComponentInternal(ctx, component, privateDirectory, true);
    }

    private static void unpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        unpackComponentInternal(ctx, component, privateDirectory, false);
    }

    private static void unpackComponentInternal(Context ctx, String component, boolean privateDirectory, boolean force) throws IOException {
        synchronized (sUnpackLock) {
            AssetManager am = ctx.getAssets();
            String rootDir = privateDirectory ? Tools.DIR_DATA : Tools.DIR_GAME_HOME;
            File componentTarget = new File(rootDir, component);
            String installedVersion = readInstalledComponentVersion(componentTarget);
            String builtinVersion = readBuiltinComponentVersion(am, component);
            if(!force && installedVersion != null && installedVersion.equals(builtinVersion)) {
                Log.i("AssetUnpacker", "Component "+component+" is up-to-date, continuing...");
                return;
            }
            Log.i("AssetUnpacker", (force ? "Force updating " : "Updating ") + component);

            // Extract to temporary directory first to prevent race conditions and partial reads
            File tmpTarget = new File(rootDir, component + "_tmp_" + System.currentTimeMillis());
            if (tmpTarget.exists()) {
                FileUtils.deleteQuietly(tmpTarget);
            }
            if (!tmpTarget.mkdirs()) {
                throw new IOException("Failed to create temporary directory for " + component + " at " + tmpTarget.getAbsolutePath());
            }

            try {
                String componentSource = "components/" + component;
                String[] fileList = am.list(componentSource);
                if (fileList != null) {
                    for (String fileName : fileList) {
                        if(fileName.equals("version")) continue;
                        String sourcePath = componentSource + "/" + fileName;
                        Tools.copyAssetFile(ctx, sourcePath, tmpTarget.getAbsolutePath(), true);
                    }
                }

                // Write the version file into the temp directory before atomic swap
                if (builtinVersion != null) {
                    Tools.write(new File(tmpTarget, "version").getAbsolutePath(), builtinVersion);
                }

                // Atomic replacement: rename existing componentTarget to backup, then rename tmpTarget to componentTarget
                File oldTarget = new File(rootDir, component + "_old_" + System.currentTimeMillis());
                boolean renamedExisting = false;
                if (componentTarget.exists()) {
                    renamedExisting = componentTarget.renameTo(oldTarget);
                    if (!renamedExisting) {
                        // Fallback if rename of existing directory failed: delete componentTarget
                        FileUtils.deleteDirectory(componentTarget);
                    }
                }

                boolean moved = tmpTarget.renameTo(componentTarget);
                if (!moved) {
                    // Fallback if rename failed across filesystems: copy directory
                    FileUtils.copyDirectory(tmpTarget, componentTarget);
                    FileUtils.deleteQuietly(tmpTarget);
                }

                if (renamedExisting && oldTarget.exists()) {
                    FileUtils.deleteQuietly(oldTarget);
                }
            } catch (Exception e) {
                // If any error occurred during extraction, delete temp directory
                FileUtils.deleteQuietly(tmpTarget);
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException("Failed to unpack component " + component, e);
                }
            }
        }
    }

    public static void extractDefaultSettings(Context context, File gamedir)  {
        try {
            String gameDirPath = gamedir.getAbsolutePath();
            Tools.copyAssetFile(context, "options.txt", gameDirPath, false);
        }catch (IOException e) {
            Tools.showError(context, e);
        }
    }
}
