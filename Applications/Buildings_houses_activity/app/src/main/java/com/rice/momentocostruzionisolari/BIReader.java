package com.rice.momentocostruzionisolari;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.companion.Task;
import com.bfr.buddysdk.services.companion.TaskCallback;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.File;

public class BIReader {

    private static final String TAG = "BIReader";
    private Context context;
    private ImageView imageView;
    private VideoView videoView;
    private PlayerView exoView; // Correctly cast ExoPlayer's PlayerView

    public BIReader(Context context, ImageView imageView, VideoView videoView, PlayerView exoView) {
        this.context = context;
        this.imageView = imageView;
        this.videoView = videoView;
        this.exoView = exoView; // Expect PlayerView here instead of a generic Object
    }
    public void readBI(String biName){
        String videoPath = "/storage/emulated/0/Download/.mp4"; // Path to your video file
        Uri uri = Uri.parse(videoPath);

        // Set the video URI to the VideoView
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Get duration of the video
                int duration = videoView.getDuration();

                // Seek to 1 minute (60,000 milliseconds)
                if (duration > 20000) {
                    videoView.seekTo(00000);
                }

                // Start playback
                videoView.start();

                // Stop playback after 1 minute (60,000 milliseconds)
                videoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        videoView.pause();

                        videoView.setVisibility(View.GONE);
                        BuddySDK.USB.stopAllLed(new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
                                Log.i("coucou", "Message received : "+ s);

                            }

                            @Override
                            public void onFailed(String s) throws RemoteException {

                            }
                        });

                    }
                }, 32000);
            }
        });
    }
    public void readBI2(String biName) {
        // Get the path to the Downloads directory
        String docPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        Log.i(TAG, "Path: " + docPath);

        // Create the full path of the file to be used
        String fileName = docPath + "/" + biName;
        Log.i(TAG, "File: " + fileName);
        File source = new File(fileName);

        // Make the VideoView visible (though it's not necessary if you're using ExoPlayer's PlayerView)
        //videoView.setVisibility(View.VISIBLE);
        exoView.setVisibility(View.VISIBLE);

        try {
            // Create the BI task with the proper file path and media views
            Task biTask = BuddySDK.Companion.createBITask("/storage/emulated/0/Download/", exoView, imageView, true);
            biTask.start(new TaskCallback() {
                @Override
                public void onStarted() {
                    Log.d(TAG, "BI Task started");
                }

                @Override
                public void onSuccess(@NonNull String s) {
                    Log.d(TAG, "BI Task success: " + s);
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "BI Task cancelled");
                }

                @Override
                public void onError(@NonNull String s) {
                    Log.e(TAG, "BI Task error: " + s);
                }

                @Override
                public void onIntermediateResult(@NonNull String s) {
                    Log.e(TAG, "Intermediate result: " + s);
                }
            });

        } catch (Exception e) {
            // Log the exception to help with debugging if something goes wrong
            Log.e(TAG, "Error during BI Task creation", e);
        }
    }
}
