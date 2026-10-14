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

package ch.protonmail.android.useragent

import ch.protonmail.android.useragent.model.AppBuildInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns the app version name, tagged with its install source.
 *
 * Play Store installs keep the plain version name.
 * Any other install source (sideload, other stores) gets a `+apk` suffix.
 *
 * Dev and alpha builds are left untouched.
 * They are never distributed through the Play Store, and their version already carries `+gitHash`.
 */
@Singleton
class GetAppVersion @Inject constructor(
    private val appBuildInfo: AppBuildInfo,
    private val getInstallerPackageName: GetInstallerPackageName
) {

    /** The suffix alone, empty for Play Store installs and non-prod builds. */
    val installSourceSuffix: String by lazy { resolveInstallSourceSuffix() }

    operator fun invoke(): String = "${appBuildInfo.versionName}$installSourceSuffix"

    private fun resolveInstallSourceSuffix(): String {
        if (appBuildInfo.flavor != PROD_FLAVOR) return ""
        if (getInstallerPackageName() == PLAY_STORE_PACKAGE) return ""
        return APK_SUFFIX
    }

    private companion object {

        const val PROD_FLAVOR = "prod"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val APK_SUFFIX = "+apk"
    }
}
