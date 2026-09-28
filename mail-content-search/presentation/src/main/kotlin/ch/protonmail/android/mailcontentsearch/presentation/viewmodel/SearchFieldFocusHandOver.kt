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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

/**
 * Decides when the search field takes its focus, and so raises the keyboard, on its own.
 *
 * Held by [ContentSearchViewModel] rather than the composition. Opening a message or the set-up page
 * tears the screen's composition (and any `remember`) down, while the ViewModel outlives it on the
 * back stack.
 */
class SearchFieldFocusHandOver {

    private var hasAutoFocused = false

    private var isAwaitingSetUpReturn = false

    /**
     * Whether the field still has its first focus to take. True only once: coming back from a message,
     * leaving selection mode or rotating all re-create the field, and re-focusing then would cover the
     * results the user came back to read.
     *
     * The screen decides when to spend it. With no banner to show that is on open; with one, it is
     * when the banner is dismissed.
     */
    fun consumeAutoFocus(): Boolean {
        if (hasAutoFocused) return false
        hasAutoFocused = true
        return true
    }

    /**
     * Records that the user left for the set-up page. The field gets its focus back once they return.
     *
     * It also spends the first focus. The banner going away on the way out must not raise the
     * keyboard over the set-up page.
     */
    fun leaveForSetUp() {
        hasAutoFocused = true
        isAwaitingSetUpReturn = true
    }

    /**
     * Whether the user is back from the set-up page and the field should take its focus. True only
     * once per trip.
     *
     * The screen asks once nothing else is in the way. That is on return, or once the content search
     * sheet has closed if the user chose to turn content search on.
     */
    fun consumeSetUpReturn(): Boolean {
        if (!isAwaitingSetUpReturn) return false
        isAwaitingSetUpReturn = false
        return true
    }
}
