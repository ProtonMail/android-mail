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
import ch.protonmail.android.mailnps.domain.repository.NpsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class DismissNpsTest {

    private val npsRepository = mockk<NpsRepository> {
        coEvery { dismiss(any()) } returns Unit.right()
    }
    private val dismissNps = DismissNps(npsRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the prompt is dismissed, when invoked, then the dismissal is recorded`() = runTest {
        // When
        dismissNps(userId)

        // Then
        coVerify(exactly = 1) { npsRepository.dismiss(userId) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
