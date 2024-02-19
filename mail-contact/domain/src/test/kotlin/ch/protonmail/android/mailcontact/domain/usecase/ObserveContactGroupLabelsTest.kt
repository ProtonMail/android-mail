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

package ch.protonmail.android.mailcontact.domain.usecase

import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveContactGroupLabelsTest {

    private val contactGroupLabels = listOf(
        LabelTestData.buildLabel(
            "LabelId1",
            UserIdTestData.userId,
            LabelType.ContactGroup,
            "Label 1"
        )
    )
    private val repository = mockk<LabelRepository> {
        every { this@mockk.observeLabels(UserIdTestData.userId, LabelType.ContactGroup) } returns flowOf(
            DataResult.Success(
                ResponseSource.Remote,
                contactGroupLabels
            )
        )
    }

    private val observeContactGroupLabels = ObserveContactGroupLabels(repository)

    @Test
    fun `when repository returns labels they are successfully emitted`() = runTest {
        // When
        observeContactGroupLabels(UserIdTestData.userId).test {
            // Then
            val actual = assertIs<Either.Right<List<Label>>>(awaitItem())
            assertEquals(contactGroupLabels, actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when repository returns any data error then emit get contact groups error`() = runTest {
        // Given
        every { repository.observeLabels(UserIdTestData.userId, LabelType.ContactGroup) } returns flowOf(
            DataResult.Error.Remote(message = "Unauthorised", cause = null, httpCode = 401)
        )
        // When
        observeContactGroupLabels(UserIdTestData.userId).test {
            // Then
            assertIs<Either.Left<GetContactGroupLabelsError>>(awaitItem())
            awaitComplete()
        }
    }
}
