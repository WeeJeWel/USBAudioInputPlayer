package net.weejewel.usbaudioinputplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

// TODO:
// Add visuals — https://github.com/bogerchan/Nier-Visualizer

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.checkForAudioPermission();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                audioTrack.stop();
                Toast
                    .makeText(this, "Stopped Playback", Toast.LENGTH_SHORT)
                    .show();

                finish();
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PLAY: {
                if(audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                    audioTrack.play();
                    Toast
                        .makeText(this, "Resumed Playback", Toast.LENGTH_LONG)
                        .show();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PAUSE: {
                if(audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.pause();
                    Toast
                        .makeText(this, "Paused Playback", Toast.LENGTH_LONG)
                        .show();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 200: // Audio Permission
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("Audio", "Got Audio permission.");
                    MainActivity.this.startListening();
                } else {
                    Log.v("Audio", "Denied Audio permission.");

                    Toast
                            .makeText(this, "You must allow the Recording permission!", Toast.LENGTH_LONG)
                            .show();

                    finish();
                }
                break;
        }
    }


    public void checkForAudioPermission() {
        Log.v("Audio", "Checking for Audio permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.v("Audio", "Already got Audio permission.");
            this.startListening();
        } else {
            Log.v("Audio", "Asking for Audio permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        }
    }

    public void startListening() {
        Log.v("Audio", "Starting listening...");

        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        Log.v("Audio", "Creating audio recording...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.v("Audio", "Missing Audio permission.");
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, minBufferSize);

        Log.v("Audio", "Starting audio recording...");
        audioRecord.startRecording();

        Log.v("Audio", "Creating audio track...");
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                audioFormat,
                minBufferSize,
                AudioTrack.MODE_STREAM);

        Log.v("Audio", "Playing audio track...");
        audioTrack.play();

        Log.v("Audio", "Streaming audio recording to audio track...");
        Toast
            .makeText(this, "Now playing audio from " + audioRecord.getRoutedDevice().getProductName() + " to TV", Toast.LENGTH_LONG)
            .show();

        new Thread(() -> {
            byte[] audioData = new byte[minBufferSize];
            while (true) {
                audioRecord.read(audioData, 0, minBufferSize);
                audioTrack.write(audioData, 0, minBufferSize);
            }
        }).start();
    }
}