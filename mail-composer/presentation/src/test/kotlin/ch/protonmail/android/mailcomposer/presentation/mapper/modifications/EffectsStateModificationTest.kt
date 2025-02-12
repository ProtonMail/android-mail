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

package ch.protonmail.android.mailcomposer.presentation.mapper.modifications

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAttachmentError
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.CompletionEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.EffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.LoadingError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.UnrecoverableError
import ch.protonmail.android.mailmessage.domain.model.Recipient
import io.mockk.mockk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class EffectsStateModificationTest(
    @Suppress("unused") private val testName: String,
    private val initialState: ComposerState.Effects,
    private val modification: EffectsStateModification,
    private val expectedState: ComposerState.Effects
) {

    @Test
    fun `should apply the modification`() {
        val updatedState = modification.apply(initialState)
        assertEquals(expectedState, updatedState)
    }

    companion object {

        private val initialState = ComposerState.Effects.initial()
        private val externalRecipients = listOf(mockk<Recipient>())

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "shows invalid sender error",
                initialState,
                UnrecoverableError.InvalidSenderAddress,
                initialState.copy(exitError = Effect.of(TextUiModel(R.string.composer_error_invalid_sender)))
            ),
            arrayOf(
                "shows draft content loading error",
                initialState,
                LoadingError.DraftContent,
                initialState.copy(error = Effect.of(TextUiModel(R.string.composer_error_loading_draft)))
            ),
            arrayOf(
                "shows parent message loading error",
                initialState,
                UnrecoverableError.ParentMessageMetadata,
                initialState.copy(exitError = Effect.of(TextUiModel(R.string.composer_error_loading_parent_message)))
            ),
            arrayOf(
                "shows free user sender change error (paid feature)",
                initialState,
                RecoverableError.SenderChange.FreeUser,
                initialState.copy(
                    premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature))
                )
            ),
            arrayOf(
                "shows failed getting permissions on sender change error",
                initialState,
                RecoverableError.SenderChange.UnknownPermissions,
                initialState.copy(
                    error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
                )
            ),
            arrayOf(
                "shows attachments not found error",
                initialState,
                RecoverableError.AttachmentsStore(StoreDraftWithAttachmentError.AttachmentsMissing),
                initialState.copy(error = Effect.of(TextUiModel.TextRes(R.string.composer_attachment_not_found)))
            ),
            arrayOf(
                "show file size exceeded error",
                initialState,
                RecoverableError.AttachmentsStore(StoreDraftWithAttachmentError.FileSizeExceedsLimit),
                initialState.copy(attachmentsFileSizeExceeded = Effect.of(Unit))
            ),
            arrayOf(
                "shows attachments re-encryption failed error",
                initialState,
                RecoverableError.ReEncryptAttachment,
                initialState.copy(
                    attachmentsReEncryptionFailed = Effect.of(
                        TextUiModel(R.string.composer_attachment_reencryption_failed_message)
                    )
                )
            ),
            arrayOf(
                "shows expiration error",
                initialState,
                RecoverableError.Expiration,
                initialState.copy(
                    error = Effect.of(TextUiModel(R.string.composer_error_setting_expiration_time)),
                    changeBottomSheetVisibility = Effect.of(false)
                )
            ),
            arrayOf(
                "shows sending failed error",
                initialState,
                RecoverableError.SendingFailed("Test error"),
                initialState.copy(sendingErrorEffect = Effect.of(TextUiModel.Text("Test error")))
            ),
            arrayOf(
                "shows image picker",
                initialState,
                ContentEffectsStateModifications.OnAddAttachmentRequested,
                initialState.copy(openImagePicker = Effect.of(Unit))
            ),
            arrayOf(
                "handles draft content ready with valid sender",
                initialState,
                ContentEffectsStateModifications.DraftContentReady(
                    ValidateSenderAddress.ValidationResult.Valid(SenderEmail("test@example.com")),
                    isDataRefresh = true,
                    forceBodyFocus = true
                ),
                initialState.copy(focusTextBody = Effect.of(Unit))
            ),
            arrayOf(
                "handles draft content ready with paid address error",
                initialState,
                ContentEffectsStateModifications.DraftContentReady(
                    ValidateSenderAddress.ValidationResult.Invalid(
                        validAddress = SenderEmail("test@proton.me"),
                        invalid = SenderEmail("a@b.c"),
                        reason = ValidateSenderAddress.ValidationError.PaidAddress
                    ),
                    isDataRefresh = true,
                    forceBodyFocus = false
                ),
                initialState.copy(
                    senderChangedNotice = Effect.of(
                        TextUiModel(R.string.composer_sender_changed_pm_address_is_a_paid_feature)
                    )
                )
            ),
            arrayOf(
                "handles draft content ready with disabled address error",
                initialState,
                ContentEffectsStateModifications.DraftContentReady(
                    ValidateSenderAddress.ValidationResult.Invalid(
                        validAddress = SenderEmail("test@proton.me"),
                        invalid = SenderEmail("a@b.c"),
                        reason = ValidateSenderAddress.ValidationError.DisabledAddress
                    ),
                    isDataRefresh = true,
                    forceBodyFocus = false
                ),
                initialState.copy(
                    senderChangedNotice = Effect.of(
                        TextUiModel(R.string.composer_sender_changed_original_address_disabled)
                    )
                )
            ),
            arrayOf(
                "closes composer with saved draft",
                initialState,
                CompletionEffectsStateModification.CloseComposer(hasSavedDraft = true),
                initialState.copy(closeComposerWithDraftSaved = Effect.of(Unit))
            ),
            arrayOf(
                "closes composer without saved draft",
                initialState,
                CompletionEffectsStateModification.CloseComposer(hasSavedDraft = false),
                initialState.copy(closeComposer = Effect.of(Unit))
            ),
            arrayOf(
                "sends message and exit",
                initialState,
                CompletionEffectsStateModification.SendMessage.SendAndExit,
                initialState.copy(closeComposerWithMessageSending = Effect.of(Unit))
            ),
            arrayOf(
                "sends message and exit offline",
                initialState,
                CompletionEffectsStateModification.SendMessage.SendAndExitOffline,
                initialState.copy(closeComposerWithMessageSendingOffline = Effect.of(Unit))
            ),
            arrayOf(
                "shows bottom sheet",
                initialState,
                BottomSheetEffectsStateModification.ShowBottomSheet,
                initialState.copy(changeBottomSheetVisibility = Effect.of(true))
            ),
            arrayOf(
                "hides bottom sheet",
                initialState,
                BottomSheetEffectsStateModification.HideBottomSheet,
                initialState.copy(changeBottomSheetVisibility = Effect.of(false))
            ),
            arrayOf(
                "requests no subject confirmation",
                initialState,
                ConfirmationsEffectsStateModification.SendNoSubjectConfirmationRequested,
                initialState.copy(confirmSendingWithoutSubject = Effect.of(Unit))
            ),
            arrayOf(
                "cancels no subject confirmation",
                initialState,
                ConfirmationsEffectsStateModification.CancelSendNoSubject,
                initialState.copy(
                    changeFocusToField = Effect.of(FocusedFieldType.SUBJECT),
                    confirmSendingWithoutSubject = Effect.empty()
                )
            ),
            arrayOf(
                "shows external expiring recipients confirmation",
                initialState,
                ConfirmationsEffectsStateModification.ShowExternalExpiringRecipients(externalRecipients),
                initialState.copy(confirmSendExpiringMessage = Effect.of(externalRecipients))
            )
        )
    }
}
