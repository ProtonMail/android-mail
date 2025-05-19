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

package ch.protonmail.android.mailsettings.data.repository

import androidx.work.Constraints
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailsettings.data.local.ClearLocalDataWorker
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test

internal class LocalStorageDataRepositoryImplTest {

    private val enqueuer = mockk<Enqueuer>()
    private val localDataRepository = LocalStorageDataRepositoryImpl(enqueuer)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should enqueue a worker with the correct params when perform clear data is invoked`() {
        // Given
        every {
            enqueuer.enqueueUniqueWork<ClearLocalDataWorker>(
                userId = BaseUserId,
                workerId = BaseWorkerId,
                params = BaseParams,
                constraints = BaseConstraints
            )
        } returns Unit

        // When
        localDataRepository.performClearData(BaseUserId, BaseClearAction)

        // Then
        verify(exactly = 1) {
            enqueuer.enqueueUniqueWork<ClearLocalDataWorker>(
                userId = BaseUserId,
                workerId = BaseWorkerId,
                params = BaseParams,
                constraints = BaseConstraints
            )
        }
    }

    private companion object {

        val BaseUserId = UserId("userId")
        val BaseClearAction = ClearDataAction.ClearAll
        val BaseWorkerId = "ClearLocalDataWorker-${BaseClearAction.hashCode()}"
        val BaseConstraints = Constraints.Builder().build()
        val BaseParams = ClearLocalDataWorker.params(BaseClearAction)
    }
}
