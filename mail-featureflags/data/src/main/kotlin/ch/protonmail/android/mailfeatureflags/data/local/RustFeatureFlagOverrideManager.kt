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
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagOverrideManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import uniffi.mail_uniffi.DebugFeatureFlagOverride
import uniffi.mail_uniffi.DebugFeatureFlagOverrideEntry
import uniffi.mail_uniffi.MailSessionClearDebugFeatureFlagOverrideResult
import uniffi.mail_uniffi.MailSessionListDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailSessionSetDebugFeatureFlagOverrideResult
import uniffi.mail_uniffi.MailUserSessionClearDebugFeatureFlagOverrideResult
import uniffi.mail_uniffi.MailUserSessionListDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailUserSessionSetDebugFeatureFlagOverrideResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets, clears and lists device-local debug overrides on the Rust SDK. Overrides never touch the
 * backend and no sync resets them. Session selection lives in [RustFeatureFlagSessionResolver].
 */
@Singleton
class RustFeatureFlagOverrideManager @Inject constructor(
    private val sessionResolver: RustFeatureFlagSessionResolver,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : FeatureFlagOverrideManager {

    override suspend fun overriddenFlags(): Map<String, Boolean?> = withContext(ioDispatcher) {
        listOverrideEntries().associate { it.flagName to it.debugOverride.enabled }
    }

    override suspend fun setOverride(key: String, enabled: Boolean) = withContext(ioDispatcher) {
        val override = DebugFeatureFlagOverride(enabled = enabled, variant = null)
        when (val session = sessionResolver.activeSession()) {
            is ActiveSession.User ->
                when (val result = session.session.setDebugFeatureFlagOverride(key, override)) {
                    is MailUserSessionSetDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailUserSessionSetDebugFeatureFlagOverrideResult.Error -> logError("set", key, result.v1)
                }

            is ActiveSession.App ->
                when (val result = session.session.setDebugFeatureFlagOverride(key, override)) {
                    is MailSessionSetDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailSessionSetDebugFeatureFlagOverrideResult.Error -> logError("set", key, result.v1)
                }
        }
    }

    override suspend fun clearOverride(key: String) = withContext(ioDispatcher) {
        when (val session = sessionResolver.activeSession()) {
            is ActiveSession.User ->
                when (val result = session.session.clearDebugFeatureFlagOverride(key)) {
                    is MailUserSessionClearDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailUserSessionClearDebugFeatureFlagOverrideResult.Error -> logError("clear", key, result.v1)
                }

            is ActiveSession.App ->
                when (val result = session.session.clearDebugFeatureFlagOverride(key)) {
                    is MailSessionClearDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailSessionClearDebugFeatureFlagOverrideResult.Error -> logError("clear", key, result.v1)
                }
        }
    }

    /**
     * Rust exposes no "clear all", so we clear every currently overridden flag one by one.
     */
    override suspend fun clearAllOverrides() {
        overriddenFlags().keys.forEach { clearOverride(it) }
    }

    private suspend fun listOverrideEntries(): List<DebugFeatureFlagOverrideEntry> {
        return when (val session = sessionResolver.activeSession()) {
            is ActiveSession.User -> when (val result = session.session.listDebugFeatureFlagOverrides()) {
                is MailUserSessionListDebugFeatureFlagOverridesResult.Ok -> result.v1
                is MailUserSessionListDebugFeatureFlagOverridesResult.Error -> emptyList()
            }

            is ActiveSession.App -> when (val result = session.session.listDebugFeatureFlagOverrides()) {
                is MailSessionListDebugFeatureFlagOverridesResult.Ok -> result.v1
                is MailSessionListDebugFeatureFlagOverridesResult.Error -> emptyList()
            }
        }
    }

    private fun logError(
        action: String,
        key: String,
        error: Any?
    ) {
        Timber.e("Rust FF override: unable to $action debug override for '$key': $error")
    }
}
