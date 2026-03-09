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

package ch.protonmail.android.mailfeatureflags.data.local

import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagProviderPriority
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagValueProvider
import ch.protonmail.android.mailfeatureflags.domain.annotation.FeatureFlagsCoroutineScope
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.MailSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.MailUserSessionIsFeatureEnabledResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnleashFeatureFlagValueProvider @Inject constructor(
    private val sessionFacade: Lazy<SessionFacade>,
    @FeatureFlagsCoroutineScope private val coroutineScope: CoroutineScope
) : FeatureFlagValueProvider {

    override val priority: Int = FeatureFlagProviderPriority.UnleashProvider

    override val name: String = "Unleash FF provider"

    override suspend fun getFeatureFlagValue(key: String): Boolean? = withContext(coroutineScope.coroutineContext) {
        // needs to be lazy because of initialisation steps
        val session = sessionFacade.get()
        // For feature flags that are used in the app initialisation
        if (session.isMailSessionInitialised().not()) {
            Timber.w(
                "Getting FeatureFlag:: MailSession is not initialized yet. " +
                    " It's probably that either the user is not logged in or the app is initialising"
            )
            return@withContext getAppSessionFeatureFlag(key = key, sessionFacade = session)
        }

        val userId = session.getUserId()
        if (userId == null) {
            Timber.w(
                "Getting FeatureFlag:: No user session available"
            )
            return@withContext getAppSessionFeatureFlag(key = key, sessionFacade = session)
        }
        return@withContext getUserSessionFeatureFlag(key = key, userId = userId, session)
    }

    private suspend fun getUserSessionFeatureFlag(
        key: String,
        userId: UserId,
        sessionFacade: SessionFacade
    ): Boolean? {
        return when (val result = sessionFacade.getIsUserSessionFeatureEnabled(key = key, userId = userId)) {
            is MailUserSessionIsFeatureEnabledResult.Error -> null
            is MailUserSessionIsFeatureEnabledResult.Ok -> result.v1
            null -> null
        }
    }

    /**
     * MailSession::isFeatureEnabled won't refresh if there is an active user session!
     * Use MailUserSession::isFeatureEnabled instead. MailSession::isFeatureEnabled should be used only
     * before user logs in, for example on login screen.
     */
    private suspend fun getAppSessionFeatureFlag(sessionFacade: SessionFacade, key: String): Boolean? {
        return when (val result = sessionFacade.getIsMailSessionFeatureEnabled(key)) {
            is MailSessionIsFeatureEnabledResult.Error -> null
            is MailSessionIsFeatureEnabledResult.Ok -> result.v1
        }
    }
}
