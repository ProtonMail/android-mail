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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsBccChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsCcChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsToChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.NotSubmittable
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.Submittable
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Invalid
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Valid
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ComposerReducerTest(
    private val testName: String,
    private val testTransition: TestTransition
) {

    private val composerReducer = ComposerReducer()

    @Test
    fun `Test composer transition states`() = with(testTransition) {
        val actualState = composerReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val messageId = MessageId(UUID.randomUUID().toString())

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

        private val transitions = listOf(
            EmptyToSubmittableToField,
            EmptyToNotSubmittableToField,
            EmptyToSubmittableCcField,
            EmptyToNotSubmittableCcField,
            EmptyToSubmittableBccField,
            EmptyToNotSubmittableBccField,
            NotSubmittableToWithoutErrorToField,
            NotSubmittableToWithErrorToField,
            NotSubmittableWithoutErrorWhenRemoving
        )

        private fun aSubmittableState(
            draftId: MessageId,
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList()
        ) = Submittable(
            fields = ComposerFields(
                draftId = draftId,
                from = "",
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            )
        )

        private fun aNotSubmittableState(
            draftId: MessageId,
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList(),
            error: Effect<TextUiModel> = Effect.of(TextUiModel(R.string.composer_error_invalid_email))
        ) = NotSubmittable(
            fields = ComposerFields(
                draftId = draftId,
                from = "",
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            ),
            error = error
        )

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
