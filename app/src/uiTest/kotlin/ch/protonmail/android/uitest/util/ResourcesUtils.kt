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

package ch.protonmail.android.uitest.util

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.uitest.util.InstrumentationHolder.instrumentation

fun getString(text: TextUiModel): String = when (text) {
    is TextUiModel.Text -> text.value
    is TextUiModel.TextRes -> getString(text.value)
    is TextUiModel.TextResWithArgs -> getString(text.value, *text.formatArgs.toTypedArray())
    is TextUiModel.PluralisedText -> getTestString(text.value, text.value)
}

fun getString(@StringRes resId: Int): String = ApplicationProvider.getApplicationContext<Application>().getString(resId)

fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
    ApplicationProvider.getApplicationContext<Application>().getString(resId, formatArgs)

fun getTestString(@StringRes resId: Int, vararg formatArgs: Any): String =
    instrumentation.context.getString(resId, *formatArgs)

fun getTestString(@PluralsRes pluralResId: Int, value: Int): String =
    instrumentation.context.resources.getQuantityString(pluralResId, value, value)
