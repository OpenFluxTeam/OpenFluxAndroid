package io.openflux.android.core

import android.content.Context
import io.openflux.bridge.mobile.Mobile
import io.openflux.desktop.data.FileScriptRepository
import io.openflux.desktop.model.ScriptSource
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * The Android app's registry of installed JS script transports. The core is a
 * library here, so reading a transport is an in-process call; the scripts
 * shipped in the APK (assets/scripts) are installed the first time the experimental
 * features are turned on and replace the installed copies when a build brings newer ones.
 */
class AndroidScriptRepository(context: Context) :
    FileScriptRepository(File(context.applicationContext.filesDir, "scripts"), { data, sig, key -> Mobile.inspectTransport(data, sig, key) }) {

    private val appContext = context.applicationContext

    val officialKey: String get() = Mobile.officialScriptKey()

    /**
     * Installs the scripts shipped in the APK, or upgrades the installed copies of them. Not at
     * construction: it runs the script engine, so it waits for the user to turn the experimental
     * features on (see OpenFluxApplication).
     */
    fun syncBundled() {
        if (scripts.value.isEmpty()) installBundled() else upgradeBundled()
    }

    /** The on-disk file + pinned key for a carrier; null when the script is gone. */
    internal fun carrier(id: String): CoreSpecs.ScriptCarrier? {
        val s = byId(id) ?: return null
        return CoreSpecs.ScriptCarrier(File(dir, s.fileName).absolutePath, s.pubkeyHex, s.id, s.primaryParam?.key)
    }

    /** The (script, detached signature) pairs shipped with this build. */
    private fun shipped(): List<Triple<String, ByteArray, ByteArray>> {
        val assets = appContext.assets
        val names = runCatching { assets.list("scripts")?.toList().orEmpty() }.getOrDefault(emptyList()).filter { it.endsWith(".js") }
        return names.mapNotNull { file ->
            runCatching {
                val js = assets.open("scripts/$file").use { it.readBytes() }
                val sig = assets.open("scripts/$file.sig").use { it.readBytes() }
                Triple(file, js, sig)
            }.getOrNull()
        }
    }

    private fun installBundled() {
        for ((file, js, sig) in shipped()) {
            runCatching { install(js, sig, officialKey, ScriptSource.Bundled, "bundled:$file", now = 0L) }
        }
    }

    /**
     * The scripts shipped with this build replace the installed copies of the
     * same bundled script when they are newer: a bundled script has no update
     * address of its own, so a new build is how it gets fixed. Only what is
     * installed is touched (a script the user deleted stays deleted), a script
     * the user switched off stays off, and one they added themselves under the
     * same name is left alone.
     */
    private fun upgradeBundled() {
        for ((file, js, sig) in shipped()) {
            runCatching {
                val report = report(js, sig, officialKey)?.takeIf { it.isValid() } ?: return@runCatching
                val name = report["name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
                val shippedVersion = report["version"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val installed = byId(name) ?: return@runCatching
                if (installed.source != ScriptSource.Bundled) return@runCatching
                if (Mobile.compareScriptVersions(shippedVersion, installed.version) <= 0) return@runCatching
                val wasEnabled = installed.enabled
                install(js, sig, officialKey, ScriptSource.Bundled, "bundled:$file", now = installed.addedAt)
                if (!wasEnabled) setEnabled(name, false)
            }
        }
    }
}
