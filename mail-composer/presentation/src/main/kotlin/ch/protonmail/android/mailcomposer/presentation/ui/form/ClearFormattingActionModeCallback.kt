/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailcomposer.presentation.ui.form

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import ch.protonmail.android.mailcomposer.presentation.R

internal class ClearFormattingActionModeCallback(
    private val webView: WebView,
    private val wrapped: ActionMode.Callback
) : ActionMode.Callback2() {

    private var clearFormattingRequested = false

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val created = wrapped.onCreateActionMode(mode, menu)
        addClearFormattingItem(menu)
        // Android WebView doesn't reliably emit selectionchange during touch
        // drag selection, so we explicitly snapshot the live range now.
        // The floating toolbar is only shown when a non-collapsed selection exists.
        webView.evaluateJavascript("__protonCaptureSelection();", null)
        return created
    }

    // Chromium's WebView callback clears the menu in onPrepareActionMode and
    // re-populates it from scratch on every selection change, so we must
    // re-inject our item here, or it disappears.
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        wrapped.onPrepareActionMode(mode, menu)
        addClearFormattingItem(menu)
        // Keep the cache in sync if Chromium refreshes the toolbar because
        // the user adjusted the selection handles.
        webView.evaluateJavascript("__protonCaptureSelection();", null)
        return true
    }

    private fun addClearFormattingItem(menu: Menu) {
        if (menu.findItem(MENU_ITEM_ID) != null) return
        menu.add(
            Menu.NONE,
            MENU_ITEM_ID,
            Menu.NONE,
            webView.context.getString(R.string.composer_action_clear_formatting)
        )
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == MENU_ITEM_ID) {
            clearFormattingRequested = true
            webView.evaluateJavascript("clearSelectionFormatting();", null)
            mode.finish()
            return true
        }
        return wrapped.onActionItemClicked(mode, item)
    }

    // Invalidate the JS-side selection cache when the action mode dismisses
    // for any reason other than our own item firing, otherwise a future
    // ActionMode could resurrect a stale range and clear it.
    override fun onDestroyActionMode(mode: ActionMode) {
        wrapped.onDestroyActionMode(mode)
        if (!clearFormattingRequested) {
            webView.evaluateJavascript("__protonInvalidateSelectionCache();", null)
        }
    }

    override fun onGetContentRect(
        mode: ActionMode?,
        view: View?,
        outRect: Rect?
    ) {
        if (wrapped is ActionMode.Callback2) {
            wrapped.onGetContentRect(mode, view, outRect)
        } else {
            super.onGetContentRect(mode, view, outRect)
        }
    }

    private companion object {

        private const val MENU_ITEM_ID = 0x0FFF0001
    }
}
