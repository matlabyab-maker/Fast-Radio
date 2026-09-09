package com.fast.radio;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private TextView statusText;

    private static final String RADIO_URL =
            "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);

        Button playButton = findViewById(R.id.playButton);
        Button stopButton = findViewById(R.id.stopButton);

        player = new ExoPlayer.Builder(this).build();

        player.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int playbackState) {

                if (playbackState == Player.STATE_BUFFERING) {
                    statusText.setText("در حال اتصال...");
                } else if (playbackState == Player.STATE_READY) {
                    statusText.setText("در حال پخش");
                } else if (playbackState == Player.STATE_ENDED) {
                    statusText.setText("پخش پایان یافت");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                statusText.setText("خطا در پخش");
            }
        });

        playButton.setOnClickListener(v -> playRadio());

        stopButton.setOnClickListener(v -> stopRadio());
    }

    private void playRadio() {

        MediaItem mediaItem =
                MediaItem.fromUri(Uri.parse(RADIO_URL));

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        statusText.setText("در حال اتصال...");
    }

    private void stopRadio() {

        player.stop();
        statusText.setText("متوقف شد");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
            player = null;
        }
    }
}
