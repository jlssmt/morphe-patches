package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;

public class ShortsNotificationsPatchTest {
    private static final int PUSH_NOTIFICATION_RENDERER_FIELD = 77819057;
    private static final int REEL_WATCH_ENDPOINT_FIELD = 139608561;

    @Test
    public void identifiesReelEndpointInSerializedPushProto() throws IOException {
        byte[] endpoint = fieldBytes(50, "reel".getBytes(StandardCharsets.UTF_8));
        byte[] navigation = fieldBytes(REEL_WATCH_ENDPOINT_FIELD, endpoint);
        byte[] renderer = fieldBytes(2, navigation);

        assertTrue(ShortsNotificationsPatch.isShortsNotification(
                fieldBytes(PUSH_NOTIFICATION_RENDERER_FIELD, renderer)));
    }

    @Test
    public void ignoresReelEndpointOutsidePushNotificationRenderer() throws IOException {
        byte[] endpoint = fieldBytes(50, "reel".getBytes(StandardCharsets.UTF_8));
        byte[] navigation = fieldBytes(REEL_WATCH_ENDPOINT_FIELD, endpoint);

        assertFalse(ShortsNotificationsPatch.isShortsNotification(fieldBytes(1, navigation)));
    }

    @Test
    public void ignoresPushNotificationsWithoutReelEndpoint() throws IOException {
        byte[] videoEndpoint = fieldBytes(50, "watch".getBytes(StandardCharsets.UTF_8));
        byte[] renderer = fieldBytes(2, videoEndpoint);

        assertFalse(ShortsNotificationsPatch.isShortsNotification(
                fieldBytes(PUSH_NOTIFICATION_RENDERER_FIELD, renderer)));
    }

    @Test
    public void dumpsPrivateNestedFieldsWhenToStringIsAnOpaqueIdentity() {
        String payload = ShortsNotificationsPatch.debugPayload(new PlainPayload());

        assertTrue(payload.contains("b=77819057"));
        assertTrue(payload.contains("title=Shorts"));
    }

    @Test
    public void dumpsNestedFieldsWhenCustomToStringHidesThem() {
        String payload = ShortsNotificationsPatch.debugPayload(new StructuredPayload());

        assertTrue(payload.contains("f=91"));
    }

    @Test
    public void includesCompleteSerializedProtoBytes() {
        String payload = ShortsNotificationsPatch.debugPayload(new SerializedPayload());

        assertTrue(payload.contains("protobuf_base64=CAEQAg=="));
    }

    private static byte[] fieldBytes(int fieldNumber, byte[] value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeByteArray(fieldNumber, value);
        output.flush();
        return bytes.toByteArray();
    }

    private static final class StructuredPayload {
        private final int b = 77819057;
        private final OpaqueNestedPayload c = new OpaqueNestedPayload();

        @Override
        public String toString() {
            return "bjox{b=77819057, c=# bbfu@99d7722e}";
        }
    }

    private static final class OpaqueNestedPayload {
        private final int f = 91;
    }

    private static final class PlainPayload {
        private final int b = 77819057;
        private final NestedPayload c = new NestedPayload();

        @Override
        public String toString() {
            return "# PlainPayload@e3645603";
        }
    }

    private static final class NestedPayload {
        private final String title = "Shorts";
    }

    private static final class SerializedPayload {
        public byte[] toByteArray() {
            return new byte[]{0x08, 0x01, 0x10, 0x02};
        }
    }
}
