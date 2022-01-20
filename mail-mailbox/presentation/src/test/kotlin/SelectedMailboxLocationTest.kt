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

import app.cash.turbine.test
import ch.protonmail.android.mailmailbox.presentation.SelectedMailboxLocation
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import ch.protonmail.android.mailmessage.domain.model.MailLocation.AllMail
import ch.protonmail.android.mailmessage.domain.model.MailLocation.Archive
import ch.protonmail.android.mailmessage.domain.model.MailLocation.Drafts
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SelectedMailboxLocationTest {

    private lateinit var selectedMailboxLocation: SelectedMailboxLocation

    @Before
    fun setUp() {
        selectedMailboxLocation = SelectedMailboxLocation()
    }

    @Test
    fun initialSelectedLocationIsInboxByDefault() = runTest {
        selectedMailboxLocation.location.test {
            assertEquals(MailLocation.Inbox, awaitItem())
        }
    }

    @Test
    fun emitsNewlySelectedLocationWhenItChanges() = runTest {
        selectedMailboxLocation.location.test {
            assertEquals(MailLocation.Inbox, awaitItem())

            selectedMailboxLocation.set(Drafts)

            assertEquals(Drafts, awaitItem())
        }
    }

    @Test
    fun doesNotEmitSameLocationTwice() = runTest {
        selectedMailboxLocation.location.test {
            assertEquals(MailLocation.Inbox, awaitItem())

            selectedMailboxLocation.set(Archive)
            selectedMailboxLocation.set(Archive)
            selectedMailboxLocation.set(AllMail)

            assertEquals(Archive, awaitItem())
            assertEquals(AllMail, awaitItem())
        }
    }
}
