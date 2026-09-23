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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState
import ch.protonmail.android.mailcontentsearch.presentation.settings.model.IndexSizeUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ContentSearchSettingsReducerTest {

    private val reducer = ContentSearchSettingsReducer()

    @Test
    fun `toggling on preserves indexing progress so synced state does not flash 'Preparing'`() {
        val current = ContentSearchSettingsState.Data(
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
    fun `toggling leaves the indexing fields to the progress event`() {
        // Given
        val current = ContentSearchSettingsState.Data(
            isContentSearchEnabled = true,
            isAllowMobileDataEnabled = true,
            syncPercentage = 42.0,
            isIndexingActive = true,
            isIndexingFailed = true
        )

        val result =
            reducer.newStateFrom(current, ContentSearchSettingsEvent.Data.ContentSearchToggled(newValue = false))

        assertEquals(current.copy(isContentSearchEnabled = false), result)
    }

    @Test
    fun `an accepted retry clears the failure without waiting for the stream`() {
        // Given
        val current = FailedState.copy(isRetryingIndexing = true)

        // When
        val result = reducer.newStateFrom(current, ContentSearchSettingsEvent.Data.IndexingRetryAccepted)

        // Then
        assertEquals(
            current.copy(isRetryingIndexing = false, isIndexingFailed = false, isIndexingActive = true),
            result
        )
    }

    @Test
    fun `a pause is taken from the progress event, which is the only thing that reports one`() {
        // When
        val result = reducer.newStateFrom(EnabledState, PausedProgress)

        // Then
        assertTrue(result.asData().isIndexingPaused)
    }

    @Test
    fun `a running backfill clears the pause`() {
        // Given
        val paused = reducer.newStateFrom(EnabledState, PausedProgress)

        // When
        val result = reducer.newStateFrom(
            paused,
            ContentSearchSettingsEvent.Data.IndexingProgress(
                percentage = 31.0,
                isActive = true,
                isCompleted = false,
                isWaitingForUnmeteredConnection = false,
                isPaused = false,
                isFailed = false
            )
        )

        // Then
        assertFalse(result.asData().isIndexingPaused)
    }

    @Test
    fun `switching the account off clears the pause with it`() {
        // Given
        val paused = reducer.newStateFrom(EnabledState, PausedProgress)

        // When - nothing is paused for an account that is off, and turning it on starts it again.
        val result = reducer.newStateFrom(
            paused,
            ContentSearchSettingsEvent.Data.ContentSearchToggled(newValue = false)
        )

        // Then
        assertFalse(result.asData().isIndexingPaused)
    }

    @Test
    fun `resuming clears the pause`() {
        // Given
        val paused = reducer.newStateFrom(EnabledState, PausedProgress)

        // When
        val result = reducer.newStateFrom(paused, ContentSearchSettingsEvent.Data.IndexingRetryAccepted)

        // Then
        assertFalse(result.asData().isIndexingPaused)
    }

    @Test
    fun `a completed account is marked as such, so the card can say 100 percent`() {
        // When
        val result = reducer.newStateFrom(
            EnabledState,
            ContentSearchSettingsEvent.Data.IndexingProgress(
                percentage = null,
                isActive = false,
                isCompleted = true,
                isWaitingForUnmeteredConnection = false,
                isPaused = false,
                isFailed = false
            )
        )

        // Then
        assertTrue(result.asData().isIndexingCompleted)
    }

    @Test
    fun `the index size is kept as read`() {
        // When
        val result = reducer.newStateFrom(EnabledState, ContentSearchSettingsEvent.Data.IndexSizeLoaded(1_024L))

        // Then
        assertEquals(IndexSizeUiModel.Loaded(1_024L), result.asData().indexSize)
    }

    @Test
    fun `an index size rust cannot read hides the row`() {
        // When
        val result = reducer.newStateFrom(EnabledState, ContentSearchSettingsEvent.Data.IndexSizeLoaded(null))

        // Then
        assertEquals(IndexSizeUiModel.Unavailable, result.asData().indexSize)
    }

    @Test
    fun `clearing the local data measures the index size again`() {
        // Given
        val withSize = reducer.newStateFrom(EnabledState, ContentSearchSettingsEvent.Data.IndexSizeLoaded(1_024L))

        // When
        val result = reducer.newStateFrom(withSize, ContentSearchSettingsEvent.Data.LocalSearchDataCleared)

        // Then
        assertEquals(IndexSizeUiModel.Loading, result.asData().indexSize)
    }

    @Test
    fun `a progress event overrules a retry that is still pending`() {
        // Given
        val current = FailedState.copy(isRetryingIndexing = true)

        // When
        val result = reducer.newStateFrom(
            current,
            ContentSearchSettingsEvent.Data.IndexingProgress(
                percentage = 12.0,
                isActive = true,
                isCompleted = false,
                isWaitingForUnmeteredConnection = false,
                isPaused = false,
                isFailed = false
            )
        )

        // Then
        assertEquals(
            current.copy(
                syncPercentage = 12.0,
                isIndexingActive = true,
                isIndexingFailed = false,
                isRetryingIndexing = false
            ),
            result
        )
    }

    @Test
    fun `a failed retry ends the retry and asks for a snackbar of its own`() {
        // Given
        val current = FailedState.copy(isRetryingIndexing = true)

        // When
        val result = reducer.newStateFrom(current, ContentSearchSettingsEvent.Error.IndexingRetryFailed)

        // Then
        val data = result as ContentSearchSettingsState.Data
        assertTrue(data.isIndexingFailed)
        assertFalse(data.isRetryingIndexing)
        assertEquals(
            TextUiModel.TextRes(R.string.mail_settings_content_search_retry_error),
            data.updateErrorEffect.consume()
        )
    }

    private companion object {

        val FailedState = ContentSearchSettingsState.Data(
            isContentSearchEnabled = true,
            isAllowMobileDataEnabled = true,
            syncPercentage = null,
            isIndexingActive = false,
            isIndexingFailed = true
        )
    }
}

private val PausedProgress = ContentSearchSettingsEvent.Data.IndexingProgress(
    percentage = 30.0,
    isActive = false,
    isCompleted = false,
    isWaitingForUnmeteredConnection = false,
    isPaused = true,
    isFailed = false
)

private val EnabledState = ContentSearchSettingsState.Data(
    isContentSearchEnabled = true,
    isAllowMobileDataEnabled = true,
    syncPercentage = 30.0,
    isIndexingActive = true
)

private fun ContentSearchSettingsState.asData() = this as ContentSearchSettingsState.Data
