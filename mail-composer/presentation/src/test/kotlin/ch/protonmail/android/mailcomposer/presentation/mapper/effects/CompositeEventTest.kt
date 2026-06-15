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

package ch.protonmail.android.mailcomposer.presentation.mapper.effects

import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailattachments.domain.model.RemoveAttachmentError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentAddErrorWithList
import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftHead
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AttachmentsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendExpirationSupportUnknownConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendExpirationUnsupportedForSomeConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendNoSubjectConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class CompositeEventTest(
    @Suppress("unused") private val testName: String,
    private val effect: CompositeEvent,
    private val expectedModification: ComposerStateModifications
) {

    @Test
    fun `should map to the correct modification`() {
        val actualModification = effect.toStateModifications()
        assertEquals(expectedModification, actualModification)
    }

    companion object {

        private val expirationRecipients = listOf("external@foo.com")
        private val senderEmail = SenderEmail("sender@email.com")
        private val draftDisplayBody = DraftDisplayBodyUiModel("<html>draft display body</html>")
        private val draftFields = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            BodyFields(
                DraftHead("The head of this draft"),
                DraftBody("Decrypted body of this draft")
            ),
            DraftMimeType.Html,
            RecipientsTo(
                listOf(DraftRecipient.SingleRecipient("Name", "you@proton.ch", privacyLock = PrivacyLock.None))
            ),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList())
        )
        private val draftUiModel = DraftUiModel(draftFields, draftDisplayBody)

        private val draftContentReady = CompositeEvent.DraftContentReady(
            draftUiModel = draftUiModel,
            isDataRefreshed = false,
            bodyShouldTakeFocus = false
        )

        private val senderAddresses: List<SenderUiModel> = listOf(mockk())

        private val noErrorAttachment = mockk<AttachmentMetadataWithState>().apply {
            every { attachmentState } returns AttachmentState.Uploaded
        }

        private val errorAttachment = mockk<AttachmentMetadataWithState>().apply {
            every { attachmentState } returns AttachmentState.Error(
                AttachmentError.AddAttachment(AddAttachmentError.TooManyAttachments)
            )
        }
        private val removeErrorAttachment = mockk<AttachmentMetadataWithState>().apply {
            every { attachmentState } returns AttachmentState.Error(
                AttachmentError.RemoveAttachment(RemoveAttachmentError.BadRequest("Server error message"))
            )
        }
        private val errorList = listOf(errorAttachment)
        private val removeErrorList = listOf(removeErrorAttachment)
        private val noErrorList = listOf(noErrorAttachment)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "DraftContentReady to modification",
                draftContentReady,
                ComposerStateModifications(
                    mainModification = MainStateModification.OnDraftReady(
                        draftUiModel = draftUiModel,
                        bodyShouldTakeFocus = draftContentReady.bodyShouldTakeFocus
                    ),
                    effectsModification = ContentEffectsStateModifications.DraftContentReady(
                        draftUiModel,
                        draftContentReady.isDataRefreshed,
                        draftContentReady.bodyShouldTakeFocus
                    )
                )
            ),
            arrayOf(
                "SenderAddressesListReady to modification",
                CompositeEvent.SenderAddressesListReady(senderAddresses),
                ComposerStateModifications(
                    mainModification = MainStateModification.SendersListReady(senderAddresses),
                    effectsModification = BottomSheetEffectsStateModification.ShowBottomSheet
                )
            ),
            arrayOf(
                "OnSendWithEmptySubject to modification",
                CompositeEvent.OnSendWithEmptySubject,
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.None),
                    effectsModification = SendNoSubjectConfirmationRequested
                )
            ),
            arrayOf(
                "UserChangedSender to modification",
                CompositeEvent.UserChangedSender(senderEmail, draftDisplayBody),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateSender(senderEmail),
                    effectsModification = ContentEffectsStateModifications.DraftSenderChanged(draftDisplayBody)
                )
            ),
            arrayOf(
                "AttachmentListChanged with error to modification",
                CompositeEvent.AttachmentListChanged(errorList),
                ComposerStateModifications(
                    attachmentsModification = AttachmentsStateModification.ListUpdated(errorList),
                    effectsModification = RecoverableError.AttachmentsListChangedWithError(
                        attachmentAddErrorWithList = AttachmentAddErrorWithList(
                            AddAttachmentError.TooManyAttachments,
                            errorList
                        )
                    )
                )
            ),
            arrayOf(
                "AttachmentListChanged with remove error to modification",
                CompositeEvent.AttachmentListChanged(removeErrorList),
                ComposerStateModifications(
                    attachmentsModification = AttachmentsStateModification.ListUpdated(removeErrorList),
                    effectsModification = RecoverableError.AttachmentRemoveFailed(
                        RemoveAttachmentError.BadRequest("Server error message")
                    )
                )
            ),
            arrayOf(
                "AttachmentListChanged with no error to modification",
                CompositeEvent.AttachmentListChanged(noErrorList),
                ComposerStateModifications(
                    attachmentsModification = AttachmentsStateModification.ListUpdated(noErrorList),
                    effectsModification = null
                )
            ),
            arrayOf(
                "On Send with expiration may fail to modification",
                CompositeEvent.OnSendWithExpirationSupportUnknown,
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.None),
                    effectsModification = SendExpirationSupportUnknownConfirmationRequested
                )
            ),
            arrayOf(
                "On Send with expiration will fail to modification",
                CompositeEvent.OnSendWithExpirationUnsupportedForSome(expirationRecipients),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.None),
                    effectsModification = SendExpirationUnsupportedForSomeConfirmationRequested(expirationRecipients)
                )
            )
        )
    }
}
