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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test

internal class ClearLocalStorageTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val localDataRepository = mockk<LocalStorageDataRepository>()

    private val clearLocalStorage = ClearLocalStorage(
        observePrimaryUserId,
        localDataRepository
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should not invoke data clearing if the user id cannot be fetched`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        clearLocalStorage.invoke(ClearDataAction.ClearAll)

        // Then
        verify { localDataRepository wasNot called }
    }

    @Test
    fun `should call the local repository with the primary userId when data clearing is requested`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(BaseUserId)
        every { localDataRepository.performClearData(BaseUserId, BaseClearDataAction) } just runs

        // When
        clearLocalStorage.invoke(BaseClearDataAction)

        // Then
        verify(exactly = 1) {
            localDataRepository.performClearData(BaseUserId, BaseClearDataAction)
        }
        confirmVerified(localDataRepository)
    }

    private companion object {

        val BaseUserId = UserId("userId")
        val BaseClearDataAction = ClearDataAction.ClearAll
    }
}

