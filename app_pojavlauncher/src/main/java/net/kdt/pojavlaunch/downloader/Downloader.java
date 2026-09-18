package net.kdt.pojavlaunch.downloader;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.tasks.SpeedCalculator;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import git.artdeell.mojo.R;

public class Downloader {
    private static final double ONE_MEGABYTE = (1024d * 1024d);
    private static final int MAX_DOWNLOAD_THREADS = 8;
    private static final ThreadLocal<byte[]> sThreadLocalBuffer = new ThreadLocal<>();
    private final String mProgressKey;
    private final AtomicReference<IOException> mThreadException = new AtomicReference<>();
    private final AtomicInteger mDownloadedFileCounter = new AtomicInteger();
    private final AtomicLong mDownloadedSizeCounter = new AtomicLong();
    private final AtomicLong mInternetUsageCounter = new AtomicLong();
    private final AtomicBoolean mUseSizeProgress = new AtomicBoolean(true);
    private final SpeedCalculator mSpeedCalculator = new SpeedCalculator();
    private ExecutorService mDownloadService;
    private ExecutorService mVerifyService;

    public Downloader(String mProgressKey) {
        this.mProgressKey = mProgressKey;
    }

    protected void runDownloads(ArrayList<? extends TaskMetadata> downloads) throws IOException, InterruptedException {
        insertMetadata(downloads);
        performDownloads(downloads);
    }

    private void performDownloads(ArrayList<? extends TaskMetadata> metadata) throws IOException, InterruptedException {
        mThreadException.set(null);
        mDownloadedFileCounter.set(0);
        mDownloadedSizeCounter.set(0);
        waitForNetwork();
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int downloadThreads = Math.max(3, Math.min(MAX_DOWNLOAD_THREADS, processors * 2));
        mDownloadService = Executors.newFixedThreadPool(downloadThreads);
        mVerifyService = Executors.newFixedThreadPool(Math.max(1, processors));
        long totalSize = 0;
        int totalCount = metadata.size();
        boolean sizeCounter = mUseSizeProgress.get();
        for(TaskMetadata element : metadata) {
            totalSize += Math.max(0, element.size);
            mVerifyService.submit(new CheckFileOnDiskTask(element, this));
        }
        double totalMegabytes = totalSize / ONE_MEGABYTE;
        while(mDownloadedFileCounter.get() < totalCount) {
            IOException exception = mThreadException.get();
            if(exception != null) throw exception;
            if(sizeCounter) reportSizeProgress(totalMegabytes);
            else reportCountProgress(R.string.newerdl_downloading_files_count, totalCount);
            Thread.sleep(33);
        }
        mDownloadService.shutdown();
        mVerifyService.shutdown();
        if(!mDownloadService.awaitTermination(30, TimeUnit.SECONDS) || !mVerifyService.awaitTermination(30, TimeUnit.SECONDS)) {
            throw new IOException("Timed out while finishing downloads");
        }
    }

