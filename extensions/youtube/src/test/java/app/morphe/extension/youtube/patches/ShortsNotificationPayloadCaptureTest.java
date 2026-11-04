package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public class ShortsNotificationPayloadCaptureTest {
    @Test
    public void appendsAndReadsPayloadsFromPersistentStorage() throws IOException {
        File directory = Files.createTempDirectory("shorts-payloads").toFile();

        ShortsNotificationPayloadCapture.append(directory, "first payload");
        ShortsNotificationPayloadCapture.append(directory, "second payload");

        String contents = ShortsNotificationPayloadCapture.read(directory);
        assertTrue(contents.contains("first payload"));
        assertTrue(contents.contains("second payload"));
    }

    @Test
    public void clearRemovesCapturedFiles() throws IOException {
        File directory = Files.createTempDirectory("shorts-payloads-clear").toFile();
        ShortsNotificationPayloadCapture.append(directory, "payload");

        ShortsNotificationPayloadCapture.clear(directory);

        assertTrue(ShortsNotificationPayloadCapture.read(directory).isEmpty());
        assertFalse(new File(directory, ShortsNotificationPayloadCapture.CAPTURE_FILE_NAME).exists());
    }

    @Test
    public void keepsRecentPayloadsWhenCaptureRollsOver() throws IOException {
        File directory = Files.createTempDirectory("shorts-payloads-rollover").toFile();
        String filler = "x".repeat(8_000);

        for (int i = 0; i < 300; i++) {
            ShortsNotificationPayloadCapture.append(directory, "payload-" + i + filler);
        }

        String contents = ShortsNotificationPayloadCapture.read(directory);
        assertTrue(contents.contains("payload-299"));
        assertFalse(contents.contains("payload-0"));
        File capture = new File(directory, ShortsNotificationPayloadCapture.CAPTURE_FILE_NAME);
        File backup = new File(directory, ShortsNotificationPayloadCapture.CAPTURE_FILE_NAME + ".1");
        assertTrue(capture.length() <= ShortsNotificationPayloadCapture.MAX_FILE_BYTES);
        assertTrue(backup.isFile());
        assertTrue(backup.length() <= ShortsNotificationPayloadCapture.MAX_FILE_BYTES);
    }
}
