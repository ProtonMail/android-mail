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

package ch.protonmail.android.mailcommon.data.system

import java.io.File
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import ch.protonmail.android.mailcommon.domain.system.ParcelFileDescriptorProvider
import javax.inject.Inject

class ParcelFileDescriptorProviderImpl @Inject constructor() : ParcelFileDescriptorProvider {

    private val handlerThread = Handler(HandlerThread("ParcelFileDescriptorThread").apply { start() }.looper)

    override fun provideParcelFileDescriptor(file: File): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY,
            handlerThread
        ) { file.delete() }
    }
}
