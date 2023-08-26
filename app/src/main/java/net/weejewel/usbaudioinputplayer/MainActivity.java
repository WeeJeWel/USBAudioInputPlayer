package net.weejewel.usbaudioinputplayer;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.usb.*;
import android.media.*;
import android.os.Bundle;
import android.util.Log;
import androidx.core.content.ContextCompat;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.HashMap;

// TODO:
// Add visuals — https://github.com/bogerchan/Nier-Visualizer

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private UsbManager usbManager;
    private static final String ACTION_USB_PERMISSION = "com.example.app.USB_PERMISSION";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Register Broadcast Receiver for USB permission and connection
        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    // Handle permission result
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    // USB device attached, try to initialize audio
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    // USB device detached, release resources
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        Log.v("USB", "Checking devices...");
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        UsbDevice usbDevice = null;

        // Iterate through connected devices and find your USB audio device
        for (UsbDevice device : deviceList.values()) {
            String deviceName = device.getProductName();
            Log.v("USB", "Device Name:" + deviceName);

            if (deviceName.contains("USB PnP Audio Device")) {
                usbDevice = device;
                break;
            }
        }

        if (usbDevice == null) {
            return;
        }

        // Request permission
        Log.v("USB", "Checking usb permission...");
        if (!usbManager.hasPermission(usbDevice)) {
            Log.v("USB", "Asking usb permission...");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(usbDevice, pi);
        }
        Log.v("USB", "Got usb permission.");

        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        Log.v("Audio", "Checking audio permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.v("Audio", "Asking audio permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},200);
            return;
        }
        Log.v("Audio", "Got audio permission.");

        Log.v("Audio", "Creating audio recording...");
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
        new Thread(() -> {
            byte[] audioData = new byte[minBufferSize];
            while (true) {
                audioRecord.read(audioData, 0, minBufferSize);
                audioTrack.write(audioData, 0, minBufferSize);
            }
        }).start();
    }

//    @Override
//    public void onBackPressed() {
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        if (fragmentManager.getBackStackEntryCount() > 0) {
//            // If there are other fragments on the back stack, pop it
//            fragmentManager.popBackStack();
//        } else {
//            // If no fragments are on the back stack, finish the activity
//            finish();
//        }
//    }
}