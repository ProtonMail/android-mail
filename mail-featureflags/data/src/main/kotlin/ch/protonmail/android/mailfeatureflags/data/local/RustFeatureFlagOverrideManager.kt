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
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagOverride
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import uniffi.mail_uniffi.DebugFeatureFlagOverride
import uniffi.mail_uniffi.DebugFeatureFlagOverrideEntry
import uniffi.mail_uniffi.FeatureFlagPayloadType
import uniffi.mail_uniffi.FeatureFlagVariant
import uniffi.mail_uniffi.FeatureFlagVariantPayload
import uniffi.mail_uniffi.MailSessionClearAllDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailSessionListDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailSessionSetDebugFeatureFlagOverrideResult
import uniffi.mail_uniffi.MailUserSessionClearAllDebugFeatureFlagOverridesResult
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

    override suspend fun overriddenDebugFlags(): Map<String, FeatureFlagOverride> = withContext(ioDispatcher) {
        listOverrideEntries().associate { entry ->
            entry.flagName to FeatureFlagOverride(
                enabled = entry.debugOverride.enabled,
                variant = entry.debugOverride.variant?.payload?.value
            )
        }
    }

    override suspend fun setDebugOverride(
        key: String,
        enabled: Boolean,
        variant: String?
    ) {
        val override = DebugFeatureFlagOverride(enabled = enabled, variant = variant?.toFeatureFlagVariant())
        withContext(ioDispatcher) {
            activeUserSession()?.let { userSession ->
                when (val result = userSession.setDebugFeatureFlagOverride(key, override)) {
                    is MailUserSessionSetDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailUserSessionSetDebugFeatureFlagOverrideResult.Error -> logError("set", key, result.v1)
                }
            }
            // Always mirrored onto the app session. Flag reads go to the app session when no user is
            // logged in, so an override set while logged in still applies after logout.
            appSession()?.let { appSession ->
                when (val result = appSession.setDebugFeatureFlagOverride(key, override)) {
                    is MailSessionSetDebugFeatureFlagOverrideResult.Ok -> Unit
                    is MailSessionSetDebugFeatureFlagOverrideResult.Error -> logError("set", key, result.v1)
                }
            }
        }
    }

    override suspend fun clearAllDebugOverrides() {
        withContext(ioDispatcher) {
            activeUserSession()?.let { userSession ->
                when (val result = userSession.clearAllDebugFeatureFlagOverrides()) {
                    is MailUserSessionClearAllDebugFeatureFlagOverridesResult.Ok -> Unit
                    is MailUserSessionClearAllDebugFeatureFlagOverridesResult.Error ->
                        logError(action = "clear", key = null, error = result.v1)
                }
            }
            // Mirrored writes must be mirrored on the way out too, or a cleared flag stays overridden.
            appSession()?.let { appSession ->
                when (val result = appSession.clearAllDebugFeatureFlagOverrides()) {
                    is MailSessionClearAllDebugFeatureFlagOverridesResult.Ok -> Unit
                    is MailSessionClearAllDebugFeatureFlagOverridesResult.Error ->
                        logError(action = "clear", key = null, error = result.v1)
                }
            }
        }
    }

    // The SDK matches on the payload, never on the variant name, so both carry the same value.
    private fun String.toFeatureFlagVariant() = FeatureFlagVariant(
        name = this,
        enabled = true,
        payload = FeatureFlagVariantPayload(ty = FeatureFlagPayloadType.STRING, value = this)
    )

    private suspend fun activeUserSession() = (sessionResolver.activeSession() as? ActiveSession.User)?.session

    private fun appSession() = sessionResolver.appSession()

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
        key: String?,
        error: Any?
    ) {
        val target = key?.let { "'$it'" } ?: "all overrides"
        Timber.e("Rust FF override: unable to $action debug override for $target: $error")
    }
}
