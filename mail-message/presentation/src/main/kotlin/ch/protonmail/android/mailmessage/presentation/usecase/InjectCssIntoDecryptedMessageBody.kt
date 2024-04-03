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

package ch.protonmail.android.mailmessage.presentation.usecase

import java.io.IOException
import android.content.Context
import android.content.res.Resources.NotFoundException
import androidx.annotation.RawRes
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import timber.log.Timber
import javax.inject.Inject

class InjectCssIntoDecryptedMessageBody @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    operator fun invoke(
        messageBodyWithType: MessageBodyWithType,
        viewModePreference: ViewModePreference = ViewModePreference.ThemeDefault
    ): String {
        val messageBodyDocument = Jsoup.parse(messageBodyWithType.messageBody)
        val messageBodyHead = messageBodyDocument.head()

        messageBodyHead.injectMetaViewport()
        messageBodyHead.injectCss(R.raw.css_reset_with_custom_props)
        if (viewModePreference in arrayOf(ViewModePreference.ThemeDefault, ViewModePreference.DarkMode)) {
            messageBodyHead.injectCss(R.raw.css_media_scheme)
        }

        return messageBodyDocument.toString()
    }

    private fun Element.injectCss(@RawRes rawResource: Int) {
        try {
            context.resources.openRawResource(rawResource).use {
                val css = it.readBytes().decodeToString()
                append("<style>$css</style>")
            }
        } catch (notFoundException: NotFoundException) {
            Timber.e(notFoundException, "Raw css resource is not found")
        } catch (ioException: IOException) {
            Timber.e(ioException, "Failed to read raw css resource")
        }
    }

    private fun Element.injectMetaViewport() {
        prepend("<meta name=\"viewport\" content=\"width=device-width, user-scalable=yes\">")
    }
}
