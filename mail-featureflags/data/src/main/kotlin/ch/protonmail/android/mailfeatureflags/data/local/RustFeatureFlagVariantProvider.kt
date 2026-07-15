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

package ch.protonmail.android.mailfeatureflags.data.local

import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagVariantProvider
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariant
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariantPayload
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariantPayloadType
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uniffi.mail_uniffi.MailSessionGetFeatureFlagVariantResult
import uniffi.mail_uniffi.MailUserSessionGetFeatureFlagVariantResult
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.mail_uniffi.FeatureFlagVariant as LocalFeatureFlagVariant
import uniffi.mail_uniffi.FeatureFlagPayloadType as LocalFeatureFlagVariantPayloadType

/**
 * Reads feature-flag variants from the Rust SDK. Session selection lives in
 * [RustFeatureFlagSessionResolver].
 */
@Singleton
class RustFeatureFlagVariantProvider @Inject constructor(
    private val sessionResolver: Lazy<RustFeatureFlagSessionResolver>,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : FeatureFlagVariantProvider {

    override suspend fun getVariant(key: String): FeatureFlagVariant? = withContext(ioDispatcher) {
        when (val session = sessionResolver.get().activeSession()) {
            is ActiveSession.User -> when (val result = session.session.getFeatureFlagVariant(featureId = key)) {
                is MailUserSessionGetFeatureFlagVariantResult.Ok -> result.v1?.toFeatureFlagVariant()
                is MailUserSessionGetFeatureFlagVariantResult.Error -> null
            }

            is ActiveSession.App -> when (val result = session.session.getFeatureFlagVariant(featureId = key)) {
                is MailSessionGetFeatureFlagVariantResult.Ok -> result.v1?.toFeatureFlagVariant()
                is MailSessionGetFeatureFlagVariantResult.Error -> null
            }
        }
    }
}

private fun LocalFeatureFlagVariant.toFeatureFlagVariant() = FeatureFlagVariant(
    name = name,
    enabled = enabled,
    payload = payload?.let { FeatureFlagVariantPayload(type = it.ty.toPayloadType(), value = it.value) }
)

private fun LocalFeatureFlagVariantPayloadType.toPayloadType() = when (this) {
    LocalFeatureFlagVariantPayloadType.JSON -> FeatureFlagVariantPayloadType.JSON
    LocalFeatureFlagVariantPayloadType.CSV -> FeatureFlagVariantPayloadType.CSV
    LocalFeatureFlagVariantPayloadType.STRING -> FeatureFlagVariantPayloadType.STRING
    LocalFeatureFlagVariantPayloadType.NUMBER -> FeatureFlagVariantPayloadType.NUMBER
}
