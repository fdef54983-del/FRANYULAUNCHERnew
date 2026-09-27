package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptiFineDownloadTask implements AsyncMinecraftDownloader.DoneListener {
    private static final Pattern sMcVersionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?");
    private final OptiFineUtils.OptiFineVersion mOptiFineVersion;
    private final Object mMinecraftDownloadLock = new Object();
    private Throwable mDownloaderThrowable;

    public OptiFineDownloadTask(OptiFineUtils.OptiFineVersion mOptiFineVersion) {
        this.mOptiFineVersion = mOptiFineVersion;
    }

    public void prepareForInstall() throws Exception {
        String minecraftVersion = determineMinecraftVersion();
        if(minecraftVersion == null) return;
        java.io.File clientJar = new java.io.File(net.kdt.pojavlaunch.Tools.DIR_HOME_VERSION, minecraftVersion + java.io.File.separator + minecraftVersion + ".jar");
        java.io.File clientJson = new java.io.File(net.kdt.pojavlaunch.Tools.DIR_HOME_VERSION, minecraftVersion + java.io.File.separator + minecraftVersion + ".json");
        if(clientJar.exists() && clientJar.length() > 0 && clientJson.exists() && clientJson.length() > 0) {
            return;
        }
        if(!downloadMinecraft(minecraftVersion)) {
            if(mDownloaderThrowable instanceof Exception) {
                throw (Exception) mDownloaderThrowable;
            }else if(mDownloaderThrowable != null) {
                throw new Exception(mDownloaderThrowable);
            }else {
                throw new java.io.IOException("No se pudo descargar la versión base de Minecraft " + minecraftVersion);
            }
        }
    }

    public String determineMinecraftVersion() {
        Matcher matcher = sMcVersionPattern.matcher(mOptiFineVersion.minecraftVersion);
        if(matcher.find()) {
            StringBuilder mcVersionBuilder = new StringBuilder();
            mcVersionBuilder.append(matcher.group(1));
            mcVersionBuilder.append('.');
            mcVersionBuilder.append(matcher.group(2));
            String thirdGroup = matcher.group(3);
            if(thirdGroup != null && !thirdGroup.isEmpty() && !"0".equals(thirdGroup)) {
                mcVersionBuilder.append('.');
                mcVersionBuilder.append(thirdGroup);
            }
            return mcVersionBuilder.toString();
        }else{
            return null;
        }
    }

    public boolean downloadMinecraft(String minecraftVersion) {
        // the string is always normalized
        JMinecraftVersionList.Version minecraftJsonVersion = AsyncMinecraftDownloader.getListedVersion(minecraftVersion);
        if(minecraftJsonVersion == null) {
            mDownloaderThrowable = new java.io.IOException("No se encontró la versión " + minecraftVersion + " en la lista de versiones oficiales.");
            return false;
        }
        try {
            synchronized (mMinecraftDownloadLock) {
                new MinecraftDownloader().start(null, minecraftJsonVersion, minecraftVersion, this);
                mMinecraftDownloadLock.wait();
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
            mDownloaderThrowable = e;
            return false;
        }
        return mDownloaderThrowable == null;
    }

    @Override
    public void onDownloadDone() {
        synchronized (mMinecraftDownloadLock) {
            mDownloaderThrowable = null;
            mMinecraftDownloadLock.notifyAll();
        }
    }

    @Override
    public void onDownloadFailed(Throwable throwable) {
        synchronized (mMinecraftDownloadLock) {
            mDownloaderThrowable = throwable;
            mMinecraftDownloadLock.notifyAll();
        }
    }
}
