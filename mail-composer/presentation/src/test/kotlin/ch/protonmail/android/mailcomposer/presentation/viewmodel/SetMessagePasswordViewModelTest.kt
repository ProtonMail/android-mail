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
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessagePassword
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class SetMessagePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val saveMessagePassword = mockk<SaveMessagePassword>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(SetMessagePasswordScreen.DraftMessageIdKey) } returns messageId.id
    }

    private val setMessagePasswordViewModel = SetMessagePasswordViewModel(
        saveMessagePassword,
        observePrimaryUserId,
        savedStateHandle
    )

    @Test
    fun `should save message password and close the screen when apply password action is submitted`() = runTest {
        // Given
        val password = "password"
        val passwordHint = "password hint"
        coEvery { saveMessagePassword(userId, messageId, password, passwordHint) } returns Unit.right()

        // When
        setMessagePasswordViewModel.submit(MessagePasswordAction.ApplyPassword(password, passwordHint))

        // Then
        setMessagePasswordViewModel.state.test {
            val expected = SetMessagePasswordState(exitScreen = Effect.of(Unit))
            assertEquals(expected, awaitItem())
            coVerify { saveMessagePassword(userId, messageId, password, passwordHint) }
        }
    }
}
