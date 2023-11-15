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

package ch.protonmail.android.mailcommon.data.repository

import java.util.Locale
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.mailcommon.domain.repository.AppLocaleRepository

class AppLocaleRepositoryImpl(val context: Context) : AppLocaleRepository, BroadcastReceiver() {

    private var savedLocale: Locale? = null

    init {
        val localeBroadcastFilter = IntentFilter().apply {
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        context.registerReceiver(this, localeBroadcastFilter)
    }

    override fun current(): Locale {
        // AppCompatDelegate.getApplicationLocales makes IPC call to Locale Service on Android 13 and above.
        // So it's better to cache the result
        if (savedLocale == null) {
            savedLocale = obtainCurrentLocale()
        }

        return savedLocale ?: Locale.getDefault() // Use default Locale if savedLocale is null
    }

    override fun refresh() {
        savedLocale = obtainCurrentLocale()
    }

    private fun obtainCurrentLocale(): Locale {
        val savedAppLocales = AppCompatDelegate.getApplicationLocales()
        val languageTag = savedAppLocales[0]?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()
        return Locale.forLanguageTag(languageTag)
    }

    // Refresh saved locale when app locale changes
    override fun onReceive(context: Context?, intent: Intent?) {
        refresh()
    }
}
