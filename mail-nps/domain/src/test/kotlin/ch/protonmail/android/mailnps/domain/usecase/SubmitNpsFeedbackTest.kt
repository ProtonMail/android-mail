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

package ch.protonmail.android.mailnps.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailnps.domain.repository.NpsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import io.mockk.unmockkAll

internal class SubmitNpsFeedbackTest {

    private val npsRepository = mockk<NpsRepository> {
        coEvery { recordScore(userId, NpsScore(SCORE), any()) } returns Unit.right()
    }
    private val submitNpsFeedback = SubmitNpsFeedback(npsRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a score and comment, when submitted, then the rated response is recorded`() = runTest {
        // When
        submitNpsFeedback(userId, NpsScore(SCORE), "  loved it  ")

        // Then
        coVerify(exactly = 1) { npsRepository.recordScore(userId, NpsScore(SCORE), "loved it") }
    }

    @Test
    fun `given a blank comment, when submitted, then the comment is normalised to null`() = runTest {
        // When
        submitNpsFeedback(userId, NpsScore(SCORE), "   ")

        // Then
        coVerify(exactly = 1) { npsRepository.recordScore(userId, NpsScore(SCORE), null) }
    }

    private companion object {

        val userId = UserId("user")
        const val SCORE = 8
    }
}
