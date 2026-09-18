package net.kdt.pojavlaunch.downloader;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.HashUtils;

import java.io.File;
import java.io.IOException;

public class CheckFileOnDiskTask extends DownloaderTask {
    private final boolean mAfterDownload;

    CheckFileOnDiskTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        super(mMetadata, mHostDownloader);
        this.mAfterDownload = false;
    }

    CheckFileOnDiskTask(TaskMetadata mMetadata, Downloader mHostDownloader, boolean mAfterDownload) {
        super(mMetadata, mHostDownloader);
        this.mAfterDownload = mAfterDownload;
    }

    @Override
    protected void performTask() throws IOException {
        boolean checkResult = checkFile();
        if(checkResult) {
            if(!mAfterDownload) mDownloader.addSize(Math.max(0, mMetadata.size));
            mDownloader.fileComplete();
        } else {
            if(!mAfterDownload) mDownloader.submitFileForDownload(mMetadata);
            else throw new IOException("Failed to verify " + mMetadata.toString());
        }
    }

    private boolean checkFile() throws IOException {
        File localFile = mMetadata.path;
        if(!localFile.exists() || !localFile.isFile()) return false;
        if(mMetadata.size != -1 && mMetadata.size != localFile.length()) return false;

        // A manifest SHA-1 is authoritative and is always checked, even when
        // the optional user verification preference is disabled.
        if(mMetadata.sha1Hash != null && !mMetadata.sha1Hash.isEmpty()) {
            return HashUtils.compareSHA1(localFile, mMetadata.sha1Hash);
        }

        return LauncherPreferences.PREF_VERIFY_FILES || mMetadata.size == -1;
    }
}
