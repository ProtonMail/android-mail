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

package ch.protonmail.android.maildetail.presentation.model

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltipState
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState

data class MessageDetailState(
    val messageMetadataState: MessageMetadataState,
    val messageBannersState: MessageBannersState,
    val messageBodyState: MessageBodyState,
    val bottomBarState: BottomBarState,
    val bottomSheetState: BottomSheetState?,
    val exitScreenEffect: Effect<Unit>,
    val exitScreenWithMessageEffect: Effect<ActionResult>,
    val error: Effect<TextUiModel>,
    val openMessageBodyLinkEffect: Effect<Uri>,
    val openAttachmentEffect: Effect<OpenAttachmentIntentValues>,
    val openProtonCalendarIntent: Effect<OpenProtonCalendarIntentValues>,
    val requestLinkConfirmation: Boolean,
    val requestPhishingLinkConfirmation: Boolean,
    val deleteDialogState: DeleteDialogState,
    val reportPhishingDialogState: ReportPhishingDialogState,
    val spotlightTooltip: SpotlightTooltipState
) {

    companion object {

        val Loading = MessageDetailState(
            messageMetadataState = MessageMetadataState.Loading,
            messageBannersState = MessageBannersState.Loading,
            messageBodyState = MessageBodyState.Loading,
            bottomBarState = BottomBarState.Loading,
            bottomSheetState = null,
            exitScreenEffect = Effect.empty(),
            exitScreenWithMessageEffect = Effect.empty(),
            error = Effect.empty(),
            openMessageBodyLinkEffect = Effect.empty(),
            openAttachmentEffect = Effect.empty(),
            openProtonCalendarIntent = Effect.empty(),
            requestLinkConfirmation = false,
            requestPhishingLinkConfirmation = false,
            deleteDialogState = DeleteDialogState.Hidden,
            reportPhishingDialogState = ReportPhishingDialogState.Hidden,
            spotlightTooltip = SpotlightTooltipState.Hidden
        )
    }
}
