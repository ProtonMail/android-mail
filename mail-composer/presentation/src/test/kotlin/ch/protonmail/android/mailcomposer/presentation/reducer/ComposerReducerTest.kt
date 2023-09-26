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

package ch.protonmail.android.mailcomposer.presentation.reducer

import java.util.Random
import java.util.UUID
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsBccChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsCcChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsToChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.SenderChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Invalid
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Valid
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.NO_ATTACHMENT_LIMIT
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ComposerReducerTest(
    private val testName: String,
    private val testTransition: TestTransition
) {

    private val attachmentUiModelMapper = AttachmentUiModelMapper()
    private val composerReducer = ComposerReducer(attachmentUiModelMapper)

    @Test
    fun `Test composer transition states`() = runTest {
        with(testTransition) {
            val actualState = composerReducer.newStateFrom(currentState, operation)

            assertEquals(expectedState, actualState, testName)
        }
    }

    companion object {

        private val messageId = MessageId(UUID.randomUUID().toString())
        private val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)

        private val draftFields = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            DraftBody("Decrypted body of this draft"),
            RecipientsTo(listOf(Recipient("you@proton.ch", "Name"))),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList()),
            null
        )

        private val draftFieldsWithoutRecipients = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            DraftBody("Decrypted body of this draft"),
            RecipientsTo(emptyList()),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList()),
            null
        )

        private val draftUiModel = DraftUiModel(draftFields, null)

        private val draftUiModelWithoutRecipients = DraftUiModel(draftFieldsWithoutRecipients, null)

        private val EmptyToSubmittableToField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the to field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsToChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, to = listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableToField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the to field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsToChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, to = listOf(Invalid(this)))
            )
        }

        private val SubmittableToNotSubmittableEmptyToField = with("a@b.c") {
            TestTransition(
                name = "Should generate not-submittable state when removing all valid email addresses from TO field",
                currentState = ComposerDraftState.initial(
                    messageId,
                    to = listOf(Valid(this)),
                    isSubmittable = true
                ),
                operation = RecipientsToChanged(emptyList()),
                expectedState = aNotSubmittableState(messageId, to = emptyList(), error = Effect.empty())
            )
        }

        private val EmptyToSubmittableCcField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the cc field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsCcChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, cc = listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableCcField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the cc field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsCcChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, cc = listOf(Invalid(this)))
            )
        }

        private val SubmittableToNotSubmittableEmptyCcField = with("a@b.c") {
            TestTransition(
                name = "Should generate not-submittable state when removing all valid email addresses from CC field",
                currentState = ComposerDraftState.initial(
                    messageId,
                    cc = listOf(Valid(this)),
                    isSubmittable = true
                ),
                operation = RecipientsCcChanged(emptyList()),
                expectedState = aNotSubmittableState(messageId, cc = emptyList(), error = Effect.empty())
            )
        }

        private val EmptyToSubmittableBccField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the bcc field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsBccChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, bcc = listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableBccField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the bcc field",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsBccChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, bcc = listOf(Invalid(this)))
            )
        }

        private val SubmittableToNotSubmittableEmptyBccField = with("a@b.c") {
            TestTransition(
                name = "Should generate not-submittable state when removing all valid email addresses from BCC field",
                currentState = ComposerDraftState.initial(
                    messageId,
                    bcc = listOf(Valid(this)),
                    isSubmittable = true
                ),
                operation = RecipientsBccChanged(emptyList()),
                expectedState = aNotSubmittableState(messageId, bcc = emptyList(), error = Effect.empty())
            )
        }

        private val NotSubmittableToWithoutErrorToField = with("a@b.c") {
            val invalidEmail = UUID.randomUUID().toString()
            TestTransition(
                name = "Should generate not submittable non error state when adding valid email to current error",
                currentState = aNotSubmittableState(messageId, to = listOf(Invalid(invalidEmail))),
                operation = RecipientsToChanged(listOf(Invalid(invalidEmail), Valid(this))),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    to = listOf(Invalid(invalidEmail), Valid(this)),
                    error = Effect.empty()
                )
            )
        }

        private val NotSubmittableToWithErrorToField = with("a@b.c") {
            val invalidEmail = UUID.randomUUID().toString()
            TestTransition(
                name = "Should generate not submittable error state when adding invalid followed by invalid address",
                currentState = aNotSubmittableState(messageId, to = listOf(Invalid(invalidEmail))),
                operation = RecipientsToChanged(listOf(Invalid(invalidEmail), Invalid(this))),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    to = listOf(Invalid(invalidEmail), Invalid(this))
                )
            )
        }

        private val NotSubmittableWithoutErrorWhenRemoving = with("a@b.c") {
            val invalidEmail = UUID.randomUUID().toString()
            TestTransition(
                name = "Should generate not submittable state without error when removing invalid address",
                currentState = aNotSubmittableState(messageId, to = listOf(Invalid(invalidEmail), Invalid(this))),
                operation = RecipientsToChanged(listOf(Invalid(invalidEmail))),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    to = listOf(Invalid(invalidEmail)),
                    error = Effect.empty()
                )
            )
        }

        private val EmptyToUpgradePlan = TestTransition(
            name = "Should generate a state showing 'upgrade plan' message when free user tries to change sender",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.ErrorFreeUserCannotChangeSender,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature)),
                error = Effect.empty()
            )
        )

        private val EmptyToSenderAddressesList = TestTransition(
            name = "Should generate a state showing change sender bottom sheet when paid tries to change sender",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.SenderAddressesReceived(addresses.map { SenderUiModel(it.email) }),
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.empty(),
                senderAddresses = addresses.map { SenderUiModel(it.email) },
                changeSenderBottomSheetVisibility = Effect.of(true)
            )
        )

        private val EmptyToErrorWhenUserPlanUnknown = TestTransition(
            name = "Should generate an error state when failing to determine if user can change sender",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.ErrorVerifyingPermissionsToChangeSender,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
            )
        )

        private val EmptyToUpdatedSender = with(SenderUiModel("updated-sender@proton.ch")) {
            TestTransition(
                name = "Should update the state with the new sender and close bottom sheet when address changes",
                currentState = ComposerDraftState.initial(messageId),
                operation = SenderChanged(this),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    sender = this,
                    error = Effect.empty(),
                    changeSenderBottomSheetVisibility = Effect.of(false)
                )
            )
        }

        private val EmptyToChangeSubjectError = TestTransition(
            name = "Should update the state showing an error when error storing draft subject",
            currentState = aNotSubmittableState(draftId = messageId),
            operation = ComposerEvent.ErrorStoringDraftSubject,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.of(TextUiModel(R.string.composer_error_store_draft_subject))
            )
        )

        private val DefaultSenderToChangeSenderFailed = TestTransition(
            name = "Should update the state showing an error and preserving the previous sender address",
            currentState = aNotSubmittableState(
                draftId = messageId,
                sender = SenderUiModel("default@pm.me")
            ),
            operation = ComposerEvent.ErrorStoringDraftSenderAddress,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                sender = SenderUiModel("default@pm.me"),
                error = Effect.of(TextUiModel(R.string.composer_error_store_draft_sender_address)),
                changeSenderBottomSheetVisibility = Effect.of(false)
            )
        )

        private val DuplicateToToNotDuplicateWithError = with(aMultipleRandomRange().map { "a@b.c" }) {
            TestTransition(
                name = "Should remove duplicate TO recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsToChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    to = listOf(Valid(this.first())),
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            this.first()
                        )
                    )
                )
            )
        }

        private val DuplicateCcToNotDuplicateWithError = with(aMultipleRandomRange().map { "a@b.c" }) {
            TestTransition(
                name = "Should remove duplicate CC recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsCcChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    cc = listOf(Valid(this.first())),
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            this.first()
                        )
                    )
                )
            )
        }

        private val DuplicateBccToNotDuplicateWithError = with(aMultipleRandomRange().map { "a@b.c" }) {
            TestTransition(
                name = "Should remove duplicate BCC recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsBccChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    bcc = listOf(Valid(this.first())),
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            this.first()
                        )
                    )
                )
            )
        }

        private val ManyDuplicatesToToNotDuplicateWithError = with(
            aMultipleRandomRange().map { "a@b.c" } + aMultipleRandomRange().map { "d@e.f" }
        ) {
            val expected = listOf(Valid("a@b.c"), Valid("d@e.f"))
            TestTransition(
                name = "Should remove multiple duplicate To recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsToChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    to = expected,
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            expected.joinToString(", ") { it.address }
                        )
                    )
                )
            )
        }

        private val ManyDuplicatesCcToNotDuplicateWithError = with(
            aMultipleRandomRange().map { "a@b.c" } + aMultipleRandomRange().map { "d@e.f" }
        ) {
            val expected = listOf(Valid("a@b.c"), Valid("d@e.f"))
            TestTransition(
                name = "Should remove multiple duplicate CC recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsCcChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    cc = expected,
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            expected.joinToString(", ") { it.address }
                        )
                    )
                )
            )
        }

        private val ManyDuplicatesBccToNotDuplicateWithError = with(
            aMultipleRandomRange().map { "a@b.c" } + aMultipleRandomRange().map { "d@e.f" }
        ) {
            val expected = listOf(Valid("a@b.c"), Valid("d@e.f"))
            TestTransition(
                name = "Should remove multiple duplicate BCC recipients and contain error if there are",
                currentState = ComposerDraftState.initial(messageId),
                operation = RecipientsBccChanged(this.map { Valid(it) }),
                expectedState = aSubmittableState(
                    draftId = messageId,
                    bcc = expected,
                    error = Effect.of(
                        TextUiModel(
                            R.string.composer_error_duplicate_recipient,
                            expected.joinToString(", ") { it.address }
                        )
                    )
                )
            )
        }

        private val EmptyToUpdatedDraftBody = with(DraftBody("Updated draft body")) {
            TestTransition(
                name = "Should update the state with the new draft body when it changes",
                currentState = ComposerDraftState.initial(messageId),
                operation = ComposerAction.DraftBodyChanged(this),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    error = Effect.empty(),
                    draftBody = this.value

                )
            )
        }

        private val EmptyToUpdatedSubject = with(Subject("This is a new subject")) {
            TestTransition(
                name = "Should update the state with the new subject when it changes",
                currentState = ComposerDraftState.initial(messageId),
                operation = ComposerAction.SubjectChanged(this),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    subject = this,
                    error = Effect.empty()
                )
            )
        }

        private val EmptyToCloseComposer = TestTransition(
            name = "Should close the composer",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerAction.OnCloseComposer,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.empty(),
                closeComposer = Effect.of(Unit),
                closeComposerWithDraftSaved = Effect.empty()
            )
        )

        private val EmptyToCloseComposerWithDraftSaved = TestTransition(
            name = "Should close the composer notifying draft saved",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.OnCloseWithDraftSaved,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.empty(),
                closeComposer = Effect.empty(),
                closeComposerWithDraftSaved = Effect.of(Unit)
            )
        )

        private val SubmittableToSendMessage =
            TestTransition(
                name = "Should update submittable state with message sending after OnSendMessage action",
                currentState = aSubmittableState(messageId),
                operation = ComposerAction.OnSendMessage,
                expectedState = aSubmittableState(
                    messageId,
                    closeComposerWithMessageSending = Effect.of(Unit)
                )
            )

        private val SubmittableToOnSendMessageOffline =
            TestTransition(
                name = "Should update submittable state with message sending after OnSendMessageOffline action",
                currentState = aSubmittableState(messageId),
                operation = ComposerEvent.OnSendMessageOffline,
                expectedState = aSubmittableState(
                    messageId,
                    closeComposerWithMessageSendingOffline = Effect.of(Unit)
                )
            )

        private val EmptyToLoadingWithOpenExistingDraft = TestTransition(
            name = "Should set state to loading when open of existing draft was requested",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.OpenExistingDraft(messageId),
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.empty(),
                isLoading = true
            )
        )

        @Suppress("VariableMaxLength")
        private val LoadingToFieldsWhenReceivedDraftDataEmptyRecipients = TestTransition(
            name = "Should stop loading and set the received draft data as composer fields when draft data received, " +
                "empty recipients",
            currentState = ComposerDraftState.initial(messageId).copy(isLoading = true),
            operation = ComposerEvent.PrefillDraftDataReceived(draftUiModelWithoutRecipients),
            expectedState = aNotSubmittableState(
                draftId = messageId,
                sender = SenderUiModel(draftFieldsWithoutRecipients.sender.value),
                to = draftFieldsWithoutRecipients.recipientsTo.value.map { Valid(it.address) },
                cc = draftFieldsWithoutRecipients.recipientsCc.value.map { Valid(it.address) },
                bcc = draftFieldsWithoutRecipients.recipientsBcc.value.map { Valid(it.address) },
                subject = draftFieldsWithoutRecipients.subject,
                draftBody = draftFieldsWithoutRecipients.body.value,
                error = Effect.empty(),
                isLoading = false
            )
        )

        @Suppress("VariableMaxLength")
        private val LoadingToFieldsWhenReceivedDraftDataValidRecipients = TestTransition(
            name = "Should stop loading and set the received draft data as composer fields when draft data received, " +
                "valid recipients",
            currentState = ComposerDraftState.initial(messageId).copy(isLoading = true),
            operation = ComposerEvent.PrefillDraftDataReceived(draftUiModel),
            expectedState = aSubmittableState(
                draftId = messageId,
                sender = SenderUiModel(draftFieldsWithoutRecipients.sender.value),
                to = draftFields.recipientsTo.value.map { Valid(it.address) },
                cc = draftFields.recipientsCc.value.map { Valid(it.address) },
                bcc = draftFields.recipientsBcc.value.map { Valid(it.address) },
                subject = draftFieldsWithoutRecipients.subject,
                draftBody = draftFieldsWithoutRecipients.body.value,
                error = Effect.empty()
            )
        )

        private val LoadingToErrorWhenErrorLoadingDraftData = TestTransition(
            name = "Should stop loading and display error when failing to receive draft data",
            currentState = ComposerDraftState.initial(messageId).copy(isLoading = true),
            operation = ComposerEvent.ErrorLoadingDraftData,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.of(TextUiModel(R.string.composer_error_loading_draft)),
                isLoading = false
            )
        )

        private val EmptyToUpdatedMessageIdOnApiAssignedId = TestTransition(
            name = "Should update the draft id in the draft state fields when API assigned ID is received",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.ApiAssignedMessageIdReceived(MessageIdSample.RemoteDraft),
            expectedState = aNotSubmittableState(
                draftId = MessageIdSample.RemoteDraft,
                error = Effect.empty()
            )
        )

        private val EmptyToBottomSheetOpened = TestTransition(
            name = "Should open the bottom sheet when add attachments action is chosen",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerAction.OnAddAttachments,
            expectedState = ComposerDraftState.initial(messageId).copy(
                changeBottomSheetVisibility = Effect.of(true)
            )
        )

        private val EmptyToBottomSheetClosed = TestTransition(
            name = "Should close the bottom sheet when a bottom sheet option has been chosen",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerAction.OnBottomSheetOptionSelected,
            expectedState = ComposerDraftState.initial(messageId).copy(
                changeBottomSheetVisibility = Effect.of(false)
            )
        )

        private val EmptyToAttachmentsUpdated = TestTransition(
            name = "Should emit attachments when they are updated",
            currentState = ComposerDraftState.initial(messageId),
            operation = ComposerEvent.OnAttachmentsUpdated(listOf(MessageAttachmentSample.invoice)),
            expectedState = ComposerDraftState.initial(messageId).copy(
                attachments = AttachmentGroupUiModel(
                    limit = NO_ATTACHMENT_LIMIT,
                    attachments = listOf(AttachmentUiModelSample.deletableInvoice)
                )
            )
        )

        private val transitions = listOf(
            EmptyToSubmittableToField,
            EmptyToNotSubmittableToField,
            SubmittableToNotSubmittableEmptyToField,
            EmptyToSubmittableCcField,
            EmptyToNotSubmittableCcField,
            SubmittableToNotSubmittableEmptyCcField,
            EmptyToSubmittableBccField,
            EmptyToNotSubmittableBccField,
            SubmittableToNotSubmittableEmptyBccField,
            NotSubmittableToWithoutErrorToField,
            NotSubmittableToWithErrorToField,
            NotSubmittableWithoutErrorWhenRemoving,
            EmptyToUpgradePlan,
            EmptyToSenderAddressesList,
            EmptyToErrorWhenUserPlanUnknown,
            EmptyToUpdatedSender,
            EmptyToChangeSubjectError,
            DefaultSenderToChangeSenderFailed,
            DuplicateToToNotDuplicateWithError,
            DuplicateCcToNotDuplicateWithError,
            DuplicateBccToNotDuplicateWithError,
            ManyDuplicatesToToNotDuplicateWithError,
            ManyDuplicatesCcToNotDuplicateWithError,
            ManyDuplicatesBccToNotDuplicateWithError,
            EmptyToUpdatedDraftBody,
            EmptyToUpdatedSubject,
            EmptyToCloseComposer,
            EmptyToCloseComposerWithDraftSaved,
            SubmittableToSendMessage,
            SubmittableToOnSendMessageOffline,
            EmptyToLoadingWithOpenExistingDraft,
            LoadingToFieldsWhenReceivedDraftDataEmptyRecipients,
            LoadingToFieldsWhenReceivedDraftDataValidRecipients,
            LoadingToErrorWhenErrorLoadingDraftData,
            EmptyToUpdatedMessageIdOnApiAssignedId,
            EmptyToBottomSheetOpened,
            EmptyToBottomSheetClosed,
            EmptyToAttachmentsUpdated
        )

        private fun aSubmittableState(
            draftId: MessageId,
            sender: SenderUiModel = SenderUiModel(""),
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList(),
            draftBody: String = "",
            subject: Subject = Subject(""),
            error: Effect<TextUiModel> = Effect.empty(),
            closeComposerWithMessageSending: Effect<Unit> = Effect.empty(),
            closeComposerWithMessageSendingOffline: Effect<Unit> = Effect.empty()
        ) = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = sender,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = subject.value,
                body = draftBody,
                quotedBody = null
            ),
            attachments = AttachmentGroupUiModel(attachments = emptyList()),
            premiumFeatureMessage = Effect.empty(),
            error = error,
            isSubmittable = true,
            senderAddresses = emptyList(),
            changeBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            isLoading = false,
            isAddAttachmentsButtonVisible = false,
            closeComposerWithMessageSending = closeComposerWithMessageSending,
            closeComposerWithMessageSendingOffline = closeComposerWithMessageSendingOffline
        )

        private fun aNotSubmittableState(
            draftId: MessageId,
            sender: SenderUiModel = SenderUiModel(""),
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList(),
            error: Effect<TextUiModel> = Effect.of(TextUiModel(R.string.composer_error_invalid_email)),
            premiumFeatureMessage: Effect<TextUiModel> = Effect.empty(),
            senderAddresses: List<SenderUiModel> = emptyList(),
            changeSenderBottomSheetVisibility: Effect<Boolean> = Effect.empty(),
            draftBody: String = "",
            subject: Subject = Subject(""),
            closeComposer: Effect<Unit> = Effect.empty(),
            closeComposerWithDraftSaved: Effect<Unit> = Effect.empty(),
            isLoading: Boolean = false
        ) = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = sender,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = subject.value,
                body = draftBody,
                quotedBody = null
            ),
            attachments = AttachmentGroupUiModel(attachments = emptyList()),
            premiumFeatureMessage = premiumFeatureMessage,
            error = error,
            isSubmittable = false,
            senderAddresses = senderAddresses,
            changeBottomSheetVisibility = changeSenderBottomSheetVisibility,
            closeComposer = closeComposer,
            closeComposerWithDraftSaved = closeComposerWithDraftSaved,
            isLoading = isLoading,
            isAddAttachmentsButtonVisible = false,
            closeComposerWithMessageSending = Effect.empty(),
            closeComposerWithMessageSendingOffline = Effect.empty()
        )

        private fun aPositiveRandomInt(bound: Int = 10) = Random().nextInt(bound)

        private fun aMultipleRandomRange(lowerBound: Int = 2, upperBound: Int = 10) =
            lowerBound until aPositiveRandomInt(upperBound) + 2 * lowerBound

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = transitions.map { test -> arrayOf(test.name, test) }

        data class TestTransition(
            val name: String,
            val currentState: ComposerDraftState,
            val operation: ComposerOperation,
            val expectedState: ComposerDraftState
        )
    }
}
