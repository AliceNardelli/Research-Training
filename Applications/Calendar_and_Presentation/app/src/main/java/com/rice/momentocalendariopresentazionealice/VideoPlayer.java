package com.rice.momentocalendariopresentazionealice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;

public class VideoPlayer {
    private static final String TAG = "VideoPlayer";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private final Activity activity;
    private final String videoFilePath;

    public VideoPlayer(Activity activity, String videoFilePath) {
        this.activity = activity;
        this.videoFilePath = videoFilePath;
    }

    public void checkPermissionsAndPlayVideo() {
        Log.i(TAG, "Checking permissions for READ_EXTERNAL_STORAGE");
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission not granted. Requesting permission.");
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i(TAG, "Permission granted. Playing video.");
            playVideo(videoFilePath);
        }
    }

    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permissions granted by user. Playing video.");
            playVideo(videoFilePath);
        } else {
            Toast.makeText(activity, "Permission denied. Cannot play video.", Toast.LENGTH_LONG).show();
        }
    }

    private void playVideo(String filePath) {
        try {
            File videoFile = new File(filePath);
            if (videoFile.exists()) {
                Uri videoUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", videoFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/mp4");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(intent);
            } else {
                Log.e(TAG, "Video file not found.");
                Toast.makeText(activity, "Video file not found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while playing video: " + e.getMessage(), e);
            Toast.makeText(activity, "Error playing video", Toast.LENGTH_SHORT).show();
        }
    }
}

