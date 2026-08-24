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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsOperation
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState
import javax.inject.Inject

class ContentSearchSettingsReducer @Inject constructor() {

    fun newStateFrom(
        currentState: ContentSearchSettingsState,
        operation: ContentSearchSettingsOperation
    ): ContentSearchSettingsState = currentState.toNewStateFromOperation(operation)

    private fun ContentSearchSettingsState.toNewStateFromOperation(
        event: ContentSearchSettingsOperation
    ): ContentSearchSettingsState = when (this) {
        is ContentSearchSettingsState.Loading -> when (event) {
            is ContentSearchSettingsEvent.Data.ContentLoaded -> ContentSearchSettingsState.Data(
                isContentSearchEnabled = event.isContentSearchEnabled,
                isAllowMobileDataEnabled = event.isAllowMobileDataEnabled,
                syncPercentage = null,
                isIndexingActive = false
            )
            is ContentSearchSettingsEvent.Error.LoadingError -> ContentSearchSettingsState.LoadingError
            else -> this
        }

        is ContentSearchSettingsState.Data -> when (event) {
            // Only the toggle. The indexing fields belong to IndexingProgress, which reports them
            // for a disabled account too, and clearing them here as well wiped the failure the
            // screen had just read: the enabled observer replays its current value on open.
            is ContentSearchSettingsEvent.Data.ContentSearchToggled -> copy(
                isContentSearchEnabled = event.newValue
            )
            is ContentSearchSettingsEvent.Data.AllowMobileDataToggled -> copy(
                isAllowMobileDataEnabled = event.newValue
            )
            is ContentSearchSettingsEvent.Data.LocalSearchDataCleared -> copy(
                isContentSearchEnabled = false,
                syncPercentage = null,
                isIndexingActive = false,
                isWaitingForUnmeteredConnection = false,
                isIndexingFailed = false
            )
            // Supersedes an in-flight retry: Rust reporting where the account stands is a better
            // answer than the one the button is still waiting for.
            is ContentSearchSettingsEvent.Data.IndexingProgress -> copy(
                syncPercentage = event.percentage,
                isIndexingActive = event.isActive,
                isWaitingForUnmeteredConnection = event.isWaitingForUnmeteredConnection,
                isIndexingFailed = event.isFailed,
                isRetryingIndexing = false
            )
            is ContentSearchSettingsEvent.Data.IndexingRetryStarted -> copy(isRetryingIndexing = true)

            is ContentSearchSettingsEvent.Data.IndexingRetryAccepted -> copy(
                isRetryingIndexing = false,
                isIndexingFailed = false,
                isIndexingActive = true
            )
            is ContentSearchSettingsEvent.Error.UpdateError -> copy(
                isRetryingIndexing = false,
                updateErrorEffect = Effect.of(
                    TextUiModel.TextRes(R.string.mail_settings_content_search_update_error)
                )
            )
            is ContentSearchSettingsEvent.Error.IndexingRetryFailed -> copy(
                isRetryingIndexing = false,
                updateErrorEffect = Effect.of(
                    TextUiModel.TextRes(R.string.mail_settings_content_search_retry_error)
                )
            )
            else -> this
        }

        else -> this
    }
}
