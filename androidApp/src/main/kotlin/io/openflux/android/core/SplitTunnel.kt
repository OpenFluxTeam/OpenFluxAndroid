package io.openflux.android.core

import io.openflux.desktop.model.SplitTunnelMode

/**
 * What the VPN builder must allow or disallow. Android takes either an allowlist or a disallowlist,
 * never both, so exactly one of [allowed] and [disallowed] is used for a mode.
 */
data class SplitTunnelPlan(
    val mode: SplitTunnelMode,
    val allowed: List<String> = emptyList(),
    val disallowed: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
    val problem: String? = null,
)

fun resolveSplitTunnel(
    mode: SplitTunnelMode,
    selected: List<String>,
    installed: Set<String>,
    selfPackage: String,
): SplitTunnelPlan {
    if (mode == SplitTunnelMode.Off) {
        return SplitTunnelPlan(mode = mode, disallowed = listOf(selfPackage))
    }
    val packages = selected.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { it != selfPackage }
        .distinct()
        .toList()
    val valid = packages.filter { it in installed }.sorted()
    val missing = packages.filterNot { it in installed }.sorted()
    return when (mode) {
        SplitTunnelMode.Off -> error("handled above")
        SplitTunnelMode.Exclude -> SplitTunnelPlan(
            mode = mode,
            disallowed = (valid + selfPackage).distinct().sorted(),
            missing = missing,
        )
        SplitTunnelMode.Include -> SplitTunnelPlan(
            mode = mode,
            allowed = valid,
            missing = missing,
            problem = if (valid.isEmpty()) "Выберите хотя бы одно установленное приложение" else null,
        )
    }
}
