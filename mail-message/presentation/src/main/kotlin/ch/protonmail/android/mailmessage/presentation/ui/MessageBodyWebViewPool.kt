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

package ch.protonmail.android.mailmessage.presentation.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.staticCompositionLocalOf
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import timber.log.Timber

/**
 * A bounded, LRU pool of message-body [ZoomableWebView]s, keyed by message id.
 *
 * Why: a conversation renders each message body in a WebView inside a LazyColumn. When a card scrolls
 * out of the list's retention buffer the composable is disposed and the WebView destroyed, so scrolling
 * back builds a fresh WebView and reloads the body from scratch. On a slow device that reload blanks the
 * message and repaints over ~2s - the "jump" reported in ET-6844 that no amount of height-pinning can fix,
 * because the content genuinely reloads.
 *
 * The pool keeps a small number of rendered WebViews alive across recycling, so scrolling back re-attaches
 * an already-rendered view: no reload, no blank, no jump. Retention is driven by [evictByWeight] - the
 * screen keeps the expanded messages nearest the viewport within a memory budget and destroys the rest -
 * with an LRU cap ([evictIfNeeded]) as a last-resort safety net so a long conversation can never hold an
 * unbounded number of (heavy) WebViews.
 *
 * Not thread-safe: only touched from the main thread (composition + dispose).
 */
