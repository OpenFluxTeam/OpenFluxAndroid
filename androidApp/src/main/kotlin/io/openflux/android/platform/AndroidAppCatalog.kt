package io.openflux.android.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import io.openflux.desktop.service.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** The apps installed for this user, for Android's split-tunnel picker. */
class AndroidAppCatalog(private val context: Context) {
    private val packageManager: PackageManager get() = context.packageManager

    suspend fun installed(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val collator = Collator.getInstance(Locale.getDefault())
        val byName = Comparator<InstalledApp> { a, b ->
            val byLabel = collator.compare(a.label, b.label)
            if (byLabel != 0) byLabel else a.packageName.compareTo(b.packageName)
        }
        installedApplications()
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter(::isCurrentUserApp)
            .map { info ->
                val label = runCatching { packageManager.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
                    .ifBlank { info.packageName }
                InstalledApp(
                    packageName = info.packageName,
                    label = label,
                    system = info.isSystem,
                    icon = icon(info),
                )
            }
            .sortedWith(byName)
            .toList()
    }

    suspend fun isInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val info = applicationInfo(packageName) ?: return@withContext false
        isCurrentUserApp(info)
    }

    private fun installedApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            modernInstalledApplications()
        } else {
            legacyInstalledApplications()
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun modernInstalledApplications(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))

    @Suppress("DEPRECATION")
    private fun legacyInstalledApplications(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(0)

    private fun applicationInfo(packageName: String): ApplicationInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            modernApplicationInfo(packageName)
        } else {
            legacyApplicationInfo(packageName)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun modernApplicationInfo(packageName: String): ApplicationInfo? = runCatching {
        packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun legacyApplicationInfo(packageName: String): ApplicationInfo? = runCatching {
        packageManager.getApplicationInfo(packageName, 0)
    }.getOrNull()

    private fun isCurrentUserApp(info: ApplicationInfo): Boolean =
        info.enabled && (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0

    private val ApplicationInfo.isSystem: Boolean
        get() = (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

    private fun icon(info: ApplicationInfo): ImageBitmap? = runCatching {
        val drawable = info.loadIcon(packageManager)
        val bitmap = createBitmap(64, 64)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()
}
