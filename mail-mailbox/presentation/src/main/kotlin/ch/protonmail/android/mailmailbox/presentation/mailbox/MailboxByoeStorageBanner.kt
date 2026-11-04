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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailbyoe.presentation.storage.ByoeStorageBanner
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel
import timber.log.Timber

/**
 * The BYOE storage banner, whose "Upgrade" opens the upsell offered to this user for a Gmail import paused because
 * storage is full.
 */
@Composable
internal fun MailboxByoeStorageBanner(
    onNavigateToUpselling: (entryPoint: UpsellingEntryPoint.Feature, type: UpsellingVisibility) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keyed, so it does not share the instance of another upsell entry point on the same screen.
    val upsellingViewModel = hiltViewModel<UpsellingButtonViewModel, UpsellingButtonViewModel.Factory>(
        key = UpsellingEntryPoint.Feature.ByoeStorage.toString()
    ) { factory ->
        factory.create(UpsellingEntryPoint.Feature.ByoeStorage)
    }
    val upsellingState by upsellingViewModel.state.collectAsStateWithLifecycle()

    ByoeStorageBanner(
        onUpgrade = {
            when (val visibility = upsellingState.visibility) {
                UpsellingVisibility.Hidden -> Timber.w("byoe: no upsell to offer for the paused Gmail import")
                else -> onNavigateToUpselling(UpsellingEntryPoint.Feature.ByoeStorage, visibility)
            }
        },
        modifier = modifier
    )
}
