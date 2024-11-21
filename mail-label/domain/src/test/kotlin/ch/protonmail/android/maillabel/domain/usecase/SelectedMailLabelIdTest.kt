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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class SelectedMailLabelIdTest {

    private val appScope = TestScope()
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns emptyFlow()
    }
    private val selectedMailLabelId by lazy {
        SelectedMailLabelId(
            appScope = appScope,
            observePrimaryUserId = observePrimaryUserId
        )
    }

    @Test
    fun `initial selected mailLabelId is inbox by default`() = runTest {
        selectedMailLabelId.flow.test {
            assertEquals(MailLabelId.System.Inbox, awaitItem())
        }
    }

    @Test
    fun `emits newly selected mailLabelId when it changes`() = runTest {
        selectedMailLabelId.flow.test {
            assertEquals(MailLabelId.System.Inbox, awaitItem())

            selectedMailLabelId.set(MailLabelId.System.Drafts)

            assertEquals(MailLabelId.System.Drafts, awaitItem())
        }
    }

    @Test
    fun `does not emit same mailLabelId twice`() = runTest {
        selectedMailLabelId.flow.test {
            assertEquals(MailLabelId.System.Inbox, awaitItem())

            selectedMailLabelId.set(MailLabelId.System.Archive)
            selectedMailLabelId.set(MailLabelId.System.Archive)
            selectedMailLabelId.set(MailLabelId.Custom.Label(LabelId("lId1")))

            assertEquals(MailLabelId.System.Archive, awaitItem())
            assertEquals(MailLabelId.Custom.Label(LabelId("lId1")), awaitItem())
        }
    }

    @Test
    fun `emits inbox when primary user changes`() = runTest {
        // given
        val userFlow = MutableStateFlow<UserId?>(null)
        every { observePrimaryUserId() } returns userFlow

        selectedMailLabelId.flow.test {
            assertEquals(MailLabelId.System.Inbox, awaitItem())
            selectedMailLabelId.set(MailLabelId.System.Archive)
            assertEquals(MailLabelId.System.Archive, awaitItem())

            // when
            userFlow.emit(UserSample.Primary.userId)
            appScope.advanceUntilIdle()

            // then
            assertEquals(MailLabelId.System.Inbox, awaitItem())
        }
    }
}
