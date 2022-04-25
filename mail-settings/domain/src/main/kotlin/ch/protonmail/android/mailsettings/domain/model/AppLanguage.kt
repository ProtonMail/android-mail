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

package ch.protonmail.android.mailsettings.domain.model

enum class AppLanguage(val langName: String, val langTag: String) {
    ENGLISH("English", "en_EN"),
    CATALAN("Català", "ca_ES"),
    CZECH("Čeština", "cs_CS"),
    DANISH("Dansk", "da_DA"),
    GERMAN("Deutsch", "de_DE"),
    GREEK("Ελληνικά", "el_EL"),
    SPANISH("Español", "es_ES"),
    FRENCH("Français", "fr_FR"),
    CROATIAN("Hrvatski", "hr_HR"),
    HUNGARIAN("Magyar", "hu_HU"),
    INDONESIAN("Bahasa (Indonesia)", "in_IN"),
    ICELANDIC("íslenska", "is_IS"),
    ITALIAN("Italiano", "it_IT"),
    JAPANESE("日本語", "ja_JA"),
    KABYLIAN("Taqbaylit", "kab_KAB"),
    DUTCH("Nederlands", "nl_NL"),
    POLISH("Polski", "pl_PL"),
    PORTUGUESE("Português (Portugal)", "pt_PT"),
    BRAZILIAN("Português (Brasil)", "pt_BR"),
    ROMANIAN("Română", "ro_RO"),
    RUSSIAN("Русский", "ru_RU"),
    SWEDISH("Svenska", "sv_SE"),
    TURKISH("Türkçe", "tr_TR"),
    UKRAINIAN("Українська", "uk_UK"),
    CHINESE_TRADITIONAL("繁體中文", "zh_TW"),
    CHINESE_SIMPLIFIED("简体中文", "zh_CN");

    companion object {
        private val map = values().associateBy { it.name }
        fun enumOf(value: String?): AppLanguage? = map[value]
    }
}
