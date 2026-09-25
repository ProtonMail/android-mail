/*
 * Copyright (c) 2026 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maildetail.presentation.ui

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.HiddenMessagesBannerState
import ch.protonmail.android.maildetail.presentation.ui.localcomposition.LocalIsMessageWebViewPoolEnabled
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebViewPool
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWeight
import kotlinx.collections.immutable.ImmutableList
import timber.log.Timber

/**
 * Creates and drives the conversation's message-body WebView pool, or returns null when pooling is off for
 * this device. Provide the result via [ch.protonmail.android.mailmessage.presentation.ui.LocalMessageBodyWebViewPool]
 * so each message body acquires its WebView from it.
 *
 * Keeps a small number of message-body WebViews alive across LazyColumn recycling, so scrolling back to a
 * message re-attaches its already-rendered WebView instead of rebuilding and reloading it (ET-6844). Tied to
 * this conversation: destroyed when the screen leaves composition or the conversation changes. Pooling trades
 * memory for smoothness, so it is only enabled where there is RAM to spare and scaled by device RAM; below the
 * threshold the pool is null, falling back to the legacy destroy-on-recycle - the safe choice on the
 * low-memory devices most prone to WebView OOMs.
 *
 * Owns three concerns: creation/disposal, memory-pressure trimming, and viewport-driven weight eviction.
 */
@Composable
internal fun rememberConversationMessageWebViewPool(
    conversationId: String,
    uiModels: ImmutableList<ConversationDetailMessageUiModel>,
    listState: LazyListState,
    loadedItemsHeight: Map<String, Int>,
    hiddenMessagesBannerState: HiddenMessagesBannerState
): MessageBodyWebViewPool? {
    val config = rememberWebViewPoolConfig()
    val pool = remember(conversationId, config.enabled) {
        if (config.enabled) MessageBodyWebViewPool(maxSize = config.maxSize) else null
    }
    DisposableEffect(pool) {
        onDispose { pool?.clear() }
    }

    // Best-effort backstop, NOT the primary safety: the weight budget and the absolute instance cap
    // (POOL_MAX_INSTANCES) bound live WebViews up front, which is what keeps foreground memory in check. This
    // only drops off-screen retained WebViews when the system signals pressure. Note the foreground
    // TRIM_MEMORY_RUNNING_* levels are no longer delivered on Android 14+ (API 34), so on modern devices this
    // fires mainly on UI-hidden / backgrounded trims (still >= TRIM_MEMORY_RUNNING_LOW) rather than live
    // foreground pressure - hence "backstop". Attached (visible) bodies are never touched.
    val trimContext = LocalContext.current
    DisposableEffect(pool, trimContext) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) pool?.trimToFloor(WEBVIEW_POOL_TRIM_FLOOR)
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            override fun onLowMemory() {
                pool?.trimToFloor(WEBVIEW_POOL_TRIM_FLOOR)
            }
        }
        trimContext.registerComponentCallbacks(callbacks)
        onDispose { trimContext.unregisterComponentCallbacks(callbacks) }
    }

    // Keep the expanded messages nearest the viewport alive within a RAM-scaled weight budget and destroy the
    // rest: distance (pixels from the viewport edges) ranks them nearest-first, and each message's weight
    // (MessageBodyWeight - light/medium/heavy/huge) is charged against the budget, so a few heavy bodies or
    // many light ones are kept for the same memory. Collapsed messages are never candidates. Bounds live
    // WebViews for memory while biasing retention to what the user is about to scroll back to (ET-6844).
    val messageHeaderCount = 1 + if (hiddenMessagesBannerState is HiddenMessagesBannerState.Shown) 1 else 0
    val collapsedHeightEstimatePx = with(LocalDensity.current) { CollapsedHeightEstimate.roundToPx() }
    LaunchedEffect(pool, uiModels, messageHeaderCount, collapsedHeightEstimatePx) {
        pool ?: return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                val layoutInfo = listState.layoutInfo
                if (layoutInfo.visibleItemsInfo.isEmpty()) return@collect
                pool.evictByWeight(
                    distanceByKey = expandedDistances(
                        uiModels, messageHeaderCount, layoutInfo, loadedItemsHeight, collapsedHeightEstimatePx
                    ),
                    weightByKey = expandedWeights(uiModels, loadedItemsHeight),
                    budget = config.budget,
                    // Keep the nearest few expanded bodies (current + neighbours) by rank, not pixels: a long
                    // body is taller than the viewport, so a pixel window would not span a neighbour, and the
                    // ones being scrolled through must not be destroyed and reloaded (blank/jump - ET-6844).
                    retainNearestCount = config.retainNearest
                )
            }
    }

    return pool
}

/**
 * Each expanded message's pixel distance from the viewport edges (0 when any part of it is on screen),
 * keyed by message id. Off-screen items are placed by walking [loadedItemsHeight] (expanded message
 * heights, already in px) and [collapsedHeightEstimatePx] out from the nearest visible item, so the
 * distance follows real pixels - a single tall expanded message counts for a whole screen, several small
 * ones barely count. The estimate only has to be accurate near the threshold; far items are evicted
 * regardless of a rough figure.
 */
