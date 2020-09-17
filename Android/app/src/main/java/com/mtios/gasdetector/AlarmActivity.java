package com.mtios.gasdetector;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AlarmActivity extends AppCompatActivity {

    public Button mDismiss;
    public Button test;

    MediaPlayer mMediaPlayer;
    Vibrator mVibrator;
    long[] pattern = { 0, 200, 0 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        mDismiss = (Button) findViewById(R.id.dismiss);

        mDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlarmActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVibrator.vibrate(pattern, 0);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer = MediaPlayer.create(AlarmActivity.this, R.raw.alarm);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayer.stop();
        mVibrator.cancel();
    }
}
