import dev.openbyte.burntsushi.AdDetector;

public class AdDetectorTest {
    public static void main(String[] args) {
        check(true, "Advertisement", null, null, null);
        check(true, null, "Spotify Ad", null, null);
        check(true, null, null, "Advertisement", null);
        check(true, " Spotify Advertisement ", null, null, null);
        check(false, "After Hours", "After Hours", "The Weeknd", "The Weeknd");
        check(false, "Advertisement - Remix", null, "Artist", null);
        check(false, null, null, null, null);
        check(false, "Ad", null, "Spotify", null);
        System.out.println("All 8 AdDetector tests passed.");
    }
    private static void check(boolean expected, String title, String display, String artist, String subtitle) {
        if (AdDetector.isAdvertisement(title, display, artist, subtitle) != expected) {
            throw new AssertionError("Unexpected classification: " + title);
        }
    }
}
