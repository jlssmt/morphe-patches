/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.misc.notifications

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.dex.SdkDexVerifier
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory

fun main(arguments: Array<String>) {
    require(arguments.size == 1) { "Usage: ShortsNotificationsApkBuildKt <stock-youtube-apk>" }
    val input = File(arguments.single()).canonicalFile
    require(input.isFile) { "Stock APK does not exist: $input" }
    val sdk = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        ?: error("ANDROID_HOME or ANDROID_SDK_ROOT is required for SDK DEX verification")

    Patcher(
        PatcherConfig(
            input,
            createTempDirectory("shorts-notifications").toFile(),
            verifier = SdkDexVerifier(File(sdk)),
            validate = true,
        )
    ).use { patcher ->
        patcher += setOf(shortsNotificationsPatch)
        runBlocking {
            patcher().collect { result ->
                result.exception?.let { throw it }
                println("Applied: ${result.patch.name}")
            }
        }
        patcher.get()
        println("Shorts notification patch applied and patched DEX verified for $input")
    }
}