    private void insertMetadata(ArrayList<? extends TaskMetadata> metadata) throws IOException, InterruptedException {
        mThreadException.set(null);
        ArrayList<TaskMetadata> reducedList = new ArrayList<>();
        for(TaskMetadata element : metadata) {
            if(CompleteMetadataTask.shouldCompleteMetadata(element)) reducedList.add(element);
        }
        if(reducedList.isEmpty()) return;
        try (ExecutorService executorService = Executors.newFixedThreadPool(Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors())))) {
            for(TaskMetadata element : reducedList) executorService.submit(new CompleteMetadataTask(element, this));
            executorService.shutdown();
            while(!executorService.awaitTermination(33, TimeUnit.MILLISECONDS)) {
                IOException exception = mThreadException.get();
                if(exception != null) throw exception;
                reportCountProgress(R.string.newerdl_inserting_metadata_count, reducedList.size());
            }
        }
    }

    private double getSpeed() { return mSpeedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE; }

    private void reportCountProgress(int resource, int total) {
        int downloadedCount = mDownloadedFileCounter.get();
        int progress = total <= 0 ? 100 : (int)((downloadedCount / (float)total) * 100f);
        ProgressLayout.setProgress(mProgressKey, progress, resource, downloadedCount, total, getSpeed());
    }

    private void reportSizeProgress(double totalMegabytes) {
        double downloadedMegabytes = mDownloadedSizeCounter.get() / ONE_MEGABYTE;
        int progress = totalMegabytes <= 0 ? 100 : (int)(downloadedMegabytes / totalMegabytes * 100d);
        ProgressLayout.setProgress(mProgressKey, progress, R.string.newerdl_downloading_files_size, downloadedMegabytes, totalMegabytes, getSpeed());
    }

    protected void taskException(IOException e) { mThreadException.compareAndSet(null, e); }
    protected void disableSizeCounter() { mUseSizeProgress.lazySet(false); }
    protected void submitFileForDownload(TaskMetadata taskMetadata) { mDownloadService.submit(new DownloadFileTask(taskMetadata, this)); }
    protected void submitFileForRecheck(TaskMetadata taskMetadata) { mVerifyService.submit(new CheckFileOnDiskTask(taskMetadata, this, true)); }
    protected void fileComplete() { mDownloadedFileCounter.incrementAndGet(); }
    protected void addSize(long bytes) { mDownloadedSizeCounter.addAndGet(bytes); }

    protected boolean isNetworkAvailable() {
        Application application = ContextExecutor.getApplication();
        if(application == null) return true;
        ConnectivityManager manager = (ConnectivityManager)application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(manager == null) return true;
        Network network = manager.getActiveNetwork();
        if(network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    protected void waitForNetwork() throws InterruptedException {
        while(!isNetworkAvailable()) {
            ProgressLayout.setProgress(mProgressKey, 0, R.string.dl_no_internet);
            Thread.sleep(2000);
        }
    }

    protected void waitForNetworkRecovery() throws InterruptedException {
        while(!isNetworkAvailable()) {
            ProgressLayout.setProgress(mProgressKey, 0, R.string.dl_paused_offline);
            Thread.sleep(2000);
        }
    }

    protected void sleepBackoff(int attempt) throws InterruptedException {
        long delay = 1000L << Math.min(4, Math.max(0, attempt));
        Thread.sleep(delay);
    }

    private void copy(InputStream inputStream, OutputStream outputStream, BytesCopiedListener listener) throws IOException {
        byte[] buffer = getBuffer();
        int readLen;
        while((readLen = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readLen);
            if(listener != null) listener.onBytesCopied(readLen);
            mInternetUsageCounter.addAndGet(readLen);
        }
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        return connection;
    }

    protected void downloadToStream(HttpURLConnection connection, OutputStream outputStream, BytesCopiedListener listener) throws IOException {
        try(InputStream inputStream = connection.getInputStream()) { copy(inputStream, outputStream, listener); }
    }

    protected String downloadString(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(32, connection.getContentLength()))) {
            downloadToStream(connection, outputStream, null);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } finally { connection.disconnect(); }
    }

    protected void downloadFile(File file, URL url, BytesCopiedListener listener) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try(FileOutputStream outputStream = new FileOutputStream(file)) {
            downloadToStream(connection, outputStream, listener);
        } finally { connection.disconnect(); }
    }

    protected boolean tryContinueDownload(File file, long wantedLength, URL url, BytesCopiedListener listener) throws IOException {
        long existing = file.length();
        if(existing <= 0 || (wantedLength > 0 && existing >= wantedLength)) return false;
        HttpURLConnection connection = openConnection(url);
        connection.setRequestProperty("Range", String.format(Locale.ENGLISH, "bytes=%d-", existing));
        try {
            connection.connect();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) return false;
            try(FileOutputStream outputStream = new FileOutputStream(file, true)) {
                downloadToStream(connection, outputStream, listener);
                return wantedLength <= 0 || file.length() == wantedLength;
            }
        } finally { connection.disconnect(); }
    }

    public static byte[] getBuffer() {
        byte[] buffer = sThreadLocalBuffer.get();
        if(buffer == null) { buffer = new byte[8192]; sThreadLocalBuffer.set(buffer); }
        return buffer;
    }
}
