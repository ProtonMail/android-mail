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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import java.util.Calendar
import java.util.Date
import java.util.Locale
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultLocale
import ch.protonmail.android.mailmailbox.presentation.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MailboxItemTimeFormatterTest {

    private val calendar = Calendar.getInstance()

    private val getDefaultLocale = mockk<GetDefaultLocale> {
        every { this@mockk.invoke() } returns Locale.CANADA
    }

    private val formatter = MailboxItemTimeFormatter(
        calendar,
        getDefaultLocale
    )

    @Test
    fun `when the message is from the current day show time of the message`() {
        givenCurrentTimeIs(1658853752.seconds) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658853643L // Tue Jul 26 18:40:44 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<MailboxItemTimeFormatter.FormattedTime.Date>(actual, actual.toString())
        assertEquals(MailboxItemTimeFormatter.FormattedTime.Date("18:40"), actual)
    }

    @Test
    fun `when the message is from the day before today show yesterday`() {
        givenCurrentTimeIs(1658853752.seconds) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658772437 // 2022-07-25 20:07:17

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<MailboxItemTimeFormatter.FormattedTime.Localizable>(actual, actual.toString())
        assertEquals(MailboxItemTimeFormatter.FormattedTime.Localizable(R.string.yesterday), actual)
    }

    private fun givenCurrentTimeIs(currentTime: Duration) {
        calendar.time = Date(currentTime.inWholeMilliseconds)
    }
}
