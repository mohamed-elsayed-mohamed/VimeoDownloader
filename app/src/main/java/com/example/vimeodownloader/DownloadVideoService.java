package com.example.vimeodownloader;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.SyncStateContract;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;

public class DownloadVideoService extends Service {
    private final IBinder binder;

    public DownloadVideoService() {
        binder = new MyBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void downloadVideo(String videoURL, String videoName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream input = null;
                OutputStream output = null;
                HttpsURLConnection connection = null;

                try {
                    progressListener.onProgressStart();
                    URL url = new URL(videoURL);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.connect();

                    int fileLength = connection.getContentLength();
                    input = connection.getInputStream();
                    output = openFileOutput(videoName + MainActivity.FILE_EXTENSION, MODE_PRIVATE);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            int progressValue = (int) (total * 100 / fileLength);
                            progressListener.onProgressUpdate(progressValue);
                        }

                        output.write(data, 0, count);
                    }
                    progressListener.onDownloadComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();

                    stopSelf();
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DownloadVideoService.setOnProgressChangedListener(null);
    }

    private static OnProgressUpdateListener progressListener;
    public static void setOnProgressChangedListener(OnProgressUpdateListener _listener) {
        progressListener = _listener;
    }

    public class MyBinder extends Binder {
        DownloadVideoService getService() {
            return DownloadVideoService.this;
        }
    }

    public interface OnProgressUpdateListener {
        void onProgressStart();
        void onProgressUpdate(int progress);
        void onDownloadComplete();
    }
}