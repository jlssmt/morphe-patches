/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches;

import android.app.Activity;
import android.widget.Toast;

import java.lang.reflect.Method;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.settings.Settings;

public final class ShortsNotificationsPatch {
    // shortcut: assumes stable push proto fields and endpoint extension names, update if YouTube changes the schema.
    private static final int PUSH_NOTIFICATION_RENDERER = 77819057;
    private static final int MAX_DEBUG_PAYLOAD_LENGTH = 8000;
    private static final String[] REEL_WATCH_ENDPOINT_EXTENSIONS = {
            "defpackage.blie", // YouTube 21.42+
            "defpackage.bjts"  // YouTube 21.30
    };
    private static final Object HIDDEN_COUNT_LOCK = new Object();

    private ShortsNotificationsPatch() {
    }

    public static boolean shouldSuppressShortsNotification(Object pushNotification) {
        if (!Settings.DISABLE_SHORTS_NOTIFICATIONS.get()) return false;

        boolean isShorts = isShortsNotification(pushNotification);
        if (isShorts) {
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
        } else {
            Logger.printDebug(() -> {
                String message = "Shorts notification filter: ReelWatchEndpoint=false, decision=passed";
                return isPushNotificationRenderer(pushNotification)
                        ? message + ", decoded push payload=" + debugPayload(pushNotification)
                        : message;
            });
        }
        return isShorts;
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

    private static boolean isPushNotificationRenderer(Object pushNotification) {
        if (pushNotification == null) return false;
        try {
            return pushNotification.getClass().getField("b").getInt(pushNotification) == PUSH_NOTIFICATION_RENDERER;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static String debugPayload(Object pushNotification) {
        final String payload;
        try {
            payload = String.valueOf(pushNotification);
        } catch (RuntimeException ex) {
            return "<could not render decoded payload: " + ex.getClass().getSimpleName() + ">";
        }
        return payload.length() <= MAX_DEBUG_PAYLOAD_LENGTH
                ? payload
                : payload.substring(0, MAX_DEBUG_PAYLOAD_LENGTH) + "… [truncated]";
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
