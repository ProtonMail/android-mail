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

    ENGLISH("English", "en"),
    GERMAN("Deutsch", "de"),
    FRENCH("Français", "fr"),
    SPANISH("Español (España)", "es-ES"),
    SPANISH_LATIN_AMERICA("Español (Latinoamérica)", "es-419"),
    ITALIAN("Italiano", "it"),
    DUTCH("Nederlands", "nl"),
    POLISH("Polski", "pl"),
    PORTUGUESE_BRAZILIAN("Português (Brasil)", "pt-BR"),
    RUSSIAN("Русский", "ru"),
    KOREAN("한국어", "ko"),
    JAPANESE("日本語", "ja"),
    CATALAN("Català", "ca"),
    CZECH("Čeština", "cs"),
    DANISH("Dansk", "da"),
    FINNISH("Suomi", "fi"),
    INDONESIAN("Bahasa Indonesia", "in"),
    PORTUGUESE("Português (Portugal)", "pt-PT"),
    ROMANIAN("Română", "ro"),
    SWEDISH("Svenska", "sv-SE"),
    TURKISH("Türkçe", "tr"),
    CHINESE_SIMPLIFIED("简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文", "zh-TW"),
    HUNGARIAN("Magyar", "hu"),
    NORWEGIAN_BOKMAL("Norsk bokmål", "nb-NO"),
    SLOVAK("Slovenčina", "sk"),
    SLOVENIAN("Slovenščina", "sl"),
    GREEK("Ελληνικά", "el"),
    BELARUSIAN("Беларуская", "be"),
    UKRAINIAN("Українська", "uk");

    companion object {
        private val map = entries.associateBy { it.langTag }
        fun fromTag(tag: String?): AppLanguage? = map[tag]
    }
}
