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

import java.util.UUID
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsBccChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsCcChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsToChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Invalid
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Valid
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import io.mockk.coEvery
import io.mockk.mockk
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
    private val getComposerSenderAddresses = mockk<GetComposerSenderAddresses> {
        val result = when (testTransition.mockSenderAddress) {
            MockSenderAddress.Error -> GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription.left()
            MockSenderAddress.NotAllowed -> GetComposerSenderAddresses.Error.UpgradeToChangeSender.left()
            MockSenderAddress.Success -> addresses.right()
        }
        coEvery { this@mockk.invoke() } returns result
    }

    private val composerReducer = ComposerReducer(getComposerSenderAddresses)

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


        private val EmptyToSubmittableToField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the to field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsToChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableToField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the to field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsToChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, to = listOf(Invalid(this)))
            )
        }

        private val EmptyToSubmittableCcField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the cc field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsCcChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, cc = listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableCcField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the cc field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsCcChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, cc = listOf(Invalid(this)))
            )
        }

        private val EmptyToSubmittableBccField = with("a@b.c") {
            TestTransition(
                name = "Should generate submittable state when adding a new valid email address in the bcc field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsBccChanged(listOf(Valid(this))),
                expectedState = aSubmittableState(messageId, bcc = listOf(Valid(this)))
            )
        }

        private val EmptyToNotSubmittableBccField = with(UUID.randomUUID().toString()) {
            TestTransition(
                name = "Should generate not submittable error state when adding invalid email address in the bcc field",
                currentState = ComposerDraftState.empty(messageId),
                operation = RecipientsBccChanged(listOf(Invalid(this))),
                expectedState = aNotSubmittableState(messageId, bcc = listOf(Invalid(this)))
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

        private val EmptyToUpgradePlan = with(UUID.randomUUID().toString()) {
            val premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature))
            TestTransition(
                name = "Should generate a state showing 'upgrade plan' message when free user tries to change sender",
                currentState = ComposerDraftState.empty(messageId),
                operation = ComposerAction.OnChangeSender,
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    premiumFeatureMessage = premiumFeatureMessage,
                    error = Effect.empty()
                ),
                mockSenderAddress = MockSenderAddress.NotAllowed
            )
        }

        private val EmptyToSenderAddressesList = TestTransition(
            name = "Should generate a state showing change sender bottom sheet when paid tries to change sender",
            currentState = ComposerDraftState.empty(messageId),
            operation = ComposerAction.OnChangeSender,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                error = Effect.empty(),
                senderAddresses = addresses.map { SenderUiModel(it.email) },
                changeSenderBottomSheetVisibility = Effect.of(true)
            ),
            mockSenderAddress = MockSenderAddress.Success
        )

        private val EmptyToErrorWhenUserPlanUnknown = with(UUID.randomUUID().toString()) {
            val error = TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription)

            TestTransition(
                name = "Should generate an error state when failing to determine if user can change sender",
                currentState = ComposerDraftState.empty(messageId),
                operation = ComposerAction.OnChangeSender,
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    error = Effect.of(error)
                ),
                mockSenderAddress = MockSenderAddress.Error
            )
        }

        private val EmptyToUpdatedSender = with(UUID.randomUUID().toString()) {
            val sender = SenderUiModel("updated-sender@proton.ch")
            TestTransition(
                name = "Should update the state with the new sender and close bottom sheet when address changes",
                currentState = ComposerDraftState.empty(messageId),
                operation = ComposerAction.SenderChanged(sender),
                expectedState = aNotSubmittableState(
                    draftId = messageId,
                    sender = sender,
                    error = Effect.empty(),
                    changeSenderBottomSheetVisibility = Effect.of(false)
                )
            )
        }

        private val DefaultSenderToChangeSenderFailed = TestTransition(
            name = "Should update the state showing an error and preserving the previous sender address",
            currentState = aNotSubmittableState(
                draftId = messageId,
                sender = SenderUiModel("default@pm.me")
            ),
            operation = ComposerEvent.ChangeSenderFailed,
            expectedState = aNotSubmittableState(
                draftId = messageId,
                sender = SenderUiModel("default@pm.me"),
                error = Effect.of(TextUiModel(R.string.composer_error_resolving_sender_address)),
                changeSenderBottomSheetVisibility = Effect.of(false)
            )
        )


        private val transitions = listOf(
            EmptyToSubmittableToField,
            EmptyToNotSubmittableToField,
            EmptyToSubmittableCcField,
            EmptyToNotSubmittableCcField,
            EmptyToSubmittableBccField,
            EmptyToNotSubmittableBccField,
            NotSubmittableToWithoutErrorToField,
            NotSubmittableToWithErrorToField,
            NotSubmittableWithoutErrorWhenRemoving,
            EmptyToUpgradePlan,
            EmptyToSenderAddressesList,
            EmptyToErrorWhenUserPlanUnknown,
            EmptyToUpdatedSender,
            DefaultSenderToChangeSenderFailed
        )

        private fun aSubmittableState(
            draftId: MessageId,
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList()
        ) = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = SenderUiModel(""),
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            ),
            premiumFeatureMessage = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = true,
            senderAddresses = emptyList(),
            changeSenderBottomSheetVisibility = Effect.empty()
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
            changeSenderBottomSheetVisibility: Effect<Boolean> = Effect.empty()
        ) = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = sender,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            ),
            error = error,
            premiumFeatureMessage = premiumFeatureMessage,
            isSubmittable = false,
            senderAddresses = senderAddresses,
            changeSenderBottomSheetVisibility = changeSenderBottomSheetVisibility
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = transitions.map { test -> arrayOf(test.name, test) }

        data class TestTransition(
            val name: String,
            val currentState: ComposerDraftState,
            val operation: ComposerOperation,
            val expectedState: ComposerDraftState,
            val mockSenderAddress: MockSenderAddress = MockSenderAddress.Success
        )

        sealed interface MockSenderAddress {
            object Success : MockSenderAddress
            object NotAllowed : MockSenderAddress
            object Error : MockSenderAddress
        }
    }
}
