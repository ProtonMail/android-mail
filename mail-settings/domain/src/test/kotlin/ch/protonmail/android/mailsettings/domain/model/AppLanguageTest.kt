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

import org.junit.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun `AppLanguage tag is correct for all languages`() {
        assertEquals("en", AppLanguage.ENGLISH.langTag)
        assertEquals("de", AppLanguage.GERMAN.langTag)
        assertEquals("fr", AppLanguage.FRENCH.langTag)
        assertEquals("nl", AppLanguage.DUTCH.langTag)
        assertEquals("es-ES", AppLanguage.SPANISH.langTag)
        assertEquals("es-419", AppLanguage.SPANISH_LATIN_AMERICA.langTag)
        assertEquals("it", AppLanguage.ITALIAN.langTag)
        assertEquals("pl", AppLanguage.POLISH.langTag)
        assertEquals("pt-BR", AppLanguage.PORTUGUESE_BRAZILIAN.langTag)
        assertEquals("ru", AppLanguage.RUSSIAN.langTag)
        assertEquals("tr", AppLanguage.TURKISH.langTag)
        assertEquals("ca", AppLanguage.CATALAN.langTag)
        assertEquals("cs", AppLanguage.CZECH.langTag)
        assertEquals("da", AppLanguage.DANISH.langTag)
        assertEquals("fi", AppLanguage.FINNISH.langTag)
        assertEquals("hr", AppLanguage.CROATIAN.langTag)
        assertEquals("hu", AppLanguage.HUNGARIAN.langTag)
        assertEquals("in", AppLanguage.INDONESIAN.langTag)
        assertEquals("kab", AppLanguage.KABYLE.langTag)
        assertEquals("nb-NO", AppLanguage.NORWEGIAN_BOKMAL.langTag)
        assertEquals("pt-PT", AppLanguage.PORTUGUESE.langTag)
        assertEquals("ro", AppLanguage.ROMANIAN.langTag)
        assertEquals("sk", AppLanguage.SLOVAK.langTag)
        assertEquals("sl", AppLanguage.SLOVENIAN.langTag)
        assertEquals("sv-SE", AppLanguage.SWEDISH.langTag)
        assertEquals("el", AppLanguage.GREEK.langTag)
        assertEquals("be", AppLanguage.BELARUSIAN.langTag)
        assertEquals("uk", AppLanguage.UKRAINIAN.langTag)
        assertEquals("ka", AppLanguage.GEORGIAN.langTag)
        assertEquals("hi", AppLanguage.HINDI.langTag)
        assertEquals("ko", AppLanguage.KOREAN.langTag)
        assertEquals("ja", AppLanguage.JAPANESE.langTag)
        assertEquals("zh-CN", AppLanguage.CHINESE_SIMPLIFIED.langTag)
        assertEquals("zh-TW", AppLanguage.CHINESE_TRADITIONAL.langTag)
    }
}
