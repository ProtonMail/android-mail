/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnps.presentation.viewmodel

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailnps.domain.usecase.DismissNps
import ch.protonmail.android.mailnps.domain.usecase.SubmitNpsFeedback
import ch.protonmail.android.mailnps.presentation.model.NpsEligibilityState
import ch.protonmail.android.mailnps.presentation.model.NpsOperation.NpsAction
import ch.protonmail.android.mailnps.presentation.reducer.NpsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class NpsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val submitNpsFeedback = mockk<SubmitNpsFeedback> {
        coEvery { this@mockk.invoke(userId, NpsScore(SCORE), any()) } returns Unit.right()
    }
    private val dismissNps = mockk<DismissNps> {
        coEvery { this@mockk.invoke(any()) } returns Unit.right()
    }
    private val isNpsFeedbackEnabled = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns true
    }
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private fun viewModel() = NpsViewModel(
        submitNpsFeedback,
        dismissNps,
        NpsReducer(),
        isNpsFeedbackEnabled,
        observePrimaryUserId
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a score is selected, when observed, then it is reflected in state`() = runTest {
        // Given
        val viewModel = viewModel()

        // When + Then
        viewModel.state.test {
            assertNull(awaitItem().selectedScore)

            viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))

            val selected = awaitItem()
            assertEquals(NpsScore(SCORE), selected.selectedScore)
            assertTrue(selected.isScoreSelected)
        }
    }

    @Test
    fun `given a selected score, when shared, then feedback is recorded and the thank you shown`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))
        viewModel.submit(NpsAction.CommentChanged("nice"))

        // When
        viewModel.submit(NpsAction.ShareClicked)

        // Then
        coVerify(exactly = 1) { submitNpsFeedback(userId, NpsScore(SCORE), "nice") }
        val state = viewModel.state.value
        assertNotNull(state.showThankYou.consume())
        assertNotNull(state.dismiss.consume())
    }

    @Test
    fun `given the prompt, when the sheet is dismissed, then the dismissal is recorded`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(NpsAction.SheetDismissed)

        // Then
        coVerify(exactly = 1) { dismissNps(userId) }
        val state = viewModel.state.value
        assertTrue(state.isAnswered)
        assertNull(state.showThankYou.consume())
    }

    @Test
    fun `given a shared score, when the sheet closes, then no dismissal is recorded`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))
        viewModel.submit(NpsAction.ShareClicked)

        // When
        viewModel.submit(NpsAction.SheetDismissed)

        // Then
        coVerify(exactly = 0) { dismissNps(any()) }
    }

    @Test
    fun `given a score is being sent, when Share is tapped again, then it is recorded once`() = runTest {
        // Given
        val response = CompletableDeferred<Either<DataError, Unit>>()
        coEvery { submitNpsFeedback(userId, NpsScore(SCORE), any()) } coAnswers { response.await() }
        val viewModel = viewModel()
        viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))
        viewModel.submit(NpsAction.ShareClicked)

        // When
        viewModel.submit(NpsAction.ShareClicked)
        response.complete(Unit.right())

        // Then
        coVerify(exactly = 1) { submitNpsFeedback(userId, NpsScore(SCORE), any()) }
    }

    @Test
    fun `given a score is being sent, when the sheet is dismissed, then no dismissal is recorded`() = runTest {
        // Given
        val response = CompletableDeferred<Either<DataError, Unit>>()
        coEvery { submitNpsFeedback(userId, NpsScore(SCORE), any()) } coAnswers { response.await() }
        val viewModel = viewModel()
        viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))
        viewModel.submit(NpsAction.ShareClicked)

        // When
        viewModel.submit(NpsAction.SheetDismissed)
        response.complete(Unit.right())

        // Then
        coVerify(exactly = 0) { dismissNps(any()) }
    }

    @Test
    fun `given a selected score, when Share is tapped, then the Share button is disabled right away`() = runTest {
        // Given
        val response = CompletableDeferred<Either<DataError, Unit>>()
        coEvery { submitNpsFeedback(userId, NpsScore(SCORE), any()) } coAnswers { response.await() }
        val viewModel = viewModel()
        viewModel.submit(NpsAction.ScoreSelected(NpsScore(SCORE)))

        // When
        viewModel.submit(NpsAction.ShareClicked)

        // Then
        assertFalse(viewModel.state.value.isShareEnabled)
        response.complete(Unit.right())
    }

    @Test
    fun `given the NPS flag is enabled, when eligibility is observed, then the survey is shown`() = runTest {
        // Given
        coEvery { isNpsFeedbackEnabled.get() } returns true

        // When
        val viewModel = viewModel()

        // Then
        viewModel.eligibility.test {
            assertEquals(NpsEligibilityState.Show, awaitItem())
        }
    }

    @Test
    fun `given the NPS flag is disabled, when eligibility is observed, then the survey is hidden`() = runTest {
        // Given
        coEvery { isNpsFeedbackEnabled.get() } returns false

        // When
        val viewModel = viewModel()

        // Then
        viewModel.eligibility.test {
            assertEquals(NpsEligibilityState.Hide, awaitItem())
        }
    }

    @Test
    fun `given no signed in user, when eligibility is observed, then it stays loading`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        val viewModel = viewModel()

        // Then
        viewModel.eligibility.test {
            assertEquals(NpsEligibilityState.Loading, awaitItem())
            expectNoEvents()
        }
    }

    private companion object {

        val userId = UserId("user")
        const val SCORE = 9
    }
}
