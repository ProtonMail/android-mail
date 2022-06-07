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

package ch.protonmail.android.mailsettings.domain.model

enum class AppLanguage(val langName: String, val langTag: String) {

    BRAZILIAN("Português (Brasil)", "pt-BR"),
    CATALAN("Català", "ca-ES"),
    CHINESE_SIMPLIFIED("简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文", "zh-TW"),
    CROATIAN("Hrvatski", "hr"),
    CZECH("Čeština", "cs"),
    DANISH("Dansk", "da"),
    DUTCH("Nederlands", "nl"),
    ENGLISH("English", "en"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    GREEK("Ελληνικά", "el"),
    HUNGARIAN("Magyar", "hu-HU"),
    ICELANDIC("íslenska", "is-IS"),
    INDONESIAN("Bahasa (Indonesia)", "in"),
    ITALIAN("Italiano", "it"),
    JAPANESE("日本語", "ja"),
    KABYLIAN("Taqbaylit", "kab"),
    POLISH("Polski", "pl"),
    PORTUGUESE("Português (Portugal)", "pt-PT"),
    ROMANIAN("Română", "ro"),
    RUSSIAN("Русский", "ru"),
    SPANISH("Español (España)", "es-ES"),
    SWEDISH("Svenska", "sv-SE"),
    TURKISH("Türkçe", "tr"),
    UKRAINIAN("Українська", "uk");

    companion object {
        private val map = values().associateBy { it.langTag }
        fun fromTag(tag: String?): AppLanguage? = map[tag]
    }
}
