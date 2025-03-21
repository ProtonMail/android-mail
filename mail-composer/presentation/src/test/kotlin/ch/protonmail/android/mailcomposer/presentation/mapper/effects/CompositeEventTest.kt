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

import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AccessoriesStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

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

        private val senderEmail = SenderEmail("sender@email.com")
        private val draftContentReady = CompositeEvent.DraftContentReady(
            senderEmail = "sender@email.com",
            isDataRefreshed = false,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(senderEmail),
            quotedHtmlContent = null,
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = false
        )

        private val senderAddresses: List<SenderUiModel> = listOf(mockk())
        private val expiration = 2.hours

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "DraftContentReady to modification",
                draftContentReady,
                ComposerStateModifications(
                    mainModification = MainStateModification.OnDraftReady(
                        sender = senderEmail.value,
                        quotedHtmlContent = null,
                        shouldRestrictWebViewHeight = false
                    ),
                    effectsModification = ContentEffectsStateModifications.DraftContentReady(
                        draftContentReady.senderValidationResult,
                        draftContentReady.isDataRefreshed,
                        draftContentReady.forceBodyFocus
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
                    effectsModification = ConfirmationsEffectsStateModification.SendNoSubjectConfirmationRequested
                )
            ),
            arrayOf(
                "SetExpirationDismissed to modification",
                CompositeEvent.SetExpirationDismissed(expiration),
                ComposerStateModifications(
                    effectsModification = BottomSheetEffectsStateModification.HideBottomSheet,
                    accessoriesModification = AccessoriesStateModification.MessageExpirationUpdated(expiration)
                )
            ),
            arrayOf(
                "UserChangedSender to modification",
                CompositeEvent.UserChangedSender(senderEmail),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateSender(senderEmail),
                    effectsModification = BottomSheetEffectsStateModification.HideBottomSheet
                )
            )
        )
    }
}
