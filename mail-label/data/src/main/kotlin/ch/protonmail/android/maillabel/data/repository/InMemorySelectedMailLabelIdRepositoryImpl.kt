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

package ch.protonmail.android.maillabel.data.repository

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithLocationChangeStatus
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.asLocationLoaded
import ch.protonmail.android.maillabel.domain.model.asLocationRequested
import ch.protonmail.android.maillabel.domain.model.isLoaded
import ch.protonmail.android.maillabel.domain.model.isRequested
import ch.protonmail.android.maillabel.domain.repository.SelectedMailLabelIdRepository
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class InMemorySelectedMailLabelIdRepositoryImpl @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val findLocalSystemLabelId: FindLocalSystemLabelId,
    private val observePrimaryUserId: ObservePrimaryUserId
) : SelectedMailLabelIdRepository {

    private val mutableFlow = MutableStateFlow<MailLabelIdWithLocationChangeStatus?>(null)

    private val baseFlowOfAllLabelChanges: StateFlow<MailLabelIdWithLocationChangeStatus?> = mutableFlow.asStateFlow()

    private val loadedFlow: Flow<MailLabelIdWithCategory> = baseFlowOfAllLabelChanges
        .filterNotNull()
        .filter { it.isLoaded() }
        .map { MailLabelIdWithCategory(it.mailLabelId, it.categoryLabelId) }
        .distinctUntilChanged()

    private val requestedFlow: Flow<MailLabelIdWithCategory> = baseFlowOfAllLabelChanges
        .filterNotNull()
        .filter { it.isRequested() }
        .map { MailLabelIdWithCategory(it.mailLabelId, it.categoryLabelId) }
        .distinctUntilChanged()
        .shareIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private var isInitialLocationSelected = AtomicBoolean(false)

    init {
        observePrimaryUserId()
            .filterNotNull()
            .onEach { userId ->
                selectInitialLocation(userId)
            }
            .launchIn(appScope)
    }

    override fun selectLocation(mailLabelId: MailLabelId) {
        appScope.launch {
            mutableFlow.emit(mailLabelId.asLocationRequested())
        }
    }

    override fun selectCategory(categoryLabelId: CategoryLabelId) {
        appScope.launch {
            mutableFlow.emit(categoryLabelId.asLocationRequested(getSelectedMailLabelId()))
        }
    }

    override fun setLocationAsLoaded(mailLabelIdWithCategory: MailLabelIdWithCategory) {
        appScope.launch {
            mutableFlow.emit(mailLabelIdWithCategory.asLocationLoaded())
        }
    }

    override suspend fun getSelectedMailLabelId(): MailLabelId =
        baseFlowOfAllLabelChanges.filterNotNull().first().mailLabelId

    override fun observeLoadedMailLabelId(): Flow<MailLabelId> = loadedFlow.map {
        it.mailLabelId
    }

    override fun observeLoadedLabelWithCategory(): Flow<MailLabelIdWithCategory> = loadedFlow

    override fun observeSelectedMailLabelId(): Flow<MailLabelId> = requestedFlow.map {
        it.mailLabelId
    }

    override fun observeSelectedLabelWithCategory(): Flow<MailLabelIdWithCategory> = requestedFlow


    override suspend fun selectInitialLocationIfNeeded(userId: UserId, mailLabelIds: Set<MailLabelId>) {
        if (isInitialLocationSelected.load().not() || getSelectedMailLabelId() !in mailLabelIds) {
            selectInitialLocation(userId)
        }
    }

    private suspend fun selectInitialLocation(userId: UserId) {
        getInitialLabelId(userId)?.let {
            selectLocation(it)
            isInitialLocationSelected.store(true)
        } ?: Timber.d("Initial label id was not selected")
    }

    private suspend fun getInitialLabelId(userId: UserId): MailLabelId? =
        findLocalSystemLabelId(userId, SystemLabelId.Inbox)
}
