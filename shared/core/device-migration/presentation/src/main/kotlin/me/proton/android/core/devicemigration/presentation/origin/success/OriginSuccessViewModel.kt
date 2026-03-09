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

package me.proton.android.core.devicemigration.presentation.origin.success

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import me.proton.android.core.account.domain.usecase.ObservePrimaryCoreAccount
import me.proton.android.core.devicemigration.presentation.R
import me.proton.core.compose.viewmodel.BaseViewModel
import uniffi.mail_account_uniffi.QrLoginScanScreenViewTotalScreenId
import uniffi.mail_account_uniffi.qrLoginScanScreenTotal
import javax.inject.Inject

@HiltViewModel
internal class OriginSuccessViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observePrimaryCoreAccount: ObservePrimaryCoreAccount
) : BaseViewModel<OriginSuccessAction, OriginSuccessState>(OriginSuccessAction.Load, OriginSuccessState.Loading) {

    override fun onAction(action: OriginSuccessAction): Flow<OriginSuccessState> = when (action) {
        is OriginSuccessAction.Load -> onLoad()
    }

    override suspend fun FlowCollector<OriginSuccessState>.onError(throwable: Throwable) {
        emit(
            OriginSuccessState.Error.Unknown(
                throwable.localizedMessage ?: context.getString(R.string.presentation_error_general)
            )
        )
    }

    fun onScreenView() {
        qrLoginScanScreenTotal(QrLoginScanScreenViewTotalScreenId.SUCCESS)
    }

    private fun onLoad() = flow {
        emit(OriginSuccessState.Loading)
        val userEmail = requireNotNull(observePrimaryCoreAccount().first()?.primaryEmailAddress)
        emit(OriginSuccessState.Idle(userEmail))
    }
}
