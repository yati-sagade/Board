package com.ysag.board;

import java.util.concurrent.*;
import android.os.*;
import android.app.*;
import android.util.*;
import android.view.*;
import android.media.*;
import android.graphics.*;
import android.support.v4.view.*;

public class MainActivity extends Activity {
    private final int duration = 3; // seconds
    private final int sampleRate = 8000; // hz
    private final int numSamples = sampleRate * duration; // seconds
    private final double[] samples = new double[numSamples];
    private final double frequency = 440; // hz

    private final double minFreq = frequency;

    private final double maxFreq = 2 * minFreq;

    // The current frequency of the tone.
    private volatile double currentFrequency = minFreq;

    // Whether to keep playing or not.
    private volatile boolean playFlag = true;

    private volatile long idx = 0;

    private volatile double[] contSamples = new double[1000];

    private volatile byte[] contMusic = new byte[2000];


    // 16-bit PCM data
    private final byte[] generatedSound = new byte[2 * numSamples];

    private final double TWO_PI = 2 * Math.PI;

    private final double _R = sampleRate / frequency;

    private static final String tag = "board";

    private double screenWidth = 0.0;
    
    private double screenHeight = 0.0;

    private GestureDetectorCompat detector;

    Handler handler = new Handler();

    private final int buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    
    private final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                                         sampleRate,
                                                         AudioFormat.CHANNEL_OUT_STEREO,
                                                         AudioFormat.ENCODING_PCM_16BIT,
                                                         buffSize,
                                                         AudioTrack.MODE_STREAM);


    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create a GestureDetector with a Context and a
        // GestureDetector.OnGestureListener
        detector = new GestureDetectorCompat(this, new BoardGestureListener());

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = (double) size.x;
        screenHeight = (double) size.y;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch(action) {
        case (MotionEvent.ACTION_DOWN) :
            // Start playing here
            Log.d(tag,"Action was DOWN");
            updateFrequency(event);
            startPlaying();
            return true;

        case (MotionEvent.ACTION_MOVE) :
            // Update frequency here
            updateFrequency(event);
            Log.d(tag,"Action was MOVE");
            return true;

        case (MotionEvent.ACTION_UP) :
            // Stop playing here
            Log.d(tag,"Action was UP");
            stopPlaying();
            return true;

        case (MotionEvent.ACTION_CANCEL) :
            Log.d(tag,"Action was CANCEL");
            return false;

        case (MotionEvent.ACTION_OUTSIDE) :
            // Stop playing here
            Log.d(tag,"Movement occurred outside bounds " +
                    "of current screen element");
            stopPlaying();
            return true;      

        default : 
            return super.onTouchEvent(event);
        }      
    }

    private void updateFrequency(MotionEvent event) {
        final float x = event.getX();
        final double frac = x / screenWidth;
        currentFrequency = minFreq + frac * (maxFreq - minFreq);
    }

    private void startPlaying() {
        final Thread thread = new Thread(new Runnable(){
            public void run() {
                while (playFlag) {
                    for (int i = 0; i < contSamples.length; ++i) {
                        contSamples[i] = Math.sin(TWO_PI * currentFrequency * i / sampleRate);
                    }
                    int jdx = 0;
                    for (double sample : contSamples) {
                        short val = (short) (32767 * sample);
                        contMusic[jdx++] = (byte) (val & 0x00ff);
                        contMusic[jdx++] = (byte) ((val & 0xff00) >>> 8);
                    }
                    audioTrack.write(contMusic, 0, contMusic.length);
                }
            }
        });
        playFlag = true;
        audioTrack.play();
        thread.start();
    }

    private void stopPlaying() {
        playFlag = false;
        idx = 0;
        audioTrack.stop();
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
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSound.length,
                AudioTrack.MODE_STATIC
        );
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();
    }

    public static class BoardGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent ev1, MotionEvent ev2, float dx, float dy) {
            Log.d(tag, "onScroll(): " + dx + ", " + dy);
            return true;
        }
    }
}
