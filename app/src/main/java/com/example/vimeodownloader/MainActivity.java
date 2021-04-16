package com.example.vimeodownloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import vimeo.Extractor;
import vimeo.OnExtrationListener;
import vimeo.VideoDownloader;

public class MainActivity extends AppCompatActivity implements DownloadVideoService.OnProgressUpdateListener {
    private Button btnLock, btnPlay;
    private EditText txtURL;
    private VideoView videoView;
    private ProgressBar progressBar;

    private String videoName;

    private DownloadVideoService downloaderService;
    public final static String FILE_EXTENSION = ".abc";

    private final String LAST_VIDEO = "LAST";
    private final String VIDEO_ID = "ID";

    private final String PREFIX = "bemaapp";
    private final int UUID_LENGTH = 36;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = getSharedPreferences(LAST_VIDEO, MODE_PRIVATE);

        videoName = sharedPreferences.getString(VIDEO_ID, null);

        txtURL = findViewById(R.id.txtURL);
        videoView = findViewById(R.id.videoViewer);
        progressBar = findViewById(R.id.progressBar);

        Intent intent = new Intent(getApplicationContext(), DownloadVideoService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        DownloadVideoService.setOnProgressChangedListener(this);


        findViewById(R.id.btnDownload).setOnClickListener(v -> {
            Extractor.getInstance().fetchVideoWithURL(txtURL.getText().toString(), null, new OnExtrationListener() {
                @Override
                public void onSuccess(VideoDownloader video) {
                    String hdStream = video.getStreams().get("720p");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), video.getTitle(), Toast.LENGTH_LONG).show();
                        }
                    });

                    videoName = video.getTitle();
                    downloaderService.downloadVideo(hdStream, videoName);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        });

        btnLock = findViewById(R.id.btnLock);

        btnLock.setOnClickListener(v -> {
            videoView.stopPlayback();
            videoView.setVisibility(View.INVISIBLE);
            lockOrPlay(0);
        });

        btnPlay = findViewById(R.id.btnPlay);

        btnPlay.setOnClickListener(v -> {
            String filePath = getFilesDir() + File.separator + videoName + FILE_EXTENSION;
            lockOrPlay(1);
            Uri videoUri = Uri.parse(filePath);

            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setVolume(100f, 100f);
                        }
                    }
            );
            videoView.start();

            videoView.setOnCompletionListener(mp -> {
                videoView.setVisibility(View.INVISIBLE);
            });
        });


        if(videoName == null){
            btnLock.setVisibility(View.INVISIBLE);
            btnPlay.setVisibility(View.INVISIBLE);
            videoView.setVisibility(View.INVISIBLE);
        }
    }

    private void lockOrPlay(int mode){
        try {
            RandomAccessFile videoFile = new RandomAccessFile(new File(getFilesDir(), videoName + FILE_EXTENSION), "rw");
            StringBuilder prefixString = new StringBuilder();

            int ch = videoFile.read();

            while (ch != -1 && prefixString.length() < PREFIX.length()) {
                prefixString.append((char) ch);
                ch = videoFile.read();
            }
            if(!prefixString.toString().equals(PREFIX) && mode == 0){
                moveAndSaveFile(videoFile, mode);
            }else if(prefixString.toString().equals(PREFIX) && mode == 1){
                moveAndSaveFile(videoFile, mode);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void moveAndSaveFile(RandomAccessFile originalVideoFile, int mode){
        try {
            File originalFile = new File(getFilesDir(), videoName + FILE_EXTENSION);
            File tempFile = File.createTempFile("temp", FILE_EXTENSION, originalFile.getParentFile());

            RandomAccessFile tempVideoFile = new RandomAccessFile(tempFile, "rw");

            if(mode == 0) {
                UUID uuid = UUID.randomUUID();
                String key = PREFIX + uuid.toString();
                tempVideoFile.write(key.getBytes());
                originalVideoFile.seek(0);
            }
            else {
                originalVideoFile.seek(PREFIX.length() + UUID_LENGTH);
            }

            byte[] data = new byte[4096];
            int count;

            while ((count = originalVideoFile.read(data)) != -1) {
                tempVideoFile.write(data, 0, count);
            }

            tempVideoFile.close();
            originalVideoFile.close();

            originalFile.delete();
            tempFile.renameTo(originalFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadVideoService.MyBinder binder = (DownloadVideoService.MyBinder) service;
            downloaderService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onDestroy() {
        if(videoName != null) {
            SharedPreferences sharedPreferences = getSharedPreferences(LAST_VIDEO, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(VIDEO_ID, videoName);
            editor.commit();
        }
        super.onDestroy();
    }

    @Override
    public void onProgressStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                btnLock.setVisibility(View.INVISIBLE);
                btnPlay.setVisibility(View.INVISIBLE);
                videoView.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onProgressUpdate(int progress) {
        progressBar.setProgress(progress);
    }

    @Override
    public void onDownloadComplete() {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
                btnLock.setVisibility(View.VISIBLE);
                btnPlay.setVisibility(View.VISIBLE);
            }
        });
    }
}