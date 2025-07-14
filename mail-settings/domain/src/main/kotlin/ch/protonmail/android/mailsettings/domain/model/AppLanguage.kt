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
    DUTCH("Nederlands", "nl"),
    SPANISH("Español (España)", "es-ES"),
    SPANISH_LATIN_AMERICA("Español (Latinoamérica)", "es-419"),
    ITALIAN("Italiano", "it"),
    POLISH("Polski", "pl"),
    PORTUGUESE_BRAZILIAN("Português (Brasil)", "pt-BR"),
    RUSSIAN("Русский", "ru"),
    TURKISH("Türkçe", "tr"),
    CATALAN("Català", "ca"),
    CZECH("Čeština", "cs"),
    DANISH("Dansk", "da"),
    FINNISH("Suomi", "fi"),
    CROATIAN("Hrvatski", "hr"),
    HUNGARIAN("Magyar", "hu"),
    INDONESIAN("Bahasa Indonesia", "in"),
    KABYLE("Taqbaylit", "kab"),
    NORWEGIAN_BOKMAL("Norsk bokmål", "nb-NO"),
    PORTUGUESE("Português (Portugal)", "pt-PT"),
    ROMANIAN("Română", "ro"),
    SLOVAK("Slovenčina", "sk"),
    SLOVENIAN("Slovenščina", "sl"),
    SWEDISH("Svenska", "sv-SE"),
    GREEK("Ελληνικά", "el"),
    BELARUSIAN("Беларуская", "be"),
    UKRAINIAN("Українська", "uk"),
    GEORGIAN("Ქართული", "ka"),
    HINDI("हिन्दी (भारत)", "hi"),
    KOREAN("한국어", "ko"),
    JAPANESE("日本語", "ja"),
    CHINESE_SIMPLIFIED("简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文", "zh-TW");

    companion object {
        private val map = values().associateBy { it.langTag }
        fun fromTag(tag: String?): AppLanguage? = map[tag]
    }
}
