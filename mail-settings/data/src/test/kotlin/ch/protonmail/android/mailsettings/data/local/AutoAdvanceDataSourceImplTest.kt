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

package ch.protonmail.android.mailsettings.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMailSettings
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.NextMessageOnMove
import kotlin.test.assertEquals

internal class AutoAdvanceDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mailSettingsDataSource = mockk<MailSettingsDataSource>()
    private lateinit var dataSource: AutoAdvanceDataSourceImpl

    val mockSettings = mockk<LocalMailSettings> {
        every { this@mockk.nextMessageOnMove } returns NextMessageOnMove.ENABLED_EXPLICIT
    }

    @Before
    fun setup() {
        dataSource = AutoAdvanceDataSourceImpl(mailSettingsDataSource)
    }

    @Test
    fun `getAutoAdvance maps true on success when true`() = runTest {
        // Given
        coEvery { mailSettingsDataSource.observeMailSettings(userId) } returns flowOf(mockSettings)

        val expected = true

        // When
        val result = dataSource.getAutoAdvance(userId)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `getAutoAdvance maps false on success when false`() = runTest {
        // Given
        every { mockSettings.nextMessageOnMove } returns NextMessageOnMove.DISABLED_EXPLICIT
        coEvery { mailSettingsDataSource.observeMailSettings(userId) } returns flowOf(mockSettings)

        val expected = false

        // When
        val result = dataSource.getAutoAdvance(userId)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `getAutoAdvance returns error on failure fetching settings`() = runTest {
        // Given
        every { mockSettings.nextMessageOnMove } returns NextMessageOnMove.DISABLED_EXPLICIT
        coEvery { mailSettingsDataSource.observeMailSettings(userId) } returns flowOf()

        val expected = false

        // When
        val result = dataSource.getAutoAdvance(userId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    private companion object {

        val userId = UserId("user-id")
    }
}
