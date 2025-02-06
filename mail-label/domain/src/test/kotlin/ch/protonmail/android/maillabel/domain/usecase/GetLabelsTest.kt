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

package ch.protonmail.android.maillabel.domain.usecase

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class GetLabelsTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val messageLabels = listOf(
        buildLabel(userId = userId, type = LabelType.MessageLabel, id = "0"),
        buildLabel(userId = userId, type = LabelType.MessageLabel, id = "1"),
        buildLabel(userId = userId, type = LabelType.MessageLabel, id = "2")
    )
    private val messageFolders = listOf(
        buildLabel(userId = userId, type = LabelType.MessageFolder, id = "3"),
        buildLabel(userId = userId, type = LabelType.MessageFolder, id = "4"),
        buildLabel(userId = userId, type = LabelType.MessageFolder, id = "5")
    )
    private val labelRepository = mockk<LabelRepository> {
        coEvery { getLabels(userId, LabelType.MessageLabel) } returns messageLabels
        coEvery { getLabels(userId, LabelType.MessageFolder) } returns messageFolders
    }

    private val getLabels = GetLabels(labelRepository)

    @Test
    fun `returns labels when repository succeeds`() = runTest {
        // When
        val actual = getLabels(userId, LabelType.MessageLabel)
        // Then
        assertEquals(messageLabels.right(), actual)
    }

    @Test
    fun `returns labels of folder type when repository succeeds`() = runTest {
        // When
        val actual = getLabels(userId, LabelType.MessageFolder)
        // Then
        assertEquals(messageFolders.right(), actual)
    }

    @Test
    fun `returns no network error when repository fails throwing Unknown Host Exception`() = runTest {
        // Given
        coEvery { getLabels(userId, LabelType.MessageLabel) } throws UnknownHostException("Unable to resolve host")
        // When
        val actual = getLabels(userId, LabelType.MessageLabel)
        // Then
        val networkError = DataError.Remote.Http(NetworkError.NoNetwork)
        assertEquals(networkError.left(), actual)
    }

    @Test
    fun `returns unreachable error when repository fails throwing Socket Timeout Exception`() = runTest {
        // Given
        coEvery { getLabels(userId, LabelType.MessageLabel) } throws SocketTimeoutException("Connection timed out")
        // When
        val actual = getLabels(userId, LabelType.MessageLabel)
        // Then
        val networkError = DataError.Remote.Http(NetworkError.Unreachable)
        assertEquals(networkError.left(), actual)
    }

    @Test
    fun `logs and returns unknown error when repository fails throwing unexpected exception`() = runTest {
        // Given
        val exception = Exception("Unexpected error")
        coEvery { getLabels(userId, LabelType.MessageLabel) } throws exception
        // When
        val actual = getLabels(userId, LabelType.MessageLabel)
        // Then
        val networkError = DataError.Remote.Http(NetworkError.Unknown)
        val loggedError = "Unknown error while getting labels: $exception"
        assertEquals(networkError.left(), actual)
        loggingTestRule.assertErrorLogged(loggedError)
    }

    @Test
    fun `explicitly require labels repository not to refresh local data`() = runTest {
        // This is in the first place to ensure un-needed network calls are not performed
        // as labels and folders will be fetched the first time (happening implicitly when
        // there is no data locally, no "refresh" needed) and then kept up-to-date through the event loop.
        // **
        // note that due to reasons that weren't fully investigated, forcing this to "true" causes the usage of
        // `getLabels` done from the paging library (through GetMailboxItems) to enter an infinite loop of network calls
        // **

        // When
        getLabels(userId, LabelType.MessageLabel)
        // Then
        coVerify { labelRepository.getLabels(userId, LabelType.MessageLabel, false) }
    }

    @Test
    fun `filters folders with deleted parents`() = runTest {
        // Given
        val localItems = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id3", order = 0, parentId = null),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id4", order = 1, parentId = "id3"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id5", order = 1, parentId = "id4"),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id6", order = 2,
                parentId = "already_deleted"
            ),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id7", order = 2,
                parentId = "id6"
            ),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id8", order = 2,
                parentId = "id7"
            )
        )
        coEvery { labelRepository.getLabels(userId, LabelType.MessageFolder) } returns localItems


        // When
        val actual = getLabels(userId, LabelType.MessageFolder)
        // Then
        val expectedLabels = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id3", order = 0, parentId = null),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id4", order = 1, parentId = "id3"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id5", order = 1, parentId = "id4")
        )
        assertContentEquals(expectedLabels, actual.getOrNull())
    }
}
