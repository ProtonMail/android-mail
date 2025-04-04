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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.StyledHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.presentation.facade.AddressesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.DraftFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageContentFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageParticipantsFacade
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerStates
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsState
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.model.Recipient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.serialize
import kotlin.test.assertEquals

internal object ComposerViewModel2SharedTestData {

    val userId = UserId("user-id")
    val messageId = MessageId("message-id")
    val parentMessageId = MessageId("parent-message-id")
    val recipientsTo = listOf(Recipient(address = "123@321.1", "nameTo"))
    val recipientsCc = listOf(Recipient(address = "123@321.2", "nameCc"))
    val recipientsBcc = listOf(Recipient(address = "123@321.3", "nameBcc"))
    val originalHtmlQuote = OriginalHtmlQuote("original-html")
    val styledHtml = StyledHtmlQuote("quoted-html")

    val defaultDraftFields = DraftFields(
        sender = SenderEmail("sender@email.com"),
        subject = Subject("Subject"),
        body = DraftBody("Body"),
        recipientsTo = RecipientsTo(recipientsTo),
        recipientsCc = RecipientsCc(recipientsCc),
        recipientsBcc = RecipientsBcc(recipientsBcc),
        originalHtmlQuote = originalHtmlQuote
    )

    val defaultRecipientsState = RecipientsState(
        toRecipients = recipientsTo.map { RecipientUiModel.Valid(it.address) }.toImmutableList(),
        ccRecipients = recipientsCc.map { RecipientUiModel.Valid(it.address) }.toImmutableList(),
        bccRecipients = recipientsBcc.map { RecipientUiModel.Valid(it.address) }.toImmutableList()
    )

    fun expectShareViaData(savedStateHandle: SavedStateHandle, rawData: String) {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns rawData
        every { savedStateHandle.get<String>(ComposerScreen.HasSavedDraftKey) } returns null
    }

    fun expectExistingDraft(savedStateHandle: SavedStateHandle, rawMessageId: String) {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns rawMessageId
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.HasSavedDraftKey) } returns null
    }

    fun expectRestoredState(savedStateHandle: SavedStateHandle) {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns messageId.id
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
        every { savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) } returns true
    }

    fun expectStandaloneDraft(savedStateHandle: SavedStateHandle) {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
        every { savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) } returns null
    }

    fun expectComposeToAddressDraft(savedStateHandle: SavedStateHandle, rawData: String) {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns rawData
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
        every { savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) } returns null
    }

    @Suppress("LongParameterList")
    fun expectDraftAction(
        draftFacade: DraftFacade,
        addressesFacade: AddressesFacade,
        messageContentFacade: MessageContentFacade,
        savedStateHandle: SavedStateHandle,
        draftAction: DraftAction,
        parentMessage: MessageWithDecryptedBody,
        draftFields: DraftFields = defaultDraftFields
    ) {
        every { draftFacade.provideNewDraftId() } returns messageId
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns draftAction.serialize()
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
        every { savedStateHandle.get<String>(ComposerScreen.HasSavedDraftKey) } returns null

        val validatedSender = ValidateSenderAddress.ValidationResult.Valid(defaultDraftFields.sender)
        coEvery {
            draftFacade.parentMessageToDraftFields(userId, parentMessageId, draftAction)
        } returns Pair(parentMessage, draftFields)

        coEvery {
            draftFacade.storeDraftWithParentAttachments(
                userId = userId,
                messageId = messageId,
                parentMessage = parentMessage,
                senderEmail = draftFields.sender,
                draftAction = draftAction
            )
        } returns Unit.right()

        coEvery {
            addressesFacade.validateSenderAddress(userId, draftFields.sender)
        } returns validatedSender.right()

        coEvery {
            messageContentFacade.styleQuotedHtml(draftFields.originalHtmlQuote!!)
        } returns styledHtml

        coEvery { draftFacade.storeDraft(userId, messageId, draftFields, draftAction) } returns Unit.right()
        every { savedStateHandle[ComposerScreen.HasSavedDraftKey] = true } just runs
    }

    fun expectParticipantsMapping(messageParticipantsFacade: MessageParticipantsFacade) {
        val toRecipient = recipientsTo.first()
        coEvery {
            messageParticipantsFacade.mapToParticipant(RecipientUiModel.Valid(toRecipient.address))
        } returns Participant(
            toRecipient.address,
            toRecipient.name
        )
        val ccRecipient = recipientsCc.first()
        coEvery {
            messageParticipantsFacade.mapToParticipant(RecipientUiModel.Valid(ccRecipient.address))
        } returns Participant(
            ccRecipient.address,
            ccRecipient.name
        )
        val bccRecipient = recipientsBcc.first()
        coEvery {
            messageParticipantsFacade.mapToParticipant(RecipientUiModel.Valid(bccRecipient.address))
        } returns Participant(
            bccRecipient.address,
            bccRecipient.name
        )
    }

    fun verifyStates(
        main: ComposerState.Main = ComposerState.Main.initial(messageId),
        attachments: ComposerState.Attachments = ComposerState.Attachments.initial(),
        accessories: ComposerState.Accessories = ComposerState.Accessories.initial(),
        effects: ComposerState.Effects = ComposerState.Effects.initial(),
        actualStates: ComposerStates
    ) {
        assertEquals(main, actualStates.main)
        assertEquals(attachments, actualStates.attachments)
        assertEquals(accessories, actualStates.accessories)
        assertEquals(effects, actualStates.effects)
    }
}
