package com.rayject.storeray.usecase

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SyncAppInfoUseCaseTest {

    private val service = mockk<AppInfoService>()

    @Test
    fun `no update call when local data is empty`() = runBlocking {
        val useCase = SyncAppInfoUseCase(service, emptyMap())
        useCase.execute(dryRun = false)
        coVerify(exactly = 0) { service.update(any()) }
    }

    @Test
    fun `new locale triggers create in apply mode`() = runBlocking {
        coEvery { service.fetch() } returns emptyMap()
        coEvery { service.update(any()) } returns Unit

        val local = mapOf("en-US" to AppInfoData(name = "New App"))
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = false)

        coVerify { service.update(mapOf("en-US" to AppInfoData(name = "New App"))) }
    }

    @Test
    fun `no update call in dry run mode`() = runBlocking {
        coEvery { service.fetch() } returns emptyMap()

        val local = mapOf("en-US" to AppInfoData(name = "New App"))
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = true)

        coVerify(exactly = 0) { service.update(any()) }
    }

    @Test
    fun `locale with diffs triggers update in apply mode`() = runBlocking {
        coEvery { service.fetch() } returns mapOf(
            "en-US" to AppInfoData(name = "Old Name", subtitle = "Same Subtitle")
        )
        coEvery { service.update(any()) } returns Unit

        val local = mapOf("en-US" to AppInfoData(name = "New Name", subtitle = "Same Subtitle"))
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = false)

        coVerify { service.update(mapOf("en-US" to AppInfoData(name = "New Name", subtitle = "Same Subtitle"))) }
    }

    @Test
    fun `up to date locale skips update`() = runBlocking {
        coEvery { service.fetch() } returns mapOf(
            "en-US" to AppInfoData(name = "Same Name", description = "Same desc")
        )

        val local = mapOf("en-US" to AppInfoData(name = "Same Name", description = "Same desc"))
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = false)

        coVerify(exactly = 0) { service.update(any()) }
    }

    @Test
    fun `multiple locales only updates changed ones`() = runBlocking {
        coEvery { service.fetch() } returns mapOf(
            "en-US" to AppInfoData(name = "Old"),
            "zh-Hans" to AppInfoData(name = "Same")
        )
        coEvery { service.update(any()) } returns Unit

        val local = mapOf(
            "en-US" to AppInfoData(name = "New"),
            "zh-Hans" to AppInfoData(name = "Same")
        )
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = false)

        coVerify { service.update(mapOf("en-US" to AppInfoData(name = "New"))) }
    }

    @Test
    fun `no update call when service fetch throws`() = runBlocking {
        coEvery { service.fetch() } throws RuntimeException("API error")

        val local = mapOf("en-US" to AppInfoData(name = "App"))
        val useCase = SyncAppInfoUseCase(service, local)
        useCase.execute(dryRun = false)

        coVerify(exactly = 0) { service.update(any()) }
    }
}
