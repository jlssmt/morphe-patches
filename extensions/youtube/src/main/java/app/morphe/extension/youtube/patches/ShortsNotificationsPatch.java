/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.settings.Settings;

public final class ShortsNotificationsPatch {
    // shortcut: assumes stable push proto fields and endpoint extension names, update if YouTube changes the schema.
    private static final int PUSH_NOTIFICATION_RENDERER = 77819057;
    private static final int MAX_DEBUG_PAYLOAD_LENGTH = 8000;
    private static final int MAX_DEBUG_PAYLOAD_DEPTH = 3;
    private static final String[] REEL_WATCH_ENDPOINT_EXTENSIONS = {
            "defpackage.blie", // YouTube 21.42+
            "defpackage.bjts"  // YouTube 21.30
    };
    private static final Object HIDDEN_COUNT_LOCK = new Object();

    private ShortsNotificationsPatch() {
    }

    public static boolean shouldSuppressShortsNotification(Context context, Object pushNotification) {
        if (context != null && !Utils.isContextSet()) {
            Utils.setContext(context.getApplicationContext());
        }

        boolean captureEnabled = Settings.CAPTURE_SHORTS_NOTIFICATION_PAYLOADS.get();
        boolean filterEnabled = Settings.DISABLE_SHORTS_NOTIFICATIONS.get();
        if (!captureEnabled && !filterEnabled) return false;

        boolean isShorts = isShortsNotification(pushNotification);
        if (captureEnabled) {
            captureNotificationPayload(context, pushNotification, filterEnabled, isShorts);
        }
        if (!filterEnabled || !isShorts) {
            if (filterEnabled) {
                Logger.printDebug(() -> "Shorts notification filter: ReelWatchEndpoint=false, decision=passed");
            }
            return false;
        }

        int sinceLaunch;
        int total;
        synchronized (HIDDEN_COUNT_LOCK) {
            sinceLaunch = increment(Settings.SHORTS_NOTIFICATIONS_HIDDEN_SINCE_LAUNCH.get());
            total = increment(Math.max(Settings.SHORTS_NOTIFICATIONS_HIDDEN_TOTAL.get(), sinceLaunch - 1));
            Settings.SHORTS_NOTIFICATIONS_HIDDEN_SINCE_LAUNCH.save(sinceLaunch);
            Settings.SHORTS_NOTIFICATIONS_HIDDEN_TOTAL.save(total);
        }
        int hiddenSinceLaunch = sinceLaunch;
        int hiddenTotal = total;
        Logger.printDebug(() -> "Shorts notification filter: ReelWatchEndpoint=true, decision=suppressed, since launch="
                + hiddenSinceLaunch + ", total=" + hiddenTotal);
        return true;
    }

    public static String readCapturedPayloads(Context context) throws IOException {
        return ShortsNotificationPayloadCapture.read(context.getFilesDir());
    }

    public static void clearCapturedPayloads(Context context) throws IOException {
        ShortsNotificationPayloadCapture.clear(context.getFilesDir());
    }

    private static void captureNotificationPayload(Context context, Object pushNotification,
                                                   boolean filterEnabled, boolean isShorts) {
        if (context == null) return;

        String type = pushNotification == null ? "null" : pushNotification.getClass().getName();
        String decision = filterEnabled && isShorts ? "suppressed" : "passed";
        String entry = "timestamp_ms=" + System.currentTimeMillis() + " type=" + type
                + " filter_enabled=" + filterEnabled
                + " classified_shorts=" + isShorts
                + " decision=" + decision + System.lineSeparator()
                + debugPayload(pushNotification);
        try {
            ShortsNotificationPayloadCapture.append(context.getFilesDir(), entry);
        } catch (IOException | RuntimeException ex) {
            Logger.printException(() -> "Could not persist push notification payload", ex);
        }
    }

