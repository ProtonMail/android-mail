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

package ch.protonmail.android.mailbyoe.presentation.sidebar

import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.CloseByoeSidebarBanner
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeSidebarBannerTreatment
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ByoeSidebarBannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns flowOf(userId)
    }
    private val getByoeSidebarBannerTreatment = mockk<GetByoeSidebarBannerTreatment>()
    private val closeByoeSidebarBanner = mockk<CloseByoeSidebarBanner>(relaxUnitFun = true)

    private fun viewModel() = ByoeSidebarBannerViewModel(
        observePrimaryUserId,
        getByoeSidebarBannerTreatment,
        closeByoeSidebarBanner
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the banner should show, when the sidebar opens, then it is visible`() = runTest {
        // Given
        coEvery { getByoeSidebarBannerTreatment(userId) } returns ByoeTreatment.Treatment1
        val viewModel = viewModel()

        // When
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)

        // Then
        assertEquals(ByoeSidebarBannerState.Visible(ByoeTreatment.Treatment1), viewModel.state.value)
    }

    @Test
    fun `given the banner should not show, when the sidebar opens, then it stays hidden`() = runTest {
        // Given
        coEvery { getByoeSidebarBannerTreatment(userId) } returns null
        val viewModel = viewModel()

        // When
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)

        // Then
        assertEquals(ByoeSidebarBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun `given a visible banner, when it is closed, then it hides and the close is recorded`() = runTest {
        // Given
        coEvery { getByoeSidebarBannerTreatment(userId) } returns ByoeTreatment.Treatment1
        val viewModel = viewModel()
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)

        // When
        viewModel.submit(ByoeSidebarBannerAction.CloseClicked)

        // Then
        assertEquals(ByoeSidebarBannerState.Hidden, viewModel.state.value)
        coVerify(exactly = 1) { closeByoeSidebarBanner(userId, ByoeDismissMethod.Button) }
    }

    @Test
    fun `given the sidebar is still checking the banner, when it is closed, then it stays hidden`() = runTest {
        // Given
        val treatment = CompletableDeferred<ByoeTreatment?>()
        coEvery { getByoeSidebarBannerTreatment(userId) } coAnswers { treatment.await() }
        val viewModel = viewModel()
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)

        // When
        viewModel.submit(ByoeSidebarBannerAction.CloseClicked)
        treatment.complete(ByoeTreatment.Treatment1)

        // Then
        assertEquals(ByoeSidebarBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun `given the banner was closed, when the sidebar opens again, then it stays hidden`() = runTest {
        // Given
        coEvery { getByoeSidebarBannerTreatment(userId) } returns ByoeTreatment.Treatment1
        val viewModel = viewModel()
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)
        viewModel.submit(ByoeSidebarBannerAction.CloseClicked)

        // When
        viewModel.submit(ByoeSidebarBannerAction.SidebarOpened)

        // Then
        assertEquals(ByoeSidebarBannerState.Hidden, viewModel.state.value)
        coVerify(exactly = 1) { getByoeSidebarBannerTreatment(userId) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
