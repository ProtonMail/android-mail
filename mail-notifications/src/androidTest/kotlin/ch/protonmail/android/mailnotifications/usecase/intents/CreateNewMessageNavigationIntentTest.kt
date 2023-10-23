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

package ch.protonmail.android.mailnotifications.usecase.intents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.usecase.intents.CreateNewMessageNavigationIntent
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@SmokeTest
internal class CreateNewMessageNavigationIntentTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    private val notificationsDeepLinkHelper = mockk<NotificationsDeepLinkHelper>()
    private val createNewMessageNavigationIntent =
        CreateNewMessageNavigationIntent(context, notificationsDeepLinkHelper)

    @Before
    fun setup() {
        initializeCommonMocks()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun newMessageNavigationActionIsCreatedWithTheCorrectParameters() {
        // Given
        val notificationId = 123
        val userId = "userId"
        val messageId = "messageId"

        // When
        createNewMessageNavigationIntent(notificationId, messageId, userId)

        // Then
        verify(exactly = 1) {
            notificationsDeepLinkHelper.buildMessageDeepLinkIntent(notificationId.toString(), messageId, userId)
        }

        confirmVerified(notificationsDeepLinkHelper)
    }

    @Test
    fun newMessageGroupNavigationActionIsCreatedWithTheCorrectParameters() {
        // Given
        val notificationId = 123
        val userId = "userId"

        // When
        createNewMessageNavigationIntent(notificationId, userId)

        // Then
        verify(exactly = 1) {
            notificationsDeepLinkHelper.buildMessageGroupDeepLinkIntent(notificationId.toString(), userId)
        }

        confirmVerified(notificationsDeepLinkHelper)
    }

    private fun initializeCommonMocks() {
        val mockedIntent = Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, this::class.java)
        every { notificationsDeepLinkHelper.buildMessageDeepLinkIntent(any(), any(), any()) } returns mockedIntent
        every { notificationsDeepLinkHelper.buildMessageGroupDeepLinkIntent(any(), any()) } returns mockedIntent
    }
}
