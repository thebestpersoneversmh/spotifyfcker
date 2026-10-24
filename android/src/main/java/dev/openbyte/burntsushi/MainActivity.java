package dev.openbyte.burntsushi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(12, 18, 32);
    private static final int SURFACE = Color.rgb(23, 31, 49);
    private static final int BLUE = Color.rgb(183, 221, 255);
    private static final int DIM = Color.rgb(165, 178, 200);
    private static final int WHITE = Color.WHITE;
    private SharedPreferences prefs;
    private TextView permissionStatus;
    private Switch enabledToggle;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(38), dp(24), dp(28));
        scroll.addView(root);
        setContentView(scroll);

        TextView caption = text("UNOFFICIAL • ANDROID EXPERIMENT", 12, BLUE, true);
        root.addView(caption);
        TextView heading = text("BurntSushi\nAndroid", 34, WHITE, true);
        heading.setLineSpacing(dp(2), 1f);
        root.addView(heading, margins(0, dp(14), 0, dp(10)));
        TextView description = text("A privacy-minded Spotify ad muter for Galaxy S25. Not a port of the Windows DLL injection engine.", 15, DIM, false);
        root.addView(description, margins(0, 0, 0, dp(28)));

        LinearLayout permissions = card();
        root.addView(permissions, margins(0, 0, 0, dp(16)));
        permissions.addView(text("Notification access", 18, WHITE, true));
        permissionStatus = text("Checking…", 14, DIM, false);
        permissions.addView(permissionStatus, margins(0, dp(10), 0, dp(18)));
        Button grant = button("OPEN ACCESS SETTINGS", BLUE, BACKGROUND);
        permissions.addView(grant);
        grant.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        permissions.addView(text("Android requires this permission to read Spotify's exposed media-session metadata. The OS grants notification access broadly; this app does not upload notifications.", 12, DIM, false), margins(0, dp(13), 0, 0));

        LinearLayout feature = card();
        root.addView(feature, margins(0, 0, 0, dp(16)));
        feature.addView(text("Ad muting", 18, WHITE, true));
        enabledToggle = new Switch(this);
        enabledToggle.setText("Enable automatic muting");
        enabledToggle.setTextColor(WHITE);
        enabledToggle.setTextSize(15);
        enabledToggle.setChecked(prefs.getBoolean(SpotifyAdListener.KEY_ENABLED, false));
        feature.addView(enabledToggle, margins(0, dp(12), 0, dp(4)));
        enabledToggle.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(SpotifyAdListener.KEY_ENABLED, checked).apply());
        feature.addView(text("Mutes device-wide media audio only when Spotify explicitly labels the current media item as an advertisement, then restores volume afterward. Detection is not guaranteed.", 13, DIM, false), margins(0, dp(8), 0, 0));

        Button spotify = button("OPEN SPOTIFY", SURFACE, BLUE);
        root.addView(spotify, margins(0, dp(6), 0, dp(20)));
        spotify.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if (launch != null) startActivity(launch);
            else android.widget.Toast.makeText(this, "Spotify is not installed", android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(text("LIMITATIONS", 12, BLUE, true));
        root.addView(text("• Doesn't remove advertisements or intercept HTTPS.\n• Spotify may not expose ad markers, so ads may still play.\n• Media volume affects all apps, not Spotify alone.\n• No root required, no Internet permission, no analytics.", 13, DIM, false), margins(0, dp(10), 0, 0));
    }

    @Override protected void onResume() {
        super.onResume();
        if (permissionStatus == null) return;
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        ComponentName component = new ComponentName(this, SpotifyAdListener.class);
        boolean granted = false;
        if (enabled != null) {
            for (String entry : enabled.split(":")) {
                ComponentName found = ComponentName.unflattenFromString(entry);
                if (component.equals(found)) { granted = true; break; }
            }
        }
        permissionStatus.setText(granted ? "✓ Access enabled" : "Not enabled — tap below to grant access");
        permissionStatus.setTextColor(granted ? BLUE : DIM);
        enabledToggle.setChecked(prefs.getBoolean(SpotifyAdListener.KEY_ENABLED, false));
    }

    private LinearLayout card() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(19), dp(18), dp(19));
        panel.setBackground(rounded(SURFACE, dp(16)));
        return panel;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String text, int bg, int fg) {
        Button view = new Button(this);
        view.setText(text);
        view.setTextColor(fg);
        view.setTextSize(13);
        view.setAllCaps(false);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setBackground(rounded(bg, dp(12)));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) { return (int) (getResources().getDisplayMetrics().density * value + .5f); }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }
}
