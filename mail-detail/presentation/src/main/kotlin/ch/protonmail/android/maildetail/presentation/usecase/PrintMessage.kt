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

package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import javax.inject.Inject

class PrintMessage @Inject constructor() {

    operator fun invoke(context: Context, printDocumentAdapter: PrintDocumentAdapter) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        printManager.print(
            JobName,
            printDocumentAdapter,
            PrintAttributes.Builder().build()
        )
    }

    companion object {
        const val JobName = "Proton Mail Document"
    }
}
