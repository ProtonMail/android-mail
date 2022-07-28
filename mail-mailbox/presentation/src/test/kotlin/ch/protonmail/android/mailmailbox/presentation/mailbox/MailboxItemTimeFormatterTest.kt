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
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItemTimeFormatter.FormattedTime
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
    fun `when the message is from the current day and locale is Italian show time of the message in 24 hours format`() {
        givenCurrentLocaleIs(Locale.ITALIAN)
        givenCurrentTimeIs(1658853752.seconds) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658853643L // Tue Jul 26 18:40:44 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("18:40"), actual)
    }

    @Test
    fun `when the message is from the current day and locale is English show time of the message in 12 hours format`() {
        givenCurrentLocaleIs(Locale.ENGLISH)
        givenCurrentTimeIs(1658853752.seconds) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658853643L // Tue Jul 26 18:40:44 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("6:40 PM"), actual)
    }

    @Test
    fun `when the message is from yesterday show localized 'yesterday' string`() {
        givenCurrentTimeIs(1658853752.seconds) // Tue Jul 26 18:42:35 CEST 2022
        val itemTime = 1658772437 // Mon Jul 25 20:07:17 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localizable>(actual, actual.toString())
        assertEquals(FormattedTime.Localizable(R.string.yesterday), actual)
    }

    @Test
    fun `when the message is from the current week and older than yesterday show week day`() {
        givenCurrentLocaleIs(Locale.ENGLISH)
        givenCurrentTimeIs(1658994137.seconds) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1658772437 // Mon Jul 25 20:07:17 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("Monday"), actual)
    }

    @Test
    fun `when the message is from the current year and older than current week show day and month`() {
        givenCurrentLocaleIs(Locale.FRENCH)
        givenCurrentTimeIs(1658994137.seconds) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1647852004 // Mon Mar 21 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("21 mars"), actual)
    }

    @Test
    fun `when showing day and month ensure they are formatted based on the current locale`() {
        givenCurrentLocaleIs(Locale.US)
        givenCurrentTimeIs(1658994137.seconds) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1647852004 // Mon Mar 21 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("Mar 21"), actual)
    }

    @Test
    fun `when the message is from before the current year show the day month and year`() {
        givenCurrentLocaleIs(Locale.UK)
        givenCurrentTimeIs(1658994137.seconds) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1631518804 // Mon Sep 13 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("13 Sep 2021"), actual)
    }

    @Test
    fun `when showing day month and year ensure they are formatted based on current locale`() {
        givenCurrentLocaleIs(Locale.US)
        givenCurrentTimeIs(1658994137.seconds) // Thu Jul 28 09:42:17 CEST 2022
        val itemTime = 1631518804 // Mon Sep 13 09:40:04 CEST 2022

        val actual = formatter.invoke(itemTime.seconds)

        assertIs<FormattedTime.Localized>(actual, actual.toString())
        assertEquals(FormattedTime.Localized("Sep 13, 2021"), actual)
    }

    private fun givenCurrentLocaleIs(locale: Locale) {
        every { getDefaultLocale() } returns locale
    }

    private fun givenCurrentTimeIs(currentTime: Duration) {
        calendar.time = Date(currentTime.inWholeMilliseconds)
    }
}