class MessageBodyWebViewPool(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {

    private data class Entry(val webView: ZoomableWebView, val loadedContent: LoadedContent?)

    /**
     * The message content a pooled WebView currently holds. Compared by value on re-attach: if it matches the
     * content about to be shown, the load is skipped (that reload is the blank-and-repaint the pool removes).
     */
    data class LoadedContent(val body: MessageBodyContent, val viewModePreference: ViewModePreference)

    // accessOrder = true makes iteration order least-recently-accessed first, so the eldest entry to evict
    // is always the first one.
    private val entries = object : LinkedHashMap<String, Entry>(0, LOAD_FACTOR, true) {}

    /** A point-in-time view of the pool, for diagnostics/telemetry (e.g. on renderer death). */
    data class Snapshot(val size: Int, val attached: Int, val detached: Int, val maxSize: Int)

    /** Snapshots the current pool: total held, attached (visible) vs detached (cached), and the cap. */
    fun snapshot(): Snapshot {
        val attached = entries.values.count { it.webView.parent != null }
        return Snapshot(size = entries.size, attached = attached, detached = entries.size - attached, maxSize = maxSize)
    }

    /**
     * Returns the pooled WebView for [key], creating one with [create] on a miss. Acquiring counts as an
     * access for LRU purposes, so the view a caller is about to show is never the one evicted.
     */
    fun acquire(key: String, create: () -> ZoomableWebView): ZoomableWebView {
        entries[key]?.let { return it.webView }

        val webView = create()
        entries[key] = Entry(webView, loadedContent = null)
        evictIfNeeded(protectedKey = key)
        Timber.d("WebViewPool: create key=%s size=%d/%d", key, entries.size, maxSize)
        return webView
    }

    /** The content last loaded into [key]'s WebView, or null if it has never been loaded. */
    fun loadedContent(key: String): LoadedContent? = entries[key]?.loadedContent

    /** Records that [key]'s WebView now holds the given [content]. */
    fun markLoaded(key: String, content: LoadedContent) {
        val existing = entries[key] ?: return
        entries[key] = existing.copy(loadedContent = content)
    }

    /**
     * Keeps the expanded messages nearest the viewport alive within a memory [budget] and destroys the rest.
     * [distanceByKey] holds exactly the currently expanded messages, mapped to each one's pixel distance from
     * the viewport edges (0 = visible), and ranks candidates nearest-first. [weightByKey] gives each message's
     * coarse memory cost (see [MessageBodyWeight]); starting from the nearest, messages are kept while their
     * cumulative weight stays within [budget], so a few heavy bodies OR many light ones are retained for the
     * same memory. The nearest message is always kept even if it alone exceeds the budget. Only expanded
     * messages are keep candidates - a key absent from [distanceByKey] is a collapsed message whose body no
     * longer renders, so it is never retained (collapsing every message drains the pool to zero). Only
     * *detached* (off-screen) WebViews are destroyed; attached (visible) ones are never touched.
     */
    fun evictByWeight(
        distanceByKey: Map<String, Int>,
        weightByKey: Map<String, Int>,
        budget: Int
    ) {
        if (entries.isEmpty()) return
        val ordered = entries.keys
            .filter { it in distanceByKey }
            .sortedBy { distanceByKey.getValue(it) }

        val keep = mutableSetOf<String>()
        var used = 0
        for (key in ordered) {
            val cost = weightByKey[key] ?: DEFAULT_WEIGHT
            // Keep this body if it fits; keep scanning rather than break, so a farther cheaper body can still
            // fit after a heavy one. keep.isEmpty() also keeps the nearest when a body alone exceeds the budget
            // - a dormant guard today (max weight <= min budget), live only if a body's cost ever exceeds it.
            if (keep.isEmpty() || used + cost <= budget) {
                keep += key
                used += cost
            }
        }

        val toDestroy = entries.entries.filter { (key, entry) -> key !in keep && entry.webView.parent == null }
        toDestroy.forEach { entry ->
            entries.remove(entry.key)
            destroy(entry.value.webView)
        }
        if (toDestroy.isNotEmpty()) {
            Timber.d(
                "WebViewPool: evict destroyed=%d kept=%d budget=%d size=%d",
                toDestroy.size, keep.size, budget, entries.size
            )
        }
    }

    /**
     * Reacts to the shared renderer process dying (via WebViewClient.onRenderProcessGone). Every WebView in
     * the app is backed by that single renderer, so once it is gone the entire pool is defunct - none of the
     * cached views can be re-attached or reused. An OS kill (the usual cause, [deadView] not having crashed)
     * is also the strongest memory-pressure signal we still get on Android 14+, where the foreground
     * TRIM_MEMORY_RUNNING_* callbacks no longer fire. So this releases the whole cache rather than dropping a
     * single view: it destroys every pooled WebView and empties the pool, freeing memory immediately.
     * [deadView] is destroyed too even if it was never pooled (the legacy, non-pooled path).
     *
     * The currently attached (visible) message is destroyed here as well - it is dead too - and recovers by
     * building a fresh WebView on its next composition.
     */
    fun onRenderProcessGone(deadView: WebView) {
        // Act only if this exact dead view is still pooled. A renderer death fires one callback per affected
        // WebView; the first purges the whole pool (all cached views share the dead renderer). Later/stale
        // callbacks - and any for a replacement acquired since - find their view gone and no-op, so the handler
        // is idempotent and can't destroy a replacement.
        if (entries.values.none { it.webView === deadView }) return
        entries.values.forEach { destroy(it.webView) }
        entries.clear()
    }

    /** Destroys every pooled WebView. Call when the conversation screen leaves composition. */
    fun clear() {
        val size = entries.size
        entries.values.forEach { destroy(it.webView) }
        entries.clear()
        if (size > 0) Timber.d("WebViewPool: clear size=%d", size)
    }

    /**
     * Destroys off-screen WebViews down to at most [floor], on system memory pressure. The weight budget is
     * only an estimate and the decoded-image cost lives in the renderer process we cannot measure, so this
     * honours the real `onTrimMemory` signal. Least-recently-used first; attached (visible) views are never
     * destroyed, so [floor] may not be reached if that many bodies are on screen.
     */
    fun trimToFloor(floor: Int) {
        if (entries.size <= floor) return
        // entries iterates least-recently-accessed first (accessOrder), so the oldest detached views go first.
        val detachedLruFirst = entries.entries.filter { it.value.webView.parent == null }
        val toEvict = (entries.size - floor).coerceAtMost(detachedLruFirst.size)
        if (toEvict <= 0) return
        detachedLruFirst.take(toEvict).forEach { entry ->
            entries.remove(entry.key)
            destroy(entry.value.webView)
        }
        Timber.d("WebViewPool: trim floor=%d destroyed=%d size=%d", floor, toEvict, entries.size)
    }

    private fun evictIfNeeded(protectedKey: String) {
        while (entries.size > maxSize) {
            // Only evict a WebView that is not currently attached to the view hierarchy: a detached view is
            // off-screen (its item was disposed), so destroying it is safe. If every over-limit entry is still
            // attached (e.g. many small messages visible at once), let the pool grow rather than destroy a
            // visible WebView - the excess is bounded by how many messages fit on screen.
            val evictable = entries.entries.firstOrNull {
                it.key != protectedKey && it.value.webView.parent == null
            } ?: break
            entries.remove(evictable.key)
            destroy(evictable.value.webView)
        }
    }

    // WebView teardown: detach from the parent so the view tree can't pin the host Activity, stop in-flight
    // loading, then destroy. No loadUrl("about:blank") first - that starts an async navigation whose
    // loadingStateChanged callback then lands on the already-destroyed view ("call on a destroyed WebView");
    // blanking the page is for a WebView you keep alive, not one being destroyed right away.
    private fun destroy(webView: ZoomableWebView) {
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }.onFailure { Timber.e(it, "Failed to destroy pooled webView") }
    }

    private companion object {

        const val DEFAULT_MAX_SIZE = 6
        const val LOAD_FACTOR = 0.75f

        // Weight assumed for a key with no entry in weightByKey (treat unknown as lightest).
        const val DEFAULT_WEIGHT = 1
    }
}

/**
 * The pool in scope for the current message list, or null when bodies are not pooled (e.g. the full-message
 * screen, which shows a single body and never recycles). A null pool means the legacy behaviour: build a
 * WebView per composable and destroy it on dispose.
 */
val LocalMessageBodyWebViewPool = staticCompositionLocalOf<MessageBodyWebViewPool?> { null }
