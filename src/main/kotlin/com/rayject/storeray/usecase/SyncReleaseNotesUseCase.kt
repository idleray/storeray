package com.rayject.storeray.usecase

import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.util.Console

class SyncReleaseNotesUseCase(
    private val releaseNotesService: ReleaseNotesService,
    private val localNotes: Map<String, String>
) {

    suspend fun execute(appVersion: String, dryRun: Boolean) {
        Console.step(if (dryRun) "预览模式（不会实际修改 Release Notes）" else "执行 Release Notes 同步")
        
        if (localNotes.isEmpty()) {
            Console.error("未在工作区找到版本 $appVersion 的 Release Notes。")
            return
        }

        try {
            Console.info("正在获取远端版本 $appVersion 的 Release Notes...")
            val existingNotes = releaseNotesService.fetch(appVersion)
            
            val localNotesToUpdate = mutableMapOf<String, String>()
            
            for ((locale, newContentRaw) in localNotes) {
                val newContent = newContentRaw.trim()
                val oldContent = existingNotes[locale]?.trim() ?: ""
                
                if (newContent != oldContent) {
                    localNotesToUpdate[locale] = newContent
                    Console.detail("[~] $locale: 需要更新")
                    Console.detail("    旧版: \"$oldContent\"")
                    Console.detail("    新版: \"$newContent\"")
                } else {
                    Console.detail("[=] $locale: 无需更新")
                }
            }
            
            Console.divider()
            
            if (localNotesToUpdate.isEmpty()) {
                Console.success("所有的 Release Notes 都已是最新状态")
                return
            }
            
            Console.info("总共有 ${localNotesToUpdate.size} 个语言需要更新")
            
            if (!dryRun) {
                try {
                    releaseNotesService.update(appVersion, localNotesToUpdate)
                    Console.success("Release Notes 更新成功！")
                } catch (e: Exception) {
                    Console.error("更新失败: ${e.message}")
                }
            } else {
                Console.info("💡 提示: 使用 --apply 参数执行实际同步")
            }
            
        } catch (e: Exception) {
            Console.error("同步过程中发生异常: ${e.message}")
            e.printStackTrace()
        }
    }
}
