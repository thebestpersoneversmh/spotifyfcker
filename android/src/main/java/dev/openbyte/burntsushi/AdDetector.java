package dev.openbyte.burntsushi;

import java.util.Locale;

/**
 * Conservative, metadata-only indicator matcher. Spotify is not required to expose
 * ad indicators via Android MediaSession, so false negatives are expected.
 */
public final class AdDetector {
    private AdDetector() { }

    public static boolean isAdvertisement(String title, String displayTitle,
                                          String artist, String displaySubtitle) {
        return isAdLabel(title) || isAdLabel(displayTitle)
                || (isAdLabel(artist) && isBlank(title))
                || (isAdLabel(displaySubtitle) && isBlank(displayTitle));
    }

    private static boolean isAdLabel(String text) {
        if (text == null) return false;
        String value = text.trim().toLowerCase(Locale.ROOT);
        return value.equals("advertisement")
                || value.equals("spotify advertisement")
                || value.equals("spotify ad")
                || value.equals("advertisement from spotify")
                || value.equals("advertisement • spotify");
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
