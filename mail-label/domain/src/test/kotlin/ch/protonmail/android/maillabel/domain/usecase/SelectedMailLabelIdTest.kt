/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maillabel.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SelectedMailLabelIdTest {

    private lateinit var selectedMailLabelId: SelectedMailLabelId

    @Before
    fun setUp() {
        selectedMailLabelId = SelectedMailLabelId()
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
}
