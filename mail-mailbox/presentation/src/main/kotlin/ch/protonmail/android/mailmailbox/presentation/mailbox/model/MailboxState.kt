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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState

@Stable
data class MailboxState(
    val mailboxListState: MailboxListState,
    val topAppBarState: MailboxTopAppBarState,
    val upgradeStorageState: UpgradeStorageState,
    val unreadFilterState: UnreadFilterState,
    val bottomAppBarState: BottomBarState,
    val deleteDialogState: DeleteDialogState,
    val deleteAllDialogState: DeleteDialogState,
    val autoDeleteSettingState: AutoDeleteSettingState,
    val storageLimitState: StorageLimitState,
    val bottomSheetState: BottomSheetState?,
    val actionResult: Effect<ActionResult>,
    val error: Effect<TextUiModel>,
    val showRatingBooster: Effect<Unit>,
    val showNPSFeedback: Effect<Unit>
)
