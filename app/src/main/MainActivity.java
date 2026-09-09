package com.fast.radio;

import android.graphics.Color;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private Equalizer equalizer;
    private TextView statusText;
    private TextView equalizerTitle;
    private SeekBar volumeBar;

    private static final String RADIO_URL =
            "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createUserInterface();
        createPlayer();
    }

    private void createUserInterface() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("FAST RADIO");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);

        mainLayout.addView(title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setText("آماده پخش");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 25, 0, 25);

        mainLayout.addView(statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        Button playButton = new Button(this);
        playButton.setText("پخش رادیو");
        mainLayout.addView(playButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        Button stopButton = new Button(this);
        stopButton.setText("توقف");

        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        stopParams.topMargin = 10;
        mainLayout.addView(stopButton, stopParams);

        TextView volumeTitle = new TextView(this);
        volumeTitle.setText("صدا");
        volumeTitle.setTextSize(18);
        volumeTitle.setGravity(Gravity.CENTER);
        volumeTitle.setPadding(0, 25, 0, 0);
        mainLayout.addView(volumeTitle,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(100);

        mainLayout.addView(volumeBar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        equalizerTitle = new TextView(this);
        equalizerTitle.setText("اکوالایزر صدا");
        equalizerTitle.setTextSize(20);
        equalizerTitle.setGravity(Gravity.CENTER);
        equalizerTitle.setPadding(0, 30, 0, 10);

        mainLayout.addView(equalizerTitle,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView equalizerInfo = new TextView(this);
        equalizerInfo.setText("اکوالایزر پس از شروع پخش فعال می‌شود");
        equalizerInfo.setTextSize(14);
        equalizerInfo.setGravity(Gravity.CENTER);

        mainLayout.addView(equalizerInfo,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(mainLayout);

        playButton.setOnClickListener(v -> playRadio());
        stopButton.setOnClickListener(v -> stopRadio());

        volumeBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {

                        if (player != null) {
                            player.setVolume(progress == 0
                                    ? 0.0f
                                    : progress / 100.0f);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
    }

    private void createPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setVolume(1.0f);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    statusText.setText("در حال اتصال به رادیو...");
                    statusText.setTextColor(Color.DKGRAY);
                } else if (playbackState == Player.STATE_READY) {
                    statusText.setText("در حال پخش");
                    statusText.setTextColor(Color.rgb(0, 150, 0));
                    setupEqualizer();
                } else if (playbackState == Player.STATE_ENDED) {
                    statusText.setText("پخش پایان یافت");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                statusText.setText("خطا در پخش رادیو");
                statusText.setTextColor(Color.RED);
            }
        });
    }

    private void playRadio() {
        if (player == null) {
            createPlayer();
        }

        MediaItem mediaItem =
                MediaItem.fromUri(Uri.parse(RADIO_URL));

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        statusText.setText("در حال اتصال...");
    }

    private void stopRadio() {
        if (player != null) {
            player.stop();
            releaseEqualizer();
            statusText.setText("متوقف شد");
            statusText.setTextColor(Color.BLACK);
        }
    }

    private void setupEqualizer() {
        releaseEqualizer();

        try {
            int audioSessionId = player.getAudioSessionId();

            if (audioSessionId == 0) {
                return;
            }

            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);

            short numberOfBands = equalizer.getNumberOfBands();
            short minLevel = equalizer.getBandLevelRange()[0];
            short maxLevel = equalizer.getBandLevelRange()[1];

            View content = findViewById(android.R.id.content);

            if (!(content instanceof android.view.ViewGroup)) {
                return;
            }

            android.view.ViewGroup contentGroup =
                    (android.view.ViewGroup) content;

            if (contentGroup.getChildCount() == 0) {
                return;
            }

            View firstChild = contentGroup.getChildAt(0);

            if (!(firstChild instanceof LinearLayout)) {
                return;
            }

            LinearLayout root = (LinearLayout) firstChild;

            removeOldEqualizerControls(root);

            int bandsToShow = Math.min((int) numberOfBands, 5);

            equalizerTitle.setText(
                    "اکوالایزر صدا - " + bandsToShow + " باند");

            for (short band = 0; band < bandsToShow; band++) {
                addEqualizerBand(root, band, minLevel, maxLevel);
            }

        } catch (Exception e) {
            equalizerTitle.setText(
                    "اکوالایزر در این دستگاه قابل فعال‌سازی نیست");
        }
    }

    private void addEqualizerBand(
            LinearLayout root,
            short band,
            short minLevel,
            short maxLevel) {

        LinearLayout bandLayout = new LinearLayout(this);
        bandLayout.setOrientation(LinearLayout.VERTICAL);

        TextView bandText = new TextView(this);
        bandText.setText(getFrequencyText(band));
        bandText.setGravity(Gravity.CENTER);
        bandText.setTextSize(14);

        bandLayout.addView(bandText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        SeekBar bandBar = new SeekBar(this);
        int range = maxLevel - minLevel;
        bandBar.setMax(range);

        short currentLevel = equalizer.getBandLevel(band);
        bandBar.setProgress(currentLevel - minLevel);

        final short selectedBand = band;

        bandBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (equalizer != null) {
                            short level = (short) (minLevel + progress);
                            equalizer.setBandLevel(selectedBand, level);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        bandLayout.addView(bandBar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(bandLayout);
    }

    private String getFrequencyText(short band) {
        switch (band) {
            case 0:
                return "بیس";
            case 1:
                return "پایین";
            case 2:
                return "میانی";
            case 3:
                return "بالا";
            case 4:
                return "زیر";
            default:
                return "باند " + (band + 1);
        }
    }

    private void removeOldEqualizerControls(LinearLayout root) {
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View child = root.getChildAt(i);

            if (child instanceof LinearLayout) {
                LinearLayout possibleBand =
                        (LinearLayout) child;

                if (possibleBand.getChildCount() == 2
                        && possibleBand.getChildAt(1) instanceof SeekBar) {
                    root.removeViewAt(i);
                }
            }
        }
    }

    private void releaseEqualizer() {
        if (equalizer != null) {
            try {
                equalizer.setEnabled(false);
            } catch (Exception ignored) {
            }

            try {
                equalizer.release();
            } catch (Exception ignored) {
            }

            equalizer = null;
        }
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
        releaseEqualizer();

        if (player != null) {
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}
