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

package ch.protonmail.android.debug

import androidx.lifecycle.ViewModel
import ch.protonmail.android.mailcommon.domain.AppInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Holds the build identifiers shown at the bottom of the debug menu. */
@HiltViewModel
internal class ApplicationDebugViewModel @Inject constructor(appInformation: AppInformation) : ViewModel() {

    val appVersion = with(appInformation) { "$appVersionName ($appVersionCode) - $rustSdkVersion" }
}
