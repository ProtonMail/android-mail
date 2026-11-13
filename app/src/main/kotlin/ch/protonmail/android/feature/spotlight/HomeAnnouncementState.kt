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

import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import me.proton.core.domain.entity.UserId

/**
 * @param isHomeReady Home's own prompts are done and the mailbox is on screen, with no message open.
 * @param isAskDue the app came to the foreground or the account changed, and the SDK was not asked since.
 * @param isHeld something that decides the announcements is being worked out, e.g. a BYOE deep link.
 * @param pending the announcement Home was sent to, until its screen appears. If the mailbox is back before it
 * appeared, e.g. a notification popped it off the back stack, the SDK is asked again (ET-6768). The old answer is
 * not reused: it may no longer be due.
 * @param shown the announcement on screen, with the account it was asked for. What the user does with it is recorded
 * for that account, even if the primary account changed since.
 */
data class HomeAnnouncementState(
    val userId: UserId?,
    val isHomeReady: Boolean,
    val isAskDue: Boolean,
    val isAsking: Boolean,
    val isHeld: Boolean,
    val pending: Announcement?,
    val shown: ShownAnnouncement?,
    val navigateTo: Effect<Announcement>
) {

    val shouldAsk: Boolean
        get() = userId != null && isHomeReady && !isHeld && isAskDue && !isAsking && pending == null

    companion object {

        val Initial = HomeAnnouncementState(
            userId = null,
            isHomeReady = false,
            isAskDue = false,
            isAsking = false,
            isHeld = false,
            pending = null,
            shown = null,
            navigateTo = Effect.empty()
        )
    }
}

data class ShownAnnouncement(val userId: UserId, val announcement: Announcement)

sealed interface HomeAnnouncementOperation

sealed interface HomeAnnouncementAction : HomeAnnouncementOperation {

    data class HomeReadinessChanged(val isReady: Boolean) : HomeAnnouncementAction

    /** The announcement's screen appeared: its first composition, before any tap. */
    data class Shown(val announcement: Announcement) : HomeAnnouncementAction

    /** The user closed the content search What's New or picked one of its buttons. */
    data object ContentSearchAnswered : HomeAnnouncementAction

    /** The user tapped Connect on the BYOE spotlight. */
    data class ByoeSpotlightConnected(val announcement: Announcement.ByoeSpotlight) : HomeAnnouncementAction

    /** The user dismissed the BYOE spotlight with Later or back. */
    data class ByoeSpotlightDismissed(
        val announcement: Announcement.ByoeSpotlight,
        val method: ByoeDismissMethod
    ) : HomeAnnouncementAction

    /** The user declined the BYOE onboarding page. It is not an announcement. */
    data class ByoeOnboardingPageDismissed(val method: ByoeDismissMethod) : HomeAnnouncementAction
}

sealed interface HomeAnnouncementEvent : HomeAnnouncementOperation {

    /** The app came to the foreground, or [userId] became the primary account. */
    data class AskDue(val userId: UserId) : HomeAnnouncementEvent

    data object AskStarted : HomeAnnouncementEvent

    data class HoldChanged(val isHeld: Boolean) : HomeAnnouncementEvent

    data class AnnouncementLoaded(val userId: UserId, val announcement: Announcement?) : HomeAnnouncementEvent
}
