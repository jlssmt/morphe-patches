package app.morphe.patches.youtube.misc.debugging

import app.morphe.patches.shared.misc.debugging.enableDebuggingPatch
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.playservice.is_20_40_or_greater
import app.morphe.patches.youtube.misc.playservice.is_20_41_or_greater
import app.morphe.patches.youtube.misc.playservice.is_21_42_or_greater
import app.morphe.patches.youtube.misc.playservice.versionCheckPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

@Suppress("unused")
val enableDebuggingPatch = enableDebuggingPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            versionCheckPatch
        )

        compatibleWith(COMPATIBILITY_YOUTUBE)
    },
    executeBlock = { },
    hookIndexedBooleanFeatureFlag = { is_21_42_or_greater },
    hookStringFeatureFlag = { true },
    // 20.40 has changes not worth supporting.
    hookLongFeatureFlag = { !is_20_40_or_greater || is_20_41_or_greater},
    hookDoubleFeatureFlag = { !is_20_40_or_greater || is_20_41_or_greater},
    preferenceScreen = PreferenceScreen.MISC,
    additionalDebugPreferences = listOf(
        SwitchPreference("morphe_debug_protobuffer", summary = true),
        SwitchPreference("morphe_capture_shorts_notification_payloads", summary = true),
        NonInteractivePreference(
            "morphe_export_shorts_notification_payloads",
            tag = "app.morphe.extension.youtube.settings.preference.ExportShortsNotificationPayloadsPreference",
            selectable = true
        ),
        NonInteractivePreference(
            "morphe_clear_shorts_notification_payloads",
            tag = "app.morphe.extension.youtube.settings.preference.ClearShortsNotificationPayloadsPreference",
            selectable = true
        )
    )
)