private fun expandedDistances(
    uiModels: ImmutableList<ConversationDetailMessageUiModel>,
    messageHeaderCount: Int,
    layoutInfo: LazyListLayoutInfo,
    loadedItemsHeight: Map<String, Int>,
    collapsedHeightEstimatePx: Int
): Map<String, Int> {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return emptyMap()

    val first = visible.first()
    val last = visible.last()
    val viewportTop = layoutInfo.viewportStartOffset
    val viewportBottom = layoutInfo.viewportEndOffset
    val viewportHeight = viewportBottom - viewportTop
    val visibleByIndex = visible.associateBy { it.index }

    fun heightOf(lazyIndex: Int): Int {
        visibleByIndex[lazyIndex]?.let { return it.size }
        return when (val ui = uiModels.getOrNull(lazyIndex - messageHeaderCount)) {
            null -> collapsedHeightEstimatePx // subject header or hidden-messages banner
            is ConversationDetailMessageUiModel.Expanded ->
                loadedItemsHeight[ui.messageId.id] ?: viewportHeight
            else -> collapsedHeightEstimatePx
        }
    }

    val distances = mutableMapOf<String, Int>()
    uiModels.forEachIndexed { messageIndex, ui ->
        if (ui !is ConversationDetailMessageUiModel.Expanded) return@forEachIndexed
        val lazyIndex = messageIndex + messageHeaderCount

        val distance = when {
            lazyIndex in first.index..last.index -> 0 // on screen

            lazyIndex < first.index -> {
                var between = 0
                for (idx in lazyIndex + 1 until first.index) between += heightOf(idx)
                val itemBottom = first.offset - between
                (viewportTop - itemBottom).coerceAtLeast(0)
            }

            else -> {
                var between = 0
                for (idx in last.index + 1 until lazyIndex) between += heightOf(idx)
                val itemTop = last.offset + last.size + between
                (itemTop - viewportBottom).coerceAtLeast(0)
            }
        }
        distances[ui.messageId.id] = distance
    }
    return distances
}

/**
 * Each expanded message's coarse memory-cost weight (see [MessageBodyWeight]), keyed by message id, for the
 * pool's weight-budget eviction. Uses the body's rendered height ([loadedItemsHeight], 0 if not measured yet)
 * as one classification input.
 */
private fun expandedWeights(
    uiModels: ImmutableList<ConversationDetailMessageUiModel>,
    loadedItemsHeight: Map<String, Int>
): Map<String, Int> = uiModels
    .filterIsInstance<ConversationDetailMessageUiModel.Expanded>()
    .associate { ui ->
        val heightPx = loadedItemsHeight[ui.messageId.id] ?: 0
        ui.messageId.id to MessageBodyWeight.of(ui.messageBodyUiModel.messageBody, heightPx).cost
    }

/**
 * Whether message-body WebViews are pooled on this device, and if so the retention weight budget, the number of
 * nearest expanded bodies always kept, and the hard instance cap.
 */
private data class WebViewPoolConfig(
    val enabled: Boolean,
    val budget: Int,
    val retainNearest: Int,
    val maxSize: Int
)

/**
 * Pooling policy for this device: disabled (legacy destroy-on-recycle) on low-RAM devices, otherwise enabled
 * with a RAM-scaled weight budget. Retaining WebViews costs memory, and the low-memory devices that would
 * benefit most from fewer reloads are exactly the ones that OOM from holding them, so they opt out.
 */
@Composable
private fun rememberWebViewPoolConfig(): WebViewPoolConfig {
    val featureEnabled = LocalIsMessageWebViewPoolEnabled.current
    val context = LocalContext.current
    return remember(context, featureEnabled) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val totalRamGb = ActivityManager.MemoryInfo()
            .also { activityManager.getMemoryInfo(it) }
            .totalMem / BYTES_PER_GB
        val budget = when {
            activityManager.isLowRamDevice || totalRamGb < POOL_MIN_RAM_GB -> 0
            totalRamGb < POOL_MID_RAM_GB -> POOL_WEIGHT_BUDGET_LOW_RAM
            totalRamGb < POOL_HIGH_RAM_GB -> POOL_WEIGHT_BUDGET_MID_RAM
            else -> POOL_WEIGHT_BUDGET_HIGH_RAM
        }
        // Nearest expanded bodies always kept regardless of the weight budget (current + neighbours), so
        // scrolling between long messages hits a retained body instead of a reload. Scaled by RAM tier, capped
        // lower on low-RAM to bound the extra Huge bodies held there.
        val retainNearest = when {
            activityManager.isLowRamDevice || totalRamGb < POOL_MIN_RAM_GB -> 0
            totalRamGb < POOL_MID_RAM_GB -> POOL_RETAIN_NEAREST_LOW_RAM
            totalRamGb < POOL_HIGH_RAM_GB -> POOL_RETAIN_NEAREST_MID_RAM
            else -> POOL_RETAIN_NEAREST_HIGH_RAM
        }
        // Off unless the feature flag is on AND the device RAM tier qualifies.
        val config = WebViewPoolConfig(
            enabled = featureEnabled && budget > 0,
            budget = budget,
            retainNearest = retainNearest,
            maxSize = minOf(budget + POOL_LRU_HEADROOM, POOL_MAX_INSTANCES)
        )
        when {
            config.enabled -> Timber.d(
                "WebViewPool: config budget=%d retainNearest=%d maxSize=%d ram=%.1fGB",
                config.budget, config.retainNearest, config.maxSize, totalRamGb
            )
            !featureEnabled -> Timber.d("WebViewPool: disabled (flag off)")
            else -> Timber.d("WebViewPool: disabled (low RAM %.1fGB)", totalRamGb)
        }
        config
    }
}

