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

package ch.protonmail.android.maillabel.data.local

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.data.repository.InMemorySelectedMailLabelIdRepositoryImpl
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemorySelectedMailLabelIdRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appScope = CoroutineScope(mainDispatcherRule.testDispatcher)
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val findLocalSystemLabelId = mockk<FindLocalSystemLabelId> {
        coEvery { this@mockk.invoke(any(), SystemLabelId.Inbox) } returns MailLabelId.System(LabelId("0"))
    }

    private val initialSystemLabel = MailLabelId.System(SystemLabelId.Inbox.labelId)

    private val repository by lazy {
        InMemorySelectedMailLabelIdRepositoryImpl(
            appScope = appScope,
            findLocalSystemLabelId = findLocalSystemLabelId,
            observePrimaryUserId = observePrimaryUserId
        )
    }

    @Test
    fun `initial observeLoadedMailLabelId does not emit if not set as loaded`() = runTest {

        // When
        repository.observeLoadedMailLabelId().test {
            // Then
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial observeSelectedMailLabelId emits inbox label`() = runTest {

        // When
        repository.observeSelectedMailLabelId().test {
            // Then
            assertEquals(initialSystemLabel, awaitItem())
        }
    }

    @Test
    fun `observeLoadedMailLabelId emits when label is set as loaded`() = runTest {

        // When
        repository.observeLoadedMailLabelId().test {
            repository.setLocationAsLoaded(MailLabelIdWithCategory(MailLabelTestData.draftsSystemLabel.id))

            // Then
            assertEquals(MailLabelTestData.draftsSystemLabel.id, awaitItem())
        }
    }

    @Test
    fun `observeSelectedMailLabelId emits only when label is selected`() = runTest {

        // When
        repository.observeSelectedMailLabelId().test {
            assertEquals(initialSystemLabel, awaitItem())

            repository.selectLocation(MailLabelTestData.draftsSystemLabel.id)

            // Then
            assertEquals(MailLabelTestData.draftsSystemLabel.id, awaitItem())
        }
    }

    @Test
    fun `observeSelectedMailLabelId does not emit duplicates`() = runTest {
        val label = MailLabelTestData.draftsSystemLabel.id

        // When
        repository.observeSelectedMailLabelId().test {
            assertEquals(initialSystemLabel, awaitItem())

            repository.selectLocation(label)
            assertEquals(label, awaitItem())

            repository.selectLocation(label)

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `user change resets to inbox label`() = runTest {
        // Given
        val userIdFlow = MutableStateFlow<UserId?>(null)
        every { observePrimaryUserId() } returns userIdFlow

        val newLabel = MailLabelTestData.archiveSystemLabel.id

        // When
        repository.observeSelectedMailLabelId().test {

            repository.selectLocation(newLabel)
            assertEquals(newLabel, awaitItem())

            userIdFlow.emit(UserIdSample.Primary)
            advanceUntilIdle()

            // Then
            assertEquals(initialSystemLabel, awaitItem())
        }
    }

    @Test
    fun `observeSelectedMailLabelId emits only requested labels`() = runTest {

        // When
        repository.observeSelectedMailLabelId().test {
            assertEquals(initialSystemLabel, awaitItem())

            repository.selectLocation(MailLabelTestData.draftsSystemLabel.id)
            assertEquals(MailLabelTestData.draftsSystemLabel.id, awaitItem())

            repository.setLocationAsLoaded(MailLabelIdWithCategory(MailLabelTestData.archiveSystemLabel.id))

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `resetSelectedCategory sets selected category to null when default is selected`() = runTest {
        // Given
        val category = CategoryLabelId("20")

        repository.observeSelectedLabelWithCategory().test {
            assertEquals(MailLabelIdWithCategory(initialSystemLabel), awaitItem())

            repository.selectCategory(category)
            assertEquals(MailLabelIdWithCategory(initialSystemLabel, category), awaitItem())

            // When
            repository.resetSelectedCategory()

            // Then
            assertEquals(MailLabelIdWithCategory(initialSystemLabel), awaitItem())
        }
    }

    @Test
    fun `resetSelectedCategory does nothing when selected category is already null`() = runTest {
        // Given
        repository.observeSelectedLabelWithCategory().test {
            assertEquals(MailLabelIdWithCategory(initialSystemLabel), awaitItem())

            // When
            repository.resetSelectedCategory()

            // Then
            expectNoEvents()
        }
    }
}
