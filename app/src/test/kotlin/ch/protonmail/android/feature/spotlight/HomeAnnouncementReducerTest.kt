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

package ch.protonmail.android.feature.spotlight

import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class HomeAnnouncementReducerTest {

    private val reducer = HomeAnnouncementReducer()
    private val userId = UserId("user")
    private val spotlight = Announcement.ByoeSpotlight(ByoeTreatment.Treatment1)

    @Test
    fun `given Home is not ready, when an ask is due, then it is not asked yet`() {
        // When
        val state = reducer.newStateFrom(HomeAnnouncementState.Initial, HomeAnnouncementEvent.AskDue(userId))

        // Then
        assertFalse(state.shouldAsk)
    }

    @Test
    fun `given an ask is due, when Home becomes ready, then it is asked`() {
        // Given
        val due = reducer.newStateFrom(HomeAnnouncementState.Initial, HomeAnnouncementEvent.AskDue(userId))

        // When
        val state = reducer.newStateFrom(due, HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // Then
        assertTrue(state.shouldAsk)
    }

    @Test
    fun `given Home is ready, when an announcement is loaded, then Home navigates to it`() {
        // Given
        val asking = askingState(isHomeReady = true)

        // When
        val state = reducer.newStateFrom(asking, HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight))

        // Then
        assertEquals(Effect.of<Announcement>(spotlight), state.navigateTo)
        assertEquals(spotlight, state.pending)
    }

    @Test
    fun `given no announcement, when it is loaded, then nothing happens`() {
        // When
        val state = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, null)
        )

        // Then
        assertEquals(Effect.empty(), state.navigateTo)
        assertNull(state.pending)
        assertFalse(state.isAsking)
    }

    @Test
    fun `given an announcement popped before it appeared, when Home is ready again, then the SDK is asked again`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )
        val left = reducer.newStateFrom(loaded, HomeAnnouncementAction.HomeReadinessChanged(isReady = false))
        left.navigateTo.consume()

        // When
        val state = reducer.newStateFrom(left, HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // Then
        assertTrue(state.shouldAsk)
        assertNull(state.pending)
        assertEquals(Effect.empty(), state.navigateTo)
    }

    @Test
    fun `given Home moved on during the ask, when the answer arrives, then it is not kept and is asked for again`() {
        // Given
        val asking = askingState(isHomeReady = false)

        // When
        val loaded = reducer.newStateFrom(asking, HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight))
        val state = reducer.newStateFrom(loaded, HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // Then
        assertNull(loaded.pending)
        assertEquals(Effect.empty(), loaded.navigateTo)
        assertTrue(state.shouldAsk)
    }

    @Test
    fun `given the announcements are held, when an ask is due, then it waits for the hold to end`() {
        // Given
        val ready = reducer.newStateFrom(
            HomeAnnouncementState.Initial,
            HomeAnnouncementAction.HomeReadinessChanged(isReady = true)
        )
        val held = reducer.newStateFrom(ready, HomeAnnouncementEvent.HoldChanged(isHeld = true))

        // When
        val due = reducer.newStateFrom(held, HomeAnnouncementEvent.AskDue(userId))
        val released = reducer.newStateFrom(due, HomeAnnouncementEvent.HoldChanged(isHeld = false))

        // Then
        assertFalse(due.shouldAsk)
        assertTrue(released.shouldAsk)
    }

    @Test
    fun `given a hold started during the ask, when the answer arrives, then Home does not navigate to it`() {
        // Given
        val held = reducer.newStateFrom(askingState(isHomeReady = true), HomeAnnouncementEvent.HoldChanged(true))

        // When
        val state = reducer.newStateFrom(held, HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight))

        // Then
        assertEquals(Effect.empty(), state.navigateTo)
        assertNull(state.pending)
        assertTrue(state.isAskDue)
    }

    @Test
    fun `given a pending announcement, when it appears, then it is no longer pending`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )

        // When
        val state = reducer.newStateFrom(loaded, HomeAnnouncementAction.Shown(spotlight))

        // Then
        assertNull(state.pending)
    }

    @Test
    fun `given a pending announcement, when another account becomes primary, then it is dropped`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )

        // When
        val state = reducer.newStateFrom(loaded, HomeAnnouncementEvent.AskDue(UserId("other")))

        // Then
        assertNull(state.pending)
        assertEquals(UserId("other"), state.userId)
    }

    @Test
    fun `given a pending announcement that never appeared, when the same account comes back, then it is asked again`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )

        // When
        val state = reducer.newStateFrom(loaded, HomeAnnouncementEvent.AskDue(userId))

        // Then
        assertNull(state.pending)
        assertTrue(state.shouldAsk)
    }

    @Test
    fun `given a pending announcement on screen, when the same account comes back, then it stays pending`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )
        val onScreen = reducer.newStateFrom(loaded, HomeAnnouncementAction.HomeReadinessChanged(isReady = false))

        // When
        val state = reducer.newStateFrom(onScreen, HomeAnnouncementEvent.AskDue(userId))

        // Then
        assertEquals(spotlight, state.pending)
    }

    @Test
    fun `given the account changed during the ask, when the old answer arrives, then it is ignored`() {
        // Given
        val switched = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AskDue(UserId("other"))
        )

        // When
        val state = reducer.newStateFrom(switched, HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight))

        // Then
        assertNull(state.pending)
    }

    @Test
    fun `given a pending announcement, when it appears, then it is shown for the account it was asked for`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )

        // When
        val state = reducer.newStateFrom(loaded, HomeAnnouncementAction.Shown(spotlight))

        // Then
        assertEquals(ShownAnnouncement(userId, spotlight), state.shown)
    }

    @Test
    fun `given an announcement already shown, when it appears again, then it is not a new showing`() {
        // Given
        val loaded = reducer.newStateFrom(
            askingState(isHomeReady = true),
            HomeAnnouncementEvent.AnnouncementLoaded(userId, spotlight)
        )
        val shown = reducer.newStateFrom(loaded, HomeAnnouncementAction.Shown(spotlight))

        // When
        val state = reducer.newStateFrom(shown, HomeAnnouncementAction.Shown(spotlight))

        // Then
        assertEquals(shown, state)
    }

    private fun askingState(isHomeReady: Boolean) = HomeAnnouncementState.Initial.copy(
        userId = userId,
        isHomeReady = isHomeReady,
        isAsking = true
    )
}
