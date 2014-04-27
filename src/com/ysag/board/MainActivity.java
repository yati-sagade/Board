package com.ysag.board;

import android.os.*;
import android.app.*;
import android.util.*;
import android.view.*;
import android.media.*;
import android.support.v4.view.MotionEventCompat;

public class MainActivity extends Activity {
    private final int duration = 3; // seconds
    private final int sampleRate = 8000; // hz
    private final int numSamples = sampleRate * duration; // seconds
    private final double[] samples = new double[numSamples];
    private final double frequency = 440; // hz

    // 16-bit PCM data
    private final byte[] generatedSound = new byte[2 * numSamples];

    private final double TWO_PI = 2 * Math.PI;

    private final double _R = sampleRate / frequency;

    Handler handler = new Handler();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void generateTone(View view) {
        Log.d("board", "Here in generateTone()");
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                _generateTone();
                handler.post(new Runnable() {
                    public void run() {
                        playSound();
                    }
                });
            }
        });
        Log.d("board", "Created the thread");
        thread.start();
    }

    private void _generateTone() {
        final double end = 2 * frequency;
        for (int i = 0; i < numSamples; ++i) {
            final double frac = i / (double) numSamples;
            final double f = frequency + frac * (end - frequency);
            samples[i] = Math.sin(TWO_PI * i * f / sampleRate);
        }

        // Convert the sample buffer into a 16 bit PCM sound array.
        int idx = 0;
        for (final double sample : samples) {
            // Scale to maximum amplitude
            final short val = (short) (sample * 32767);

            // In 16-bit WAV PCM, the the first byte is the lower byte.
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    private void playSound() {
        final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSound.length,
                AudioTrack.MODE_STATIC
        );
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();
    }
}
