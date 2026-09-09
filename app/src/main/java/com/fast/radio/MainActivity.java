package com.fast.radio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ExoPlayer player;
    private Equalizer equalizer;
    private TextView statusText;
    private TextView nowPlaying;
    private TextView lamp;
    private SeekBar volumeBar;
    private LinearLayout stationList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private float volume = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createUserInterface();
        createPlayer();
        loadStations();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        return t;
    }

    private void createUserInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("FAST RADIO", 25);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        lamp = text("●", 26);
        lamp.setTextColor(Color.GRAY);
        top.addView(lamp, new LinearLayout.LayoutParams(-2, -2));
        root.addView(top);

        nowPlaying = text("ایستگاه انتخاب نشده", 17);
        nowPlaying.setGravity(Gravity.CENTER);
        nowPlaying.setPadding(0, dp(6), 0, dp(2));
        root.addView(nowPlaying, new LinearLayout.LayoutParams(-1, -2));

        statusText = text("آماده پخش", 15);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button stop = new Button(this);
        stop.setText("توقف");
        stop.setOnClickListener(v -> stopPlayback());
        controls.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));

        Button eq = new Button(this);
        eq.setText("اکوالایزر");
        eq.setOnClickListener(v -> showEqualizer());
        controls.addView(eq, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls);

        TextView volumeLabel = text("صدا", 15);
        volumeLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(volumeLabel, new LinearLayout.LayoutParams(-1, -2));

        volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(100);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                volume = progress / 100f;
                if (player != null) player.setVolume(volume);
                if (progress == 0) statusText.setText("بی‌صدا");
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        root.addView(volumeBar, new LinearLayout.LayoutParams(-1, -2));

        TextView listTitle = text("ایستگاه‌ها (111)", 19);
        listTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        listTitle.setPadding(0, dp(8), 0, dp(6));
        root.addView(listTitle);

        ScrollView scroll = new ScrollView(this);
        stationList = new LinearLayout(this);
        stationList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(stationList, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private void createPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setVolume(volume);
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    statusText.setText("در حال پخش");
                    startLamp();
                } else {
                    stopLamp();
                }
            }
            @Override public void onPlayerError(PlaybackException error) {
                statusText.setText("خطا در پخش؛ ایستگاه دیگری را امتحان کنید");
                stopLamp();
            }
        });
        setupEqualizer();
    }

    private void setupEqualizer() {
        try {
            equalizer = new Equalizer(0, 0);
            equalizer.setEnabled(true);
        } catch (Exception ignored) {
            equalizer = null;
        }
    }

    private void startLamp() {
        handler.removeCallbacksAndMessages(null);
        handler.post(new Runnable() {
            boolean on = false;
            @Override public void run() {
                if (player != null && player.isPlaying()) {
                    lamp.setTextColor(on ? Color.GREEN : Color.GRAY);
                    on = !on;
                    handler.postDelayed(this, 500);
                }
            }
        });
    }

    private void stopLamp() {
        handler.removeCallbacksAndMessages(null);
        if (lamp != null) lamp.setTextColor(Color.GRAY);
    }

    private void loadStations() {
        try {
            InputStream in = getAssets().open("stations.json");
            byte[] bytes = new byte[in.available()];
            int read = in.read(bytes);
            in.close();
            JSONArray array = new JSONArray(new String(bytes, "UTF-8"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                addStationButton(obj.getString("name"), obj.getString("stream"), obj.optString("icon", ""));
            }
        } catch (Exception e) {
            statusText.setText("خطا در خواندن فهرست ایستگاه‌ها");
        }
    }

    private void addStationButton(String name, String stream, String iconUrl) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(5), dp(8), dp(5));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(245,245,245));
        bg.setCornerRadius(dp(10));
        row.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_media_play);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView label = text(name, 16);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        label.setPadding(dp(8), 0, dp(8), 0);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(54), 1));

        row.setOnClickListener(v -> playStation(name, stream));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(3), 0, dp(3));
        stationList.addView(row, p);

        if (iconUrl != null && !iconUrl.trim().isEmpty()) loadIcon(iconUrl, icon);
    }

    private void loadIcon(String url, ImageView target) {
        imageExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(7000);
                c.setInstanceFollowRedirects(true);
                c.connect();
                if (c.getResponseCode() >= 200 && c.getResponseCode() < 400) {
                    InputStream in = c.getInputStream();
                    bitmap = BitmapFactory.decodeStream(in);
                    in.close();
                }
                c.disconnect();
            } catch (Exception ignored) {}
            Bitmap result = bitmap;
            if (result != null) {
                handler.post(() -> target.setImageBitmap(result));
            }
        });
    }

    private void playStation(String name, String stream) {
        try {
            nowPlaying.setText(name);
            statusText.setText("در حال اتصال...");
            MediaItem item = MediaItem.fromUri(Uri.parse(stream));
            player.setMediaItem(item);
            player.prepare();
            player.setVolume(volume);
            player.play();
        } catch (Exception e) {
            statusText.setText("آدرس این ایستگاه قابل پخش نیست");
        }
    }

    private void stopPlayback() {
        if (player != null) player.stop();
        statusText.setText("متوقف شد");
        stopLamp();
    }

    private void showEqualizer() {
        if (equalizer == null) {
            statusText.setText("اکوالایزر در این دستگاه قابل فعال‌سازی نیست");
            return;
        }
        final short bands = equalizer.getNumberOfBands();
        final short min = equalizer.getBandLevelRange()[0];
        final short max = equalizer.getBandLevelRange()[1];
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(10));
        TextView title = text("اکوالایزر 5 باند", 20);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        int count = Math.min(5, bands);
        for (short b = 0; b < count; b++) {
            TextView label = text("باند " + (b + 1), 14);
            box.addView(label);
            SeekBar bar = new SeekBar(this);
            bar.setMax(max - min);
            bar.setProgress(equalizer.getBandLevel(b) - min);
            final short band = b;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    try { equalizer.setBandLevel(band, (short)(min + progress)); } catch (Exception ignored) {}
                }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            box.addView(bar);
        }
        new android.app.AlertDialog.Builder(this).setView(box).setPositiveButton("بستن", null).show();
    }

    @Override protected void onDestroy() {
        stopLamp();
        if (equalizer != null) { try { equalizer.release(); } catch (Exception ignored) {} }
        if (player != null) player.release();
        imageExecutor.shutdownNow();
        super.onDestroy();
    }
}
