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

package ch.protonmail.android.mailcommon.data.system

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.mailcommon.domain.model.SenderImageTheme
import ch.protonmail.android.mailcommon.domain.usecase.SenderImageModeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SenderImageModeProviderImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : SenderImageModeProvider {

    override operator fun invoke(): SenderImageTheme {
        val isDark = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> applicationContext.resources.configuration.let {
                it.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return if (isDark) SenderImageTheme.Dark else SenderImageTheme.Light
    }
}
