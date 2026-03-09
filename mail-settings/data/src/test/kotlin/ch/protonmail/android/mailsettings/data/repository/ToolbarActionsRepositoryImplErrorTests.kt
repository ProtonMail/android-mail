/*
 * Copyright (c) 2025 Proton Technologies AG
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

import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.data.local.ToolbarActionSettingsDataSource
import ch.protonmail.android.mailsettings.domain.model.ToolbarType
import ch.protonmail.android.mailsettings.domain.repository.ToolbarActionsRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MobileAction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ToolbarActionsRepositoryImplErrorTest {

    private val dataSource = mockk<ToolbarActionSettingsDataSource>()
    private lateinit var repo: ToolbarActionsRepository

    @BeforeTest
    fun setup() {
        repo = ToolbarActionsRepositoryImpl(dataSource)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should propagate the error on read`() = runTest {
        // Given
        coEvery { dataSource.getMessageActions(userId) } returns DataError.Local.NoDataCached.left()

        // When
        val result = repo.getToolbarActions(userId, ToolbarType.Message)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
        coVerify(exactly = 1) { dataSource.getMessageActions(userId) }
        confirmVerified(dataSource)
    }

    @Test
    fun `should propagate the error on save`() = runTest {
        // Given
        val actions = listOf(Action.ReportPhishing, Action.Move)
        val rustActions = listOf(MobileAction.REPORT_PHISHING, MobileAction.MOVE)
        coEvery { dataSource.updateMessageActions(userId, rustActions) } returns DataError.Local.NoDataCached.left()

        // When
        val result = repo.saveActions(userId, ToolbarType.Message, actions)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
        coVerify(exactly = 1) { dataSource.updateMessageActions(userId, rustActions) }
        confirmVerified(dataSource)
    }

    private companion object {

        val userId = UserId("user-id")
    }
}
