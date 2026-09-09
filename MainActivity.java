package com.fast.radio;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText streamUrl;
    private Button playButton;
    private Button stopButton;
    private TextView statusText;
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        streamUrl = findViewById(R.id.streamUrl);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);
        statusText = findViewById(R.id.statusText);

        streamUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        playButton.setOnClickListener(v -> startRadio());
        stopButton.setOnClickListener(v -> stopRadio());
    }

    private void startRadio() {
        String url = streamUrl.getText().toString().trim();

        if (url.isEmpty()) {
            statusText.setText("لطفاً آدرس پخش رادیو را وارد کنید.");
            return;
        }

        stopRadio();

        statusText.setText("در حال اتصال...");

        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        player.setOnPreparedListener(mp -> {
            mp.start();
            statusText.setText("در حال پخش");
            playButton.setEnabled(false);
            stopButton.setEnabled(true);
        });

        player.setOnErrorListener((mp, what, extra) -> {
            statusText.setText("خطا در پخش یا آدرس رادیو.");
            releasePlayer();
            return true;
        });

        try {
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) {
            statusText.setText("آدرس پخش معتبر نیست.");
            releasePlayer();
        }
    }

    private void stopRadio() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }
            releasePlayer();
        }
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
        if (statusText != null) {
            statusText.setText("متوقف");
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.reset();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        stopRadio();
        super.onDestroy();
    }
}
