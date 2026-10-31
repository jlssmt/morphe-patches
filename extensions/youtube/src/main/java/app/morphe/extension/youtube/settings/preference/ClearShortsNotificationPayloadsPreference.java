package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.io.IOException;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.ShortsNotificationsPatch;

@SuppressWarnings({"unused", "deprecation"})
public final class ClearShortsNotificationPayloadsPreference extends Preference {
    public ClearShortsNotificationPayloadsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ClearShortsNotificationPayloadsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ClearShortsNotificationPayloadsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClearShortsNotificationPayloadsPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceClickListener(preference -> {
            try {
                ShortsNotificationsPatch.clearCapturedPayloads(getContext());
                Utils.showToastShort(str("morphe_shorts_notification_payloads_cleared"));
            } catch (IOException | RuntimeException ex) {
                Logger.printException(() -> "Could not clear Shorts notification payload capture", ex);
                Utils.showToastShort(str("morphe_shorts_notification_payloads_clear_failed"));
            }
            return true;
        });
    }
}
