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

import java.time.Instant
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.user.domain.UserManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.io.encoding.ExperimentalEncodingApi

@ExperimentalEncodingApi
class RefreshComposerSenderAddressesTest {

    private val userId = UserIdSample.Primary

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val userManager = mockk<UserManager>()

    private val sut by lazy {
        RefreshComposerSenderAddresses(
            observePrimaryUserId,
            userManager
        )
    }

    @Before
    fun setup() {
        mockkStatic(Instant::class)
    }

    @After
    fun teardown() {
        unmockkAll()
        unmockkStatic(Instant::class)
    }

    @Test
    fun `fetches fresh emails when called initially`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        coEvery { userManager.getAddresses(userId, true) } returns emptyList()
        mockInstant(1000L)

        // When
        sut()

        // Then

        coVerify(exactly = 1) { observePrimaryUserId.invoke() }
        coVerify(exactly = 1) { userManager.getAddresses(userId, true) }
    }

    @Test
    fun `does not fetch again within a short time`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        coEvery { userManager.getAddresses(userId, true) } returns emptyList()
        val initialTime = 1000L
        mockInstant(initialTime)

        // When
        sut()
        mockInstant(initialTime + 1000L)
        sut()

        // Then

        coVerify(exactly = 2) { observePrimaryUserId.invoke() }
        coVerify(exactly = 1) { userManager.getAddresses(userId, true) }
    }

    @Test
    fun `fetches again after a longer time`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        coEvery { userManager.getAddresses(userId, true) } returns emptyList()

        // When
        val initialTime = 1000L
        mockInstant(initialTime)
        sut()
        mockInstant(initialTime + 20 * 60_000L)
        sut()

        // Then

        coVerify(exactly = 2) { observePrimaryUserId.invoke() }
        coVerify(exactly = 2) { userManager.getAddresses(userId, true) }
    }

    private fun mockInstant(instant: Long = 1000L) {
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns instant }
    }
}
