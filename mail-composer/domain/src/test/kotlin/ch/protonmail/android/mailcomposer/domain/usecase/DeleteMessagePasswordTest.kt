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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteMessagePasswordTest {

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val messagePasswordRepository = mockk<MessagePasswordRepository>()

    private val deleteMessagePassword = DeleteMessagePassword(messagePasswordRepository)

    @Test
    fun `should call delete method from repository when deleting message password`() = runTest {
        // Given
        coEvery { messagePasswordRepository.deleteMessagePassword(userId, messageId) } just runs

        // When
        deleteMessagePassword(userId, messageId)

        // Then
        coVerify { messagePasswordRepository.deleteMessagePassword(userId, messageId) }
    }
}
