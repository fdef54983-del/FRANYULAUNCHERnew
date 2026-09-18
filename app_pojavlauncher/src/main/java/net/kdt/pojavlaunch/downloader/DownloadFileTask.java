package net.kdt.pojavlaunch.downloader;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadFileTask extends DownloaderTask implements BytesCopiedListener {
    private static final int MAX_ATTEMPTS = 6;
    private final AtomicLong mBytesDownloaded = new AtomicLong();

    DownloadFileTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        super(mMetadata, mHostDownloader);
    }

    @Override
    protected void performTask() throws IOException {
        int attempt = 0;
        while(attempt < MAX_ATTEMPTS) {
            try {
                mDownloader.waitForNetworkRecovery();
                boolean resumed = false;
                if(mMetadata.path.exists() && mMetadata.path.length() > 0
                        && (mMetadata.size <= 0 || mMetadata.path.length() < mMetadata.size)) {
                    long alreadyDownloaded = mMetadata.path.length();
                    mBytesDownloaded.set(alreadyDownloaded);
                    mDownloader.addSize(alreadyDownloaded);
                    resumed = mDownloader.tryContinueDownload(mMetadata.path, mMetadata.size, mMetadata.url, this);
                    if(!resumed) {
                        mDownloader.addSize(-alreadyDownloaded);
                        mBytesDownloaded.set(0);
                    }
                }
                if(!resumed) {
                    mBytesDownloaded.set(0);
                    mDownloader.downloadFile(mMetadata.path, mMetadata.url, this);
                }
                mDownloader.submitFileForRecheck(mMetadata);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Download interrupted for " + mMetadata.url, e);
            } catch (IOException e) {
                attempt++;
                if(attempt >= MAX_ATTEMPTS) throw e;
                try {
                    mDownloader.waitForNetworkRecovery();
                    mDownloader.sleepBackoff(attempt - 1);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted while retrying " + mMetadata.url, interruptedException);
                }
            }
        }
    }

    @Override
    public void onBytesCopied(int nbytes) {
        mBytesDownloaded.getAndAdd(nbytes);
        mDownloader.addSize(nbytes);
    }
}
