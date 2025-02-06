package ch.protonmail.android.maillabel.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.After
import org.junit.Test
import kotlin.test.assertContentEquals

class ObserveLabelsTest {

    private val userId = UserIdTestData.userId

    private val repo = mockk<LabelRepository>()

    private val observeLabels = ObserveLabels(repo)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `maps non-folder items without changes`() = runTest {
        // Given
        val expectedLabels = listOf(
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id3", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id4", order = 1),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id5", order = 2)
        )
        every { repo.observeLabels(userId = userId, type = LabelType.MessageLabel) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Remote,
                value = expectedLabels
            )
        )

        // When
        observeLabels.invoke(userId, LabelType.MessageLabel).test {
            // Then
            val item = awaitItem()
            assertContentEquals(expectedLabels, item.getOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `filters folders with deleted parents`() = runTest {
        // Given
        val localItems = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id3", order = 0, parentId = null),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id4", order = 1, parentId = "id3"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id5", order = 1, parentId = "id4"),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id6", order = 2,
                parentId = "already_deleted"
            ),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id7", order = 2,
                parentId = "id6"
            ),
            buildLabel(
                userId = userId, type = LabelType.MessageFolder, id = "id8", order = 2,
                parentId = "id7"
            )
        )
        every { repo.observeLabels(userId = userId, type = LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Local,
                value = localItems
            )
        )

        // When
        observeLabels.invoke(userId, LabelType.MessageFolder).test {
            // Then
            val item = awaitItem()
            val expectedLabels = listOf(
                buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id3", order = 0, parentId = null),
                buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id4", order = 1, parentId = "id3"),
                buildLabel(userId = userId, type = LabelType.MessageFolder, id = "id5", order = 1, parentId = "id4")
            )
            assertContentEquals(expectedLabels, item.getOrNull())
            awaitComplete()
        }
    }
}
