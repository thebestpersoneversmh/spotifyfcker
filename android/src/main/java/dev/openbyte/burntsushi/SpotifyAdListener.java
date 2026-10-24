package dev.openbyte.burntsushi;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Collections;
import java.util.List;

/**
 * Watches Spotify's exposed media session, and *temporarily mutes the global
 * media volume* if its metadata explicitly identifies an advertisement.
 * No traffic inspection, private APIs, injection, root, or Internet access.
 */
public final class SpotifyAdListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String SPOTIFY_PACKAGE = "com.spotify.music";
    private static final String PREFS = "settings";
    public static final String KEY_ENABLED = "enabled";

    private SharedPreferences preferences;
    private MediaSessionManager sessionManager;
    private AudioManager audioManager;
    private MediaController spotifyController;
    private Handler handler;
    private boolean sessionListenerInstalled;
    private int savedVolume = -1;

    private final MediaSessionManager.OnActiveSessionsChangedListener activeSessionsChanged =
            this::attachSpotifySession;

    private final MediaController.Callback spotifyCallback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { evaluateAdState(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { evaluateAdState(); }
        @Override public void onSessionDestroyed() { refreshSessions(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        sessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        if (sessionManager == null) return;
        try {
            ComponentName listener = new ComponentName(this, SpotifyAdListener.class);
            sessionManager.addOnActiveSessionsChangedListener(activeSessionsChanged, listener, handler);
            sessionListenerInstalled = true;
            refreshSessions();
        } catch (SecurityException | IllegalArgumentException ignored) {
            restoreMediaVolume();
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        // Spotify may initialize its session after initially posting a notification.
        if (sbn != null && SPOTIFY_PACKAGE.equals(sbn.getPackageName())) {
            refreshSessions();
        }
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && SPOTIFY_PACKAGE.equals(sbn.getPackageName())) {
            refreshSessions();
        }
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (KEY_ENABLED.equals(key)) handler.post(this::evaluateAdState);
    }

    private void refreshSessions() {
        if (sessionManager == null) return;
        try {
            List<MediaController> sessions = sessionManager.getActiveSessions(
                    new ComponentName(this, SpotifyAdListener.class));
            attachSpotifySession(sessions);
        } catch (SecurityException | IllegalArgumentException ignored) {
            attachSpotifySession(Collections.emptyList());
        }
    }

    private void attachSpotifySession(List<MediaController> sessions) {
        MediaController next = null;
        if (sessions != null) {
            for (MediaController session : sessions) {
                if (SPOTIFY_PACKAGE.equals(session.getPackageName())) {
                    next = session;
                    break;
                }
            }
        }
        if (spotifyController != null && next != null
                && spotifyController.getSessionToken().equals(next.getSessionToken())) {
            evaluateAdState();
            return;
        }
        if (spotifyController != null) spotifyController.unregisterCallback(spotifyCallback);
        spotifyController = next;
        if (spotifyController != null) spotifyController.registerCallback(spotifyCallback, handler);
        evaluateAdState();
    }

    private void evaluateAdState() {
        boolean shouldMute = false;
        if (preferences.getBoolean(KEY_ENABLED, false) && spotifyController != null) {
            PlaybackState state = spotifyController.getPlaybackState();
            MediaMetadata metadata = spotifyController.getMetadata();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING
                    && metadata != null) {
                shouldMute = AdDetector.isAdvertisement(
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                        metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                        metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
            }
        }
        if (shouldMute) muteMediaVolume();
        else restoreMediaVolume();
    }

    private void muteMediaVolume() {
        if (audioManager == null || savedVolume >= 0) return;
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (current == 0) return; // Respect preexisting mute; don't unmute later.
        savedVolume = current;
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        } catch (SecurityException ex) {
            savedVolume = -1;
        }
    }

    private void restoreMediaVolume() {
        if (savedVolume < 0) return;
        int original = savedVolume;
        savedVolume = -1;
        if (audioManager == null) return;
        try {
            // If the user or another app changed volume during an ad, respect it.
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0);
            }
        } catch (SecurityException ignored) { }
    }

    @Override public void onListenerDisconnected() {
        cleanupSessions();
        super.onListenerDisconnected();
    }

    @Override public void onDestroy() {
        cleanupSessions();
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void cleanupSessions() {
        if (spotifyController != null) {
            spotifyController.unregisterCallback(spotifyCallback);
            spotifyController = null;
        }
        if (sessionListenerInstalled && sessionManager != null) {
            sessionManager.removeOnActiveSessionsChangedListener(activeSessionsChanged);
            sessionListenerInstalled = false;
        }
        restoreMediaVolume();
    }
}
