package net.kdt.pojavlaunch.tasks;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;

import net.kdt.pojavlaunch.Tools;

import java.io.File;

public class AsyncMinecraftDownloader {
    public static String normalizeVersionId(String versionString) {
        JMinecraftVersionList versionList = (JMinecraftVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) {
            versionList = AsyncVersionList.getVersionListSync();
            if(versionList != null) {
                ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versionList);
            }
        }
        if(versionList == null || versionList.versions == null) return versionString;
        if(Instance.VERSION_LATEST_RELEASE.equals(versionString)) versionString = versionList.latest.get("release");
        if(Instance.VERSION_LATEST_SNAPSHOT.equals(versionString)) versionString = versionList.latest.get("snapshot");
        return versionString;
    }

    public static JMinecraftVersionList.Version getListedVersion(String normalizedVersionString) {
        if(normalizedVersionString == null) return null;
        JMinecraftVersionList versionList = (JMinecraftVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) {
            versionList = AsyncVersionList.getVersionListSync();
            if(versionList != null) {
                ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versionList);
            }
        }
        if(versionList != null && versionList.versions != null) {
            for(JMinecraftVersionList.Version version : versionList.versions) {
                if(version.id.equals(normalizedVersionString)) return version;
            }
        }
        // Fallback: check if the version exists locally on disk (installed OptiFine, Forge, Fabric, etc.)
        File localJson = new File(Tools.DIR_HOME_VERSION, normalizedVersionString + File.separator + normalizedVersionString + ".json");
        if(localJson.canRead()) {
            try {
                return Tools.GLOBAL_GSON.fromJson(Tools.read(localJson), JMinecraftVersionList.Version.class);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public interface DoneListener{
        void onDownloadDone();
        void onDownloadFailed(Throwable throwable);
    }
}
