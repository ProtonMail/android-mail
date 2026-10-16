/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcommon.data.file

import android.content.Intent
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The intent's mailto URI, with the recipients, subject and body sent as extras added to it.
 *
 * Some apps open an empty "mailto:" and pass the address in [Intent.EXTRA_EMAIL] instead.
 * Values already in the URI win, so nothing is added twice. A URI the intent sends with no
 * usable extras comes back unchanged.
 */
fun Intent.getMailToUri(): String {
    val mailTo = dataString.orEmpty()
    val uri = MailToParts.of(mailTo)

    val knownRecipients = uri.recipients().toMutableSet()
    fun List<String>.notYetIn() = filter { knownRecipients.add(it.lowercase()) }

    val params = buildList {
        getMailToRecipients(Intent.EXTRA_EMAIL).notYetIn().forEach { add("to" to it) }
        getMailToRecipients(Intent.EXTRA_CC).notYetIn().forEach { add("cc" to it) }
        getMailToRecipients(Intent.EXTRA_BCC).notYetIn().forEach { add("bcc" to it) }
        getMailToText(Intent.EXTRA_SUBJECT)?.takeUnless { uri.hasValue("subject") }?.let { add("subject" to it) }
        getMailToText(Intent.EXTRA_TEXT)?.takeUnless { uri.hasValue("body") }?.let { add("body" to it) }
    }
    if (params.isEmpty()) return mailTo

    val query = params.joinToString("&") { (name, value) -> "$name=${value.encodeForMailTo()}" }
    return uri.withParams(query)
}

/**
 * Recipients from an extra. Most apps send a String array; some send a single String,
 * which may list several addresses.
 */
private fun Intent.getMailToRecipients(name: String): List<String> {
    val values = getStringArrayExtra(name)?.toList()
        ?: getStringExtra(name)?.split(',', ';')
        ?: return emptyList()
    return values.map { it.trim() }.filter { it.isNotEmpty() }
}

// Read as a CharSequence, so a Spanned subject or body is kept as well.
private fun Intent.getMailToText(name: String): String? =
    getCharSequenceExtra(name)?.toString()?.takeIf { it.isNotEmpty() }

/** A mailto URI split at its query and fragment, read without changing a character of it. */
private class MailToParts(
    private val addresses: String,
    private val query: String?,
    private val fragment: String
) {

    private val params: List<Pair<String, String>> = query.orEmpty()
        .split('&')
        .filter { it.isNotEmpty() }
        .map { it.substringBefore('=').lowercase() to it.substringAfter('=', "") }

    // An empty value, as in "subject=", counts as missing; Rust keeps the last value of a param.
    fun hasValue(name: String): Boolean = params.any { (key, value) -> key == name && value.isNotBlank() }

    /** The lowercase addresses the URI already sends to, in the address part and to, cc and bcc. */
    fun recipients(): Set<String> {
        val inParams = params.filter { (key, _) -> key in RecipientParams }.map { (_, value) -> value }
        return (inParams + addresses.removePrefix(MAILTO_PREFIX))
            .flatMap { it.decodeOrSelf().split(',', ';') }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** The URI with [extraQuery] appended to its query, before any fragment. */
    fun withParams(extraQuery: String): String {
        val fullQuery = when {
            query.isNullOrEmpty() || query.endsWith('&') -> query.orEmpty() + extraQuery
            else -> "$query&$extraQuery"
        }
        return "$addresses?$fullQuery$fragment"
    }

    companion object {

        private const val MAILTO_PREFIX = "mailto:"
        private val RecipientParams = setOf("to", "cc", "bcc")

        fun of(mailTo: String): MailToParts {
            val fragmentStart = mailTo.indexOf('#').takeIf { it >= 0 } ?: mailTo.length
            val beforeFragment = mailTo.substring(0, fragmentStart)
            val queryStart = beforeFragment.indexOf('?')
            return MailToParts(
                addresses = if (queryStart >= 0) beforeFragment.substring(0, queryStart) else beforeFragment,
                query = if (queryStart >= 0) beforeFragment.substring(queryStart + 1) else null,
                fragment = mailTo.substring(fragmentStart)
            )
        }
    }
}

// A "+" in a mailto address is literal, not a space. A malformed escape such as "100%" is kept as it is.
private fun String.decodeOrSelf(): String =
    runCatching { URLDecoder.decode(replace("+", "%2B"), Charsets.UTF_8.name()) }.getOrDefault(this)

// RFC 6068 percent-encodes spaces; URLEncoder would turn them into "+".
private fun String.encodeForMailTo(): String = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
