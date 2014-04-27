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
    private final int sampleRate = 8000; // hz
    private static final String tag = "board";
    private float screenWidth = 0.0f;
    private float screenHeight = 0.0f;
    private final float minFreq = 440;
    private final float maxFreq = 2 * minFreq;

    private volatile float frequency = 0.0f;

    private volatile boolean isPlaying = false;

    private final int buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    
    private final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                                         sampleRate,
                                                         AudioFormat.CHANNEL_OUT_STEREO,
                                                         AudioFormat.ENCODING_PCM_16BIT,
                                                         buffSize,
                                                         AudioTrack.MODE_STREAM);

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = (float) size.x;
        screenHeight = (float) size.y;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isPlaying) {
            stopPlaying();
        }
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
        final float frac = x / screenWidth;
        frequency = minFreq + frac * (maxFreq - minFreq);
    }

    private void startPlaying() {
        isPlaying = true;
        audioTrack.play();
        new PlayerTask().execute();
    }

    private void stopPlaying() {
        isPlaying = false;
        audioTrack.stop();
    }

    private class PlayerTask extends AsyncTask<Void, Void, Void> {
        float increment;
        float angle = 0;
        short[] samples = new short[512];

        @Override
        protected Void doInBackground(Void... params) {
            while (isPlaying) {
                for (int i = 0; i < samples.length; ++i) {
                    increment = (float) (2 * Math.PI) * frequency / sampleRate;
                    samples[i] = (short)((float)Math.sin(angle) * Short.MAX_VALUE);
                    angle += increment;
                    angle %= (2.0f * (float) Math.PI);
                }
                audioTrack.write(samples, 0, samples.length);
            }
            return null;
        }
    }

}
