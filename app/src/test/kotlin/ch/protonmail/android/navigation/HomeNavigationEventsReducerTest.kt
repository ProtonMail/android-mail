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
package ch.protonmail.android.navigation

import java.net.URLEncoder
import android.content.Intent
import android.net.Uri
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.encode
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeNavigationEvent
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.model.NavigationEffect
import ch.protonmail.android.navigation.reducer.HomeNavigationEventsReducer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class HomeNavigationEventsReducerTest {

    private val reducer = HomeNavigationEventsReducer()

    private val sampleShareInfo = IntentShareInfo(
        attachmentUris = listOf(
            "content://media/external/images/media/12345",
            "content://media/external/images/media/12346"
        ),
        emailSubject = "Holiday Photos",
        emailRecipientTo = listOf("friend@example.com"),
        emailRecipientCc = listOf("ccperson@example.com"),
        emailRecipientBcc = listOf("secret@example.com"),
        emailBody = "Hi,\n\nHere are the photos from our trip.\n\nCheers,\nSerdar",
        encoded = false,
        isExternal = true
    )


    @BeforeTest
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `reduce LauncherIntentReceived when not started from launcher sets flag to true`() {
        // Given
        val initialState = HomeState.Initial.copy(startedFromLauncher = false)
        val event = HomeNavigationEvent.LauncherIntentReceived(
            intent = Intent(Intent.ACTION_MAIN)
        )

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        assertEquals(false, initialState.startedFromLauncher)
        assertEquals(true, result.startedFromLauncher)
    }

    @Test
    fun `reduce LauncherIntentReceived when already started from launcher is no-op`() {
        // Given
        val initialState = HomeState.Initial.copy(startedFromLauncher = true)
        val event = HomeNavigationEvent.LauncherIntentReceived(
            intent = Intent(Intent.ACTION_MAIN)
        )

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        assertSame(initialState, result)
    }

    @Test
    fun `navigate to composer when external share intent is received`() {
        // Given
        val initialState = HomeState.Initial

        val event = HomeNavigationEvent.ExternalShareIntentReceived(
            intent = Intent(Intent.ACTION_SEND),
            shareInfo = sampleShareInfo
        )

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        val expectedDraftAction = DraftAction.PrefillForShare(sampleShareInfo.encode())
        val expectedDestination = Destination.Screen.ShareFileComposer(
            draftAction = expectedDraftAction,
            isExternal = true
        )
        val expectedNavigation = NavigationEffect.NavigateTo(expectedDestination)
        val expectedEffect = Effect.of(expectedNavigation)

        assertEquals(expectedEffect, result.navigateToEffect)
    }

    @Test
    fun `navigate to composer when internal share intent is received`() {
        // Given
        val initialState = HomeState.Initial

        val event = HomeNavigationEvent.InternalShareIntentReceived(
            intent = Intent(Intent.ACTION_SEND),
            shareInfo = sampleShareInfo
        )

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        val expectedDraftAction = DraftAction.PrefillForShare(sampleShareInfo.encode())
        val expectedDestination = Destination.Screen.ShareFileComposer(
            draftAction = expectedDraftAction,
            isExternal = false
        )
        val expectedNavigation = NavigationEffect.NavigateTo(expectedDestination)
        val expectedEffect = Effect.of(expectedNavigation)

        assertEquals(expectedEffect, result.navigateToEffect)
    }

    @Test
    fun `navigate to composer when mailto intent is received`() {
        // Given
        val initialState = HomeState.Initial
        val mailToUri = "mailto:proton@protonmail.com"
        val mailToIntent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { dataString } returns mailToUri
        }
        val event = HomeNavigationEvent.MailToIntentReceived(
            intent = mailToIntent
        )

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        val expectedDraftAction = DraftAction.MailTo(URLEncoder.encode(mailToUri, Charsets.UTF_8.name()))
        val expectedDestination = Destination.Screen.MessageActionComposer(action = expectedDraftAction)
        val expectedNavigation = NavigationEffect.NavigateTo(expectedDestination)
        val expectedEffect = Effect.of(expectedNavigation)

        assertEquals(expectedEffect, result.navigateToEffect)
    }

    @Test
    fun `state is unchanged when unknown intent is received`() {
        // Given
        val initialState = HomeState.Initial
        val intent = Intent("some.weird.ACTION")
        val event = HomeNavigationEvent.UnknownIntentReceived(intent = intent)

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        assertSame(initialState, result)
    }

    @Test
    fun `state is unchanged when invalid share intent is received`() {
        // Given
        val initialState = HomeState.Initial
        val intent = Intent(Intent.ACTION_SEND)
        val event = HomeNavigationEvent.InvalidShareIntentReceived(intent = intent)

        // When
        val result = reducer.reduce(initialState, event)

        // Then
        assertSame(initialState, result)
    }
}
