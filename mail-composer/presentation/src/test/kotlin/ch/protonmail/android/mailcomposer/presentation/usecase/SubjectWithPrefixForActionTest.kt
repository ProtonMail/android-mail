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

package ch.protonmail.android.mailcomposer.presentation.usecase

import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubjectWithPrefixForActionTest {

    private val subjectWithPrefixForAction = SubjectWithPrefixForAction()

    @Test
    fun `should return subject as it is for Compose action`() = runTest {
        val action = DraftAction.Compose
        val subject = "Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should return subject as it is for PrefillForShare action`() = runTest {
        val action = DraftAction.PrefillForShare(IntentShareInfo.Empty)
        val subject = "Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should return subject as it is for ComposeToAddresses action`() = runTest {
        val action = DraftAction.ComposeToAddresses(emptyList())
        val subject = "Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should add forward prefix to subject for Forward action if not already present`() = runTest {
        val action = DraftAction.Forward(MessageIdSample.AugWeatherForecast)
        val subject = "Subject Line"
        val expectedSubject = "Fw: $subject"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(expectedSubject, result)
    }

    @Test
    fun `should not add forward prefix to subject for Forward action if already present`() = runTest {
        val action = DraftAction.Forward(MessageIdSample.AugWeatherForecast)
        val subject = "Fw: Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should add reply prefix to subject for Reply action if not already present`() = runTest {
        val action = DraftAction.Reply(MessageIdSample.AugWeatherForecast)
        val subject = "Subject Line"
        val expectedSubject = "Re: $subject"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(expectedSubject, result)
    }

    @Test
    fun `should not add reply prefix to subject for Reply action if already present`() = runTest {
        val action = DraftAction.Reply(MessageIdSample.AugWeatherForecast)
        val subject = " Re: Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should add reply prefix to subject for ReplyAll action if not already present`() = runTest {
        val action = DraftAction.ReplyAll(MessageIdSample.AugWeatherForecast)
        val subject = "Subject Line"
        val expectedSubject = "Re: $subject"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(expectedSubject, result)
    }

    @Test
    fun `should not add reply prefix to subject for ReplyAll action if reply prefix already present`() = runTest {
        val action = DraftAction.ReplyAll(MessageIdSample.AugWeatherForecast)
        val subject = "Re: Subject Line"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(subject, result)
    }

    @Test
    fun `should add reply prefix when replying a subject with forward prefix`() = runTest {
        val action = DraftAction.Reply(MessageIdSample.AugWeatherForecast)
        val subject = " Fw: Subject"
        val expectedSubject = "Re: $subject"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(expectedSubject, result)
    }

    @Test
    fun `should add forward prefix when forwarding a subject with reply prefix`() = runTest {
        val action = DraftAction.Forward(MessageIdSample.AugWeatherForecast)
        val subject = "Re: Subject"
        val expectedSubject = "Fw: $subject"

        val result = subjectWithPrefixForAction(action, subject)

        assertEquals(expectedSubject, result)
    }
}
