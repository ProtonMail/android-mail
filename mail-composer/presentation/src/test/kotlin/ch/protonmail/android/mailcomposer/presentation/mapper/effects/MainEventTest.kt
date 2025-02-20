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
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MainEventTest(
    @Suppress("unused") private val testName: String,
    private val effect: MainEvent,
    private val expectedModification: ComposerStateModifications
) {

    @Test
    fun `should map to the correct modification`() {
        val actualModification = effect.toStateModifications()
        assertEquals(expectedModification, actualModification)
    }

    companion object {
        private val senderEmail = SenderEmail("sender-email@proton.me")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "InitialLoadingToggled to modification",
                MainEvent.InitialLoadingToggled,
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.Initial)
                )
            ),
            arrayOf(
                "LoadingDismissed to modification",
                MainEvent.LoadingDismissed,
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.None)
                )
            ),
            arrayOf(
                "CoreLoadingToggled to modification",
                MainEvent.CoreLoadingToggled,
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateLoading(ComposerState.LoadingType.Save)
                )
            ),

            arrayOf(
                "RecipientsChanged (submittable) to modification",
                MainEvent.RecipientsChanged(areSubmittable = true),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateSubmittable(true)
                )
            ),
            arrayOf(
                "RecipientsChanged (non submittable) to modification",
                MainEvent.RecipientsChanged(areSubmittable = false),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateSubmittable(false)
                )
            ),
            arrayOf(
                "SenderChanged to modification",
                MainEvent.SenderChanged(newSender = senderEmail),
                ComposerStateModifications(
                    mainModification = MainStateModification.UpdateSender(senderEmail)
                )
            ),
            arrayOf(
                "OnQuotedHtmlRemoved to modification",
                MainEvent.OnQuotedHtmlRemoved,
                ComposerStateModifications(
                    mainModification = MainStateModification.RemoveHtmlQuotedText
                )
            )
        )
    }
}