// Rough height for a collapsed message / the subject header when estimating an off-screen item's distance.
private val CollapsedHeightEstimate = 72.dp

private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0
// totalMem reports below the nominal spec (kernel/GPU carve-out), so a device reads ~0.2-0.5 GB under its
// marketing size (a "6 GB" phone reads ~5.5-5.8 GB); the tier cutoffs sit below the round numbers to match.
private const val POOL_MIN_RAM_GB = 3.5 // below this (or a low-RAM device): no pool, destroy on recycle
private const val POOL_MID_RAM_GB = 5.5
private const val POOL_HIGH_RAM_GB = 7.5
// Retention budget in MessageBodyWeight cost units (Light=1 .. Huge=8), scaled by device RAM. A thread of
// light bodies keeps ~budget of them; a heavy or huge body eats several units, so the number kept shrinks as
// the content gets heavier and memory tracks weight instead of headcount.
//
// Measured renderer footprint (dumpsys meminfo, single shared renderer process, on the test emulators):
//   - baseline with one light body visible:            ~60-75 MB
//   - each additional RETAINED (off-screen) body:      ~+10 MB for a tall body, ~+1-2 MB for a light one
//                                                       (Chromium purges an off-screen body's decoded images,
//                                                        so it holds ~its DOM, not a second full render)
//   - the one VISIBLE body dominates the peak:          a ~20k px tall body renders at ~120-130 MB; a light
//                                                       body adds ~10-15 MB over baseline
//   => peak renderer ~= baseline + one visible body + ~10 MB x (retained bodies)
// So raising the budget to keep one more tall body cost only ~10 MB of peak (keep-1 ~160 MB -> keep-2 ~170 MB),
// which is why the budgets are generous relative to a naive "N x visible cost" estimate.
//
// Sized as multiples of the Huge cost so each tier keeps one more tall body than the one below - low 1, mid 2,
// high 3. Light-mail retention is additionally bounded by POOL_MAX_INSTANCES.
private const val POOL_WEIGHT_BUDGET_LOW_RAM = 8 // 3.5 GB up to 5.5 GB: keep 1 tall body
private const val POOL_WEIGHT_BUDGET_MID_RAM = 16 // 5.5 GB up to 7.5 GB: keep 2 tall bodies
private const val POOL_WEIGHT_BUDGET_HIGH_RAM = 24 // 7.5 GB and above: keep 3 tall bodies
// Nearest expanded bodies always kept by rank (current + neighbours), regardless of weight, so scrolling one
// message at a time always lands on a retained body. A long body is taller than the viewport, so a pixel window
// cannot span a neighbour - this is the count that actually stops the blank/reload. Capped lower on low-RAM to
// bound the extra Huge bodies held (each ~+10 MB); mid/high can afford more of the scroll-back window.
private const val POOL_RETAIN_NEAREST_LOW_RAM = 2 // current + 1 neighbour
private const val POOL_RETAIN_NEAREST_MID_RAM = 3 // current + 1 each side
private const val POOL_RETAIN_NEAREST_HIGH_RAM = 4
// Headroom for the pool's LRU safety cap above the budget, absorbing transient over-composition during a fast
// expand-all or fling before weight-budget eviction settles the live set.
private const val POOL_LRU_HEADROOM = 2
// Absolute ceiling on live WebViews, independent of the weight budget. Even a light body's WebView is not free
// - it holds a renderer document, a view tree and native buffers - so instance count is its own memory axis,
// separate from the weight budget. This caps it at roughly the useful scroll-back window (a screenful-plus of
// messages); retaining more has diminishing returns and only adds pressure. Set to the mid tier's full
// allowance (budget + headroom) so it never clips the low/mid tiers, and bounds the high tier - and any future
// budget increase - from accumulating a large number of live views.
private const val POOL_MAX_INSTANCES = 12
// Under system memory pressure, trim the pool down to at most this many bodies (off-screen ones destroyed).
private const val WEBVIEW_POOL_TRIM_FLOOR = 1
