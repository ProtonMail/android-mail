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

package ch.protonmail.android.mailsession.data.initializer

import uniffi.mail_uniffi.RustInit
import javax.inject.Inject

class InitializeRustTlsModule @Inject constructor() {

    /**
     * Initializes the rust library TLS backend;
     * This is not expected to fail. If it does, the rust lib will panic with an unrecoverable error on failure as
     * the app shouldn't be used without such component.
     */
    operator fun invoke() {
        RustInit.init_tls()
    }

}
