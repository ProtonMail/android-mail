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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.NO_ATTACHMENT_LIMIT
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration

data class ComposerStates(
    val main: ComposerState.Main,
    val attachments: ComposerState.Attachments,
    val accessories: ComposerState.Accessories,
    val effects: ComposerState.Effects
)

sealed interface ComposerState {

    enum class LoadingType {
        Initial,
        Save,
        None
    }

    data class Main(
        val draftId: MessageId,
        val senderUiModel: SenderUiModel,
        val prevSenderEmail: SenderEmail?,
        val senderAddresses: ImmutableList<SenderUiModel>,
        val isSubmittable: Boolean,
        val loadingType: LoadingType,
        val quotedHtmlContent: QuotedHtmlContent? = null,
        val shouldRestrictWebViewHeight: Boolean
    ) {

        companion object {

            fun initial(draftId: MessageId) = Main(
                draftId = draftId,
                senderUiModel = SenderUiModel(""),
                prevSenderEmail = null,
                senderAddresses = emptyList<SenderUiModel>().toImmutableList(),
                isSubmittable = false,
                loadingType = LoadingType.None,
                shouldRestrictWebViewHeight = false
            )
        }
    }

    data class Attachments(
        val uiModel: AttachmentGroupUiModel
    ) {

        companion object {

            fun initial() = Attachments(
                uiModel = AttachmentGroupUiModel(limit = NO_ATTACHMENT_LIMIT, attachments = emptyList())
            )
        }
    }

    data class Accessories(
        val isMessagePasswordSet: Boolean,
        val messageExpiresIn: Duration
    ) {

        companion object {

            fun initial() = Accessories(
                isMessagePasswordSet = false,
                messageExpiresIn = Duration.ZERO
            )
        }
    }

    data class Effects(
        val error: Effect<TextUiModel>,
        val exitError: Effect<TextUiModel>,
        val premiumFeatureMessage: Effect<TextUiModel>,
        val recipientValidationError: Effect<TextUiModel>,
        val changeBottomSheetVisibility: Effect<Boolean>,
        val closeComposer: Effect<Unit>,
        val closeComposerWithDraftSaved: Effect<Unit>,
        val closeComposerWithMessageSending: Effect<Unit>,
        val closeComposerWithMessageSendingOffline: Effect<Unit>,
        val confirmSendingWithoutSubject: Effect<Unit>,
        val changeFocusToField: Effect<FocusedFieldType>,
        val attachmentsFileSizeExceeded: Effect<Unit>,
        val attachmentsReEncryptionFailed: Effect<TextUiModel>,
        val warning: Effect<TextUiModel>,
        val focusTextBody: Effect<Unit> = Effect.empty(),
        val sendingErrorEffect: Effect<TextUiModel> = Effect.empty(),
        val senderChangedNotice: Effect<TextUiModel> = Effect.empty(),
        val confirmSendExpiringMessage: Effect<List<Participant>>,
        val openImagePicker: Effect<Unit>
    ) {

        companion object {

            fun initial() = Effects(
                error = Effect.empty(),
                exitError = Effect.empty(),
                premiumFeatureMessage = Effect.empty(),
                recipientValidationError = Effect.empty(),
                changeBottomSheetVisibility = Effect.empty(),
                closeComposer = Effect.empty(),
                closeComposerWithDraftSaved = Effect.empty(),
                closeComposerWithMessageSending = Effect.empty(),
                closeComposerWithMessageSendingOffline = Effect.empty(),
                confirmSendingWithoutSubject = Effect.empty(),
                changeFocusToField = Effect.empty(),
                attachmentsReEncryptionFailed = Effect.empty(),
                warning = Effect.empty(),
                sendingErrorEffect = Effect.empty(),
                senderChangedNotice = Effect.empty(),
                confirmSendExpiringMessage = Effect.empty(),
                openImagePicker = Effect.empty(),
                attachmentsFileSizeExceeded = Effect.empty()
            )
        }
    }
}
