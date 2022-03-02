/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation

import android.content.Context
import android.content.pm.PackageManager
import ch.protonmail.android.mailsettings.domain.model.AppInformation
import javax.inject.Inject

class GetAppInformation @Inject constructor(private val context: Context) {

    operator fun invoke(): AppInformation {
        val manager = context.packageManager
        val info = manager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        return AppInformation(info.versionName)
    }
}
