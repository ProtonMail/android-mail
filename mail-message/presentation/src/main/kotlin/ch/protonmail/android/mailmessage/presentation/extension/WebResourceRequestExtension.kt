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

package ch.protonmail.android.mailmessage.presentation.extension

import java.io.IOException
import java.net.URL
import java.util.regex.Pattern
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import timber.log.Timber

fun WebResourceRequest.isRemoteContent() = url.scheme?.let {
    Pattern.compile("https?").matcher(it).matches()
} ?: false

fun WebResourceRequest.isRemoteUnsecuredContent() = url.scheme?.let {
    Pattern.compile("http[^s]?").matcher(it).matches()
} ?: false

fun WebResourceRequest.isEmbeddedImage() = url.scheme?.let {
    Pattern.compile("cid").matcher(it).matches()
} ?: false

fun WebResourceRequest.getSecuredWebResourceResponse(): WebResourceResponse {
    return try {
        val httpsUrl = URL(this.url.toString().replaceFirst("http://", "https://"))
        val connection = httpsUrl.openConnection()
        return WebResourceResponse(
            connection.contentType,
            connection.contentEncoding,
            connection.getInputStream()
        )
    } catch (e: IOException) {
        Timber.d("Error in upgradeToSecuredWebResourceResponse", e)
        WebResourceResponse(
            "",
            "",
            null
        )
    } catch (e: IllegalArgumentException) {
        Timber.d("Error in upgradeToSecuredWebResourceResponse", e)
        WebResourceResponse(
            "",
            "",
            null
        )
    }
}
