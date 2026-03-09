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

package ch.protonmail.android.api

import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailsession.domain.model.RustApiConfig
import ch.protonmail.android.useragent.BuildUserAgent
import me.proton.core.configuration.EnvironmentConfigurationDefaults
import okhttp3.HttpUrl
import uniffi.mail_uniffi.ApiEnvId
import javax.inject.Inject

class MailRustApiConfig @Inject constructor(
    private val buildUserAgent: BuildUserAgent,
    private val baseApiUrl: HttpUrl
) : RustApiConfig {

    override val isDebug: Boolean
        get() = BuildConfig.DEBUG
    override val platform: String
        get() = "android"
    override val product: String
        get() = "mail"
    override val appVersion: String
        get() = BuildConfig.VERSION_NAME
    override val userAgent: String
        get() = buildUserAgent()
    override val proxy: String?
        get() = null
    override val envId: ApiEnvId
        get() = baseApiUrl.host.toApiEnv()

    private fun String.toApiEnv(): ApiEnvId {
        val apiEnvId = when {
            endsWith("${EnvironmentConfigurationDefaults.apiPrefix}.proton.black") -> ApiEnvId.Atlas
            endsWith("proton.black") -> ApiEnvId.Scientist(split(".")[1])
            else -> ApiEnvId.Prod
        }
        return apiEnvId
    }
}
