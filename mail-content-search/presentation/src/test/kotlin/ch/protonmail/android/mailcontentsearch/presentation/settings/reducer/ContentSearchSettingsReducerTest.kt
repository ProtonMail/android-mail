/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.settings.reducer

import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchSettingsReducerTest {

    private val reducer = ContentSearchSettingsReducer()

    @Test
    fun `toggling on preserves indexing progress so synced state does not flash 'Preparing'`() {
        val current = ContentSearchSettingsState.WithData(
            isContentSearchEnabled = false,
            isAllowMobileDataEnabled = true,
            syncPercentage = null,
            isIndexingActive = false
        )

        val result =
            reducer.newStateFrom(current, ContentSearchSettingsEvent.Data.ContentSearchToggled(newValue = true))

        assertEquals(
            current.copy(isContentSearchEnabled = true),
            result
        )
    }

    @Test
    fun `toggling off clears indexing progress and active state`() {
        val current = ContentSearchSettingsState.WithData(
            isContentSearchEnabled = true,
            isAllowMobileDataEnabled = true,
            syncPercentage = 42.0,
            isIndexingActive = true
        )

        val result =
            reducer.newStateFrom(current, ContentSearchSettingsEvent.Data.ContentSearchToggled(newValue = false))

        assertEquals(
            current.copy(
                isContentSearchEnabled = false,
                syncPercentage = null,
                isIndexingActive = false
            ),
            result
        )
    }
}
