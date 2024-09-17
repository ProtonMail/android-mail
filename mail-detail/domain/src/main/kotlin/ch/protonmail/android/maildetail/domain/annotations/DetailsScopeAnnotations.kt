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

package ch.protonmail.android.maildetail.domain.annotations

import javax.inject.Qualifier

/**
 * An annotates that marks a provided `CoroutineScope` that is expected to run all flow observations
 * that might need to be interrupted at some point when some event happens.
 *
 * One usage example is for instance when we are in the conversation details screen observing
 * the conversation metadata, messages, labels, and we trigger a mass deletion.
 *
 * Before doing that, we need to make sure that the flows are not being observed anymore since
 * that would cause issues due to potential race conditions.
 *
 * For more context on this, see MAILANDR-1945.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ObservableFlowScope
