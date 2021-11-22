/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.logging

import me.proton.core.util.kotlin.Logger
import me.proton.core.util.kotlin.LoggerLogTag
import org.jetbrains.annotations.NonNls
import timber.log.Timber

class AppLogger : Logger {

    override fun e(tag: String, e: Throwable) =
        Timber.tag(tag).e(e)

    override fun e(tag: String, e: Throwable, @NonNls message: String) =
        Timber.tag(tag).e(e, message)

    override fun i(tag: String, @NonNls message: String) =
        Timber.tag(tag).i(message)

    override fun i(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).i(e, message)

    override fun d(tag: String, message: String) =
        Timber.tag(tag).d(message)

    override fun d(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).d(e, message)

    override fun v(tag: String, message: String) =
        Timber.tag(tag).v(message)

    override fun v(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).v(e, message)

    override fun log(tag: LoggerLogTag, message: String) = when (tag) {
        me.proton.core.network.data.LogTag.API_CALL -> Timber.tag(tag.name).i(message)
        me.proton.core.network.data.LogTag.REFRESH_TOKEN -> Timber.tag(tag.name).d(message)
        me.proton.core.network.data.LogTag.SERVER_TIME_PARSE_ERROR -> Timber.tag(tag.name).e(message)
        else -> Timber.tag(tag.name).d(message)
    }
}
