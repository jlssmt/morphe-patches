package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.io.IOException;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.morphe.extension.youtube.patches.ShortsNotificationsPatch;

@SuppressWarnings({"unused", "deprecation"})
public final class ExportShortsNotificationPayloadsPreference extends Preference {
    public ExportShortsNotificationPayloadsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ExportShortsNotificationPayloadsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ExportShortsNotificationPayloadsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExportShortsNotificationPayloadsPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceClickListener(preference -> {
            final String payloads;
            try {
                payloads = ShortsNotificationsPatch.readCapturedPayloads(getContext());
            } catch (IOException | RuntimeException ex) {
                Logger.printException(() -> "Could not read Shorts notification payload capture", ex);
                Utils.showToastShort(str("morphe_shorts_notification_payloads_export_failed"));
                return true;
            }

            if (payloads.isEmpty()) {
                Utils.showToastShort(str("morphe_shorts_notification_payloads_empty"));
                return true;
            }

            AbstractPreferenceFragment fragment = AbstractPreferenceFragment.instance.get();
            if (fragment == null) {
                Utils.showToastShort(str("morphe_shorts_notification_payloads_export_failed"));
                return true;
            }

            fragment.exportTextActivity("Shorts_Notification_Payloads.txt", payloads, success ->
                    Utils.showToastShort(success
                            ? str("morphe_shorts_notification_payloads_exported")
                            : str("morphe_shorts_notification_payloads_export_failed")));
            return true;
        });
    }
}
