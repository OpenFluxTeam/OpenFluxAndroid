package io.openflux.android.core

import io.openflux.desktop.model.SplitTunnelMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SplitTunnelPlanTest {
    @Test
    fun offIsEverythingThroughTheVpnButThisApp() {
        val plan = resolveSplitTunnel(SplitTunnelMode.Off, listOf("com.a"), setOf("com.a"), "io.openflux.client")
        assertEquals(emptyList(), plan.allowed)
        assertEquals(listOf("io.openflux.client"), plan.disallowed)
        assertEquals(emptyList(), plan.missing)
        assertNull(plan.problem)
    }

    @Test
    fun excludeSkipsMissingNormalizesAndKeepsThisAppOut() {
        val plan = resolveSplitTunnel(
            SplitTunnelMode.Exclude,
            listOf(" com.b ", "", "com.a", "com.a", "io.openflux.client", "com.gone"),
            setOf("com.a", "com.b"),
            "io.openflux.client",
        )
        assertEquals(emptyList(), plan.allowed)
        assertEquals(listOf("com.a", "com.b", "io.openflux.client"), plan.disallowed)
        assertEquals(listOf("com.gone"), plan.missing)
        assertNull(plan.problem)
    }

    @Test
    fun includeAllowsOnlyInstalledAppsAndNeverThisApp() {
        val plan = resolveSplitTunnel(
            SplitTunnelMode.Include,
            listOf("com.b", "io.openflux.client", "com.gone", "com.a"),
            setOf("com.a", "com.b"),
            "io.openflux.client",
        )
        assertEquals(listOf("com.a", "com.b"), plan.allowed)
        assertEquals(emptyList(), plan.disallowed)
        assertEquals(listOf("com.gone"), plan.missing)
        assertNull(plan.problem)
    }

    @Test
    fun anEmptyOrFullyMissingIncludeIsAProblem() {
        val empty = resolveSplitTunnel(SplitTunnelMode.Include, emptyList(), emptySet(), "io.openflux.client")
        assertEquals(emptyList(), empty.allowed)
        assertEquals("Выберите хотя бы одно установленное приложение", empty.problem)

        val missing = resolveSplitTunnel(
            SplitTunnelMode.Include,
            listOf("com.gone", "io.openflux.client"),
            emptySet(),
            "io.openflux.client",
        )
        assertEquals(emptyList(), missing.allowed)
        assertEquals(listOf("com.gone"), missing.missing)
        assertEquals("Выберите хотя бы одно установленное приложение", missing.problem)
    }
}
