package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;

import defpackage.blie;
import defpackage.bjts;

public class ShortsNotificationsPatchTest {
    @Test
    public void identifiesPushNotificationsWithAReelWatchEndpoint() {
        FakePush push = new FakePush();
        push.c = new FakeRenderer(new FakeEndpoint(blie.b.d));

        assertTrue(ShortsNotificationsPatch.isShortsNotification(push));
    }

    @Test
    public void identifiesLegacyReelWatchEndpoints() {
        FakePush push = new FakePush();
        push.c = new FakeRenderer(new FakeEndpoint(bjts.b.d));

        assertTrue(ShortsNotificationsPatch.isShortsNotification(push));
    }

    @Test
    public void detectsReelWatchEndpointInSerializedPushProto() throws IOException {
        byte[] endpoint = fieldBytes(50, "reel".getBytes(StandardCharsets.UTF_8));
        byte[] navigation = fieldBytes(139608561, endpoint);
        byte[] renderer = fieldBytes(2, navigation);
        SerializedShortPush push = new SerializedShortPush(fieldBytes(77819057, renderer));

        assertTrue(ShortsNotificationsPatch.isShortsNotification(push));
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

    @Test
    public void doesNotFilterRegularUploadsOrOtherPushTypes() {
        FakePush regularVideo = new FakePush();
        regularVideo.c = new FakeRenderer(new FakeEndpoint(new Object()));
        assertFalse(ShortsNotificationsPatch.isShortsNotification(regularVideo));

        FakePush otherPush = new FakePush();
        otherPush.b = 1;
        otherPush.c = new FakeRenderer(new FakeEndpoint(blie.b.d));
        assertFalse(ShortsNotificationsPatch.isShortsNotification(otherPush));
    }

    public static final class FakePush {
        public int b = 77819057;
        public Object c;
    }

    public static final class FakeRenderer {
        public Object f;

        FakeRenderer(Object navigationEndpoint) {
            f = navigationEndpoint;
        }
    }

    public static final class FakeEndpoint {
        public FakeFieldSet l;

        FakeEndpoint(Object descriptor) {
            l = new FakeFieldSet(descriptor);
        }
    }

    public static final class FakeFieldSet {
        private final Object descriptor;

        FakeFieldSet(Object descriptor) {
            this.descriptor = descriptor;
        }

        public boolean n(Object descriptor) {
            return this.descriptor == descriptor;
        }
    }

    private static byte[] fieldBytes(int fieldNumber, byte[] value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeByteArray(fieldNumber, value);
        output.flush();
        return bytes.toByteArray();
    }

    public static final class SerializedShortPush {
        public int b = 77819057;
        private final byte[] serialized;

        SerializedShortPush(byte[] serialized) {
            this.serialized = serialized;
        }

        public byte[] toByteArray() {
            return serialized;
        }
    }

    public static final class SerializedPayload {
        public byte[] toByteArray() {
            return new byte[]{0x08, 0x01, 0x10, 0x02};
        }
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
}
