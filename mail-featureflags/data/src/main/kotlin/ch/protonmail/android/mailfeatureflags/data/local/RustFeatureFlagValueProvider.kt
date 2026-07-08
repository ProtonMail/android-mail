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

import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagProviderPriority
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagValueProvider
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uniffi.mail_uniffi.MailSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.MailUserSessionIsFeatureEnabledResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads feature-flag values from the Rust SDK, including any device-local debug override, which
 * Rust applies internally. Session selection lives in [RustFeatureFlagSessionResolver].
 */
@Singleton
class RustFeatureFlagValueProvider @Inject constructor(
    // needs to be lazy because of initialization steps
    private val sessionResolver: Lazy<RustFeatureFlagSessionResolver>,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : FeatureFlagValueProvider {

    override val priority: Int = FeatureFlagProviderPriority.RustProvider

    override val name: String = "Rust FF provider"

    override suspend fun getFeatureFlagValue(key: String): Boolean? = withContext(ioDispatcher) {
        when (val session = sessionResolver.get().activeSession()) {
            is ActiveSession.User -> when (val result = session.session.isFeatureEnabled(featureId = key)) {
                is MailUserSessionIsFeatureEnabledResult.Ok -> result.v1
                is MailUserSessionIsFeatureEnabledResult.Error -> null
            }

            is ActiveSession.App -> when (val result = session.session.isFeatureEnabled(featureId = key)) {
                is MailSessionIsFeatureEnabledResult.Ok -> result.v1
                is MailSessionIsFeatureEnabledResult.Error -> null
            }
        }
    }
}
