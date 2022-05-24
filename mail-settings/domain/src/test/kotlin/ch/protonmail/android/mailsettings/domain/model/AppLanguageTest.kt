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

import org.junit.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun `AppLanguage tag is correct for all languages`() {
        assertEquals("en", AppLanguage.ENGLISH.langTag)
        assertEquals("en", AppLanguage.ENGLISH.langTag)
        assertEquals("ca-ES", AppLanguage.CATALAN.langTag)
        assertEquals("cs", AppLanguage.CZECH.langTag)
        assertEquals("da", AppLanguage.DANISH.langTag)
        assertEquals("de", AppLanguage.GERMAN.langTag)
        assertEquals("el", AppLanguage.GREEK.langTag)
        assertEquals("es-ES", AppLanguage.SPANISH.langTag)
        assertEquals("fr", AppLanguage.FRENCH.langTag)
        assertEquals("hr", AppLanguage.CROATIAN.langTag)
        assertEquals("hu-HU", AppLanguage.HUNGARIAN.langTag)
        assertEquals("in", AppLanguage.INDONESIAN.langTag)
        assertEquals("is-IS", AppLanguage.ICELANDIC.langTag)
        assertEquals("it", AppLanguage.ITALIAN.langTag)
        assertEquals("ja", AppLanguage.JAPANESE.langTag)
        assertEquals("kab", AppLanguage.KABYLIAN.langTag)
        assertEquals("nl", AppLanguage.DUTCH.langTag)
        assertEquals("pl", AppLanguage.POLISH.langTag)
        assertEquals("pt-PT", AppLanguage.PORTUGUESE.langTag)
        assertEquals("pt-BR", AppLanguage.BRAZILIAN.langTag)
        assertEquals("ro", AppLanguage.ROMANIAN.langTag)
        assertEquals("ru", AppLanguage.RUSSIAN.langTag)
        assertEquals("sv-SE", AppLanguage.SWEDISH.langTag)
        assertEquals("tr", AppLanguage.TURKISH.langTag)
        assertEquals("uk", AppLanguage.UKRAINIAN.langTag)
        assertEquals("zh-TW", AppLanguage.CHINESE_TRADITIONAL.langTag)
        assertEquals("zh-CN", AppLanguage.CHINESE_SIMPLIFIED.langTag)
    }
}