    public static void showHiddenShortsCountOnLaunch(Activity activity) {
        synchronized (HIDDEN_COUNT_LOCK) {
            int sinceLaunch = Settings.SHORTS_NOTIFICATIONS_HIDDEN_SINCE_LAUNCH.get();
            int total = Math.max(Settings.SHORTS_NOTIFICATIONS_HIDDEN_TOTAL.get(), sinceLaunch);
            if (sinceLaunch <= 0 && !Settings.DISABLE_SHORTS_NOTIFICATIONS.get()) return;

            int stringId = activity.getResources().getIdentifier(
                    "morphe_shorts_notifications_hidden_count_toast", "string", activity.getPackageName());
            if (stringId == 0) {
                Logger.printException(() -> "Shorts notification count toast string not found");
                return;
            }

            try {
                Toast.makeText(activity, activity.getResources().getString(stringId, sinceLaunch, total), Toast.LENGTH_LONG).show();
                Settings.SHORTS_NOTIFICATIONS_HIDDEN_SINCE_LAUNCH.save(0);
                Settings.SHORTS_NOTIFICATIONS_HIDDEN_TOTAL.save(total);
            } catch (RuntimeException ex) {
                Logger.printException(() -> "Could not show Shorts notification count", ex);
            }
        }
    }

    private static int increment(int value) {
        return value < Integer.MAX_VALUE ? value + 1 : value;
    }

    static String debugPayload(Object pushNotification) {
        final String payload;
        try {
            payload = pushNotification != null && hasDefaultToString(pushNotification.getClass())
                    ? dumpFields(pushNotification, 0)
                    : String.valueOf(pushNotification);
        } catch (RuntimeException ex) {
            return "<could not render decoded payload: " + ex.getClass().getSimpleName() + ">";
        }
        return payload.length() <= MAX_DEBUG_PAYLOAD_LENGTH
                ? payload
                : payload.substring(0, MAX_DEBUG_PAYLOAD_LENGTH) + "… [truncated]";
    }

    private static boolean hasDefaultToString(Class<?> type) {
        try {
            return type.getMethod("toString").getDeclaringClass() == Object.class;
        } catch (NoSuchMethodException ignored) {
            return true;
        }
    }

    private static String dumpFields(Object value, int depth) {
        // ponytail: cap reflection at three levels; deepen only if a captured schema requires it.
        Class<?> type = value.getClass();
        if (depth >= MAX_DEBUG_PAYLOAD_DEPTH) return "<" + type.getName() + ">";
        if (type.isArray()) return type.getName() + "[length=" + java.lang.reflect.Array.getLength(value) + "]";

        StringBuilder dump = new StringBuilder(type.getName()).append('{');
        boolean first = true;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                if (!first) dump.append(", ");
                dump.append(field.getName()).append('=');
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if (fieldValue == null) {
                        dump.append("null");
                    } else if (hasDefaultToString(fieldValue.getClass())) {
                        dump.append(dumpFields(fieldValue, depth + 1));
                    } else {
                        dump.append(fieldValue);
                    }
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    dump.append("<inaccessible: ").append(ex.getClass().getSimpleName()).append('>');
                }
                if (dump.length() >= MAX_DEBUG_PAYLOAD_LENGTH) {
                    dump.setLength(MAX_DEBUG_PAYLOAD_LENGTH);
                    return dump.append("… [truncated]").toString();
                }
                first = false;
            }
        }
        return dump.append('}').toString();
    }

    static boolean isShortsNotification(Object pushNotification) {
        if (pushNotification == null) return false;

        try {
            Class<?> pushClass = pushNotification.getClass();
            if (pushClass.getField("b").getInt(pushNotification) != PUSH_NOTIFICATION_RENDERER) return false;

            Object renderer = pushClass.getField("c").get(pushNotification);
            if (renderer == null) return false;
            Object navigationEndpoint = renderer.getClass().getField("f").get(renderer);
            if (navigationEndpoint == null) return false;

            Object extensionStore = navigationEndpoint.getClass().getField("l").get(navigationEndpoint);
            for (String extensionClassName : REEL_WATCH_ENDPOINT_EXTENSIONS) {
                try {
                    Object reelWatchEndpoint = Class.forName(extensionClassName).getField("b").get(null);
                    Object descriptor = reelWatchEndpoint.getClass().getField("d").get(reelWatchEndpoint);

                    for (Method method : extensionStore.getClass().getMethods()) {
                        Class<?>[] parameters = method.getParameterTypes();
                        if (method.getName().equals("n") && parameters.length == 1 && parameters[0].isInstance(descriptor)) {
                            if (Boolean.TRUE.equals(method.invoke(extensionStore, descriptor))) return true;
                        }
                    }
                } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                    // This generated extension may belong to the other supported YouTube schema.
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    Logger.printException(() -> "Shorts push notification endpoint check failed open", ex);
                    return false;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Logger.printException(() -> "Shorts push notification detection failed open", ex);
            return false;
        }
    }
}
