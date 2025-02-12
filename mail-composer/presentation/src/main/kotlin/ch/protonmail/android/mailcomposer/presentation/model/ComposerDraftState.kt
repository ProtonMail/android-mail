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
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import kotlin.time.Duration

@Deprecated("Part of Composer V1, to be replaced with ComposerState")
data class ComposerDraftState(
    val fields: ComposerFields,
    val attachments: AttachmentGroupUiModel,
    val premiumFeatureMessage: Effect<TextUiModel>,
    val recipientValidationError: Effect<TextUiModel>,
    val error: Effect<TextUiModel>,
    val isSubmittable: Boolean,
    val isDeviceContactsSuggestionsEnabled: Boolean,
    val isDeviceContactsSuggestionsPromptEnabled: Boolean,
    val senderAddresses: List<SenderUiModel>,
    val changeBottomSheetVisibility: Effect<Boolean>,
    val closeComposer: Effect<Unit>,
    val closeComposerWithDraftSaved: Effect<Unit>,
    val closeComposerWithMessageSending: Effect<Unit>,
    val closeComposerWithMessageSendingOffline: Effect<Unit>,
    val confirmSendingWithoutSubject: Effect<Unit>,
    val changeFocusToField: Effect<FocusedFieldType>,
    val isLoading: Boolean,
    val attachmentsFileSizeExceeded: Effect<Unit>,
    val attachmentsReEncryptionFailed: Effect<Unit>,
    val replaceDraftBody: Effect<TextUiModel>,
    val warning: Effect<TextUiModel>,
    val isMessagePasswordSet: Boolean,
    val focusTextBody: Effect<Unit> = Effect.empty(),
    val sendingErrorEffect: Effect<TextUiModel> = Effect.empty(),
    val contactSuggestions: Map<ContactSuggestionsField, List<ContactSuggestionUiModel>> = emptyMap(),
    val areContactSuggestionsExpanded: Map<ContactSuggestionsField, Boolean> = emptyMap(),
    val senderChangedNotice: Effect<TextUiModel> = Effect.empty(),
    val messageExpiresIn: Duration,
    val confirmSendExpiringMessage: Effect<List<Participant>>,
    val openImagePicker: Effect<Unit>,
    val shouldRestrictWebViewHeight: Boolean
) {

    companion object {

        fun initial(
            draftId: MessageId,
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList(),
            isSubmittable: Boolean = false
        ): ComposerDraftState = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = SenderUiModel(""),
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            ),
            attachments = AttachmentGroupUiModel(
                attachments = emptyList()
            ),
            premiumFeatureMessage = Effect.empty(),
            recipientValidationError = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = isSubmittable,
            senderAddresses = emptyList(),
            changeBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            closeComposerWithMessageSending = Effect.empty(),
            closeComposerWithMessageSendingOffline = Effect.empty(),
            confirmSendingWithoutSubject = Effect.empty(),
            changeFocusToField = Effect.empty(),
            isLoading = false,
            attachmentsFileSizeExceeded = Effect.empty(),
            attachmentsReEncryptionFailed = Effect.empty(),
            warning = Effect.empty(),
            replaceDraftBody = Effect.empty(),
            sendingErrorEffect = Effect.empty(),
            isMessagePasswordSet = false,
            senderChangedNotice = Effect.empty(),
            messageExpiresIn = Duration.ZERO,
            confirmSendExpiringMessage = Effect.empty(),
            isDeviceContactsSuggestionsEnabled = false,
            isDeviceContactsSuggestionsPromptEnabled = false,
            openImagePicker = Effect.empty(),
            shouldRestrictWebViewHeight = false
        )
    }
}

data class ComposerFields(
    val draftId: MessageId,
    val sender: SenderUiModel,
    val to: List<RecipientUiModel>,
    val cc: List<RecipientUiModel>,
    val bcc: List<RecipientUiModel>,
    val subject: String,
    val body: String,
    val quotedBody: QuotedHtmlContent? = null
)

enum class ContactSuggestionsField {
    TO,
    CC,
    BCC
}
