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

package ch.protonmail.android.mailspotlight.domain.usecase

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.GetGmailConnectionSettingsAccess
import ch.protonmail.android.mailsession.domain.usecase.RefreshFeatureFlags
import ch.protonmail.android.mailspotlight.domain.AnnouncementHold
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Whether the BYOE deep link opens the Gmail connection. It does not depend on the spotlight having been seen: opening
 * the link counts as seen. A control group user, or one behind the connection killswitch, stays on the mailbox.
 *
 * The link is recorded first, before anything slow: it needs no flags, and from then on the SDK no longer offers the
 * spotlight. On a cold start the flags may not be loaded yet, so the check waits for them, within the usual timeout.
 *
 * No announcement shows while this runs, so the connection never lands on top of one. That hold has to start before
 * anything suspends, so the account is taken as a flow and read inside it, once: with no primary account the link is
 * dropped, rather than opening the connection whenever an account appears.
 */
class ShouldOpenConnectGmailDeepLink @Inject constructor(
    private val refreshFeatureFlags: RefreshFeatureFlags,
    private val markByoeDeepLinkOpened: MarkByoeDeepLinkOpened,
    private val isFeatureEntryPointEnabled: IsFeatureEntryPointEnabled,
    private val getGmailConnectionSettingsAccess: GetGmailConnectionSettingsAccess,
    private val announcementHold: AnnouncementHold
) {

    suspend operator fun invoke(primaryUserId: Flow<UserId?>): Boolean = announcementHold.hold {
        val userId = primaryUserId.first()
        if (userId == null) {
            Timber.d("byoe: no primary account, ignoring the connect Gmail deep link")
            return@hold false
        }
        check(userId)
    }

    private suspend fun check(userId: UserId): Boolean {
        markByoeDeepLinkOpened(userId)
        refreshFeatureFlags(userId)

        val isSettingsEnabled = isFeatureEntryPointEnabled(userId, FeatureEntryPoint.Byoe(ByoeEntryPoint.Settings))
        val shouldOpen = isSettingsEnabled &&
            getGmailConnectionSettingsAccess(userId) == GmailConnectionSettingsAccess.Full
        if (!shouldOpen) Timber.d("byoe: BYOE is not available, ignoring the connect Gmail deep link")
        return shouldOpen
    }
}
