package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void dumpsPrivateNestedFieldsWhenPayloadUsesObjectToString() {
        String payload = ShortsNotificationsPatch.debugPayload(new PlainPayload());

        assertTrue(payload.contains("b=77819057"));
        assertTrue(payload.contains("title=Shorts"));
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

    private static final class PlainPayload {
        private final int b = 77819057;
        private final NestedPayload c = new NestedPayload();
    }

    private static final class NestedPayload {
        private final String title = "Shorts";
    }
}
