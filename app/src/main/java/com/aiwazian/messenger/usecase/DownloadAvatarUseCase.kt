/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.util.Log
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.domain.AvatarNotFoundException
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.ShortcutManager
import javax.inject.Inject

class DownloadAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val channelRepository: ChannelRepository,
    private val downloaderManager: DownloaderManager,
    private val avatarDao: AvatarDao,
    private val fileRepository: FileRepository,
    private val shortcutManager: ShortcutManager
) {
    suspend operator fun invoke(
        profileId: Long,
        fileId: String
    ): Result<Unit> {
        val downloadUrlResult = when (ChatType.fromId(profileId)) {
            ChatType.PRIVATE -> userRepository.getAvatarDownloadUrl(fileId)
            ChatType.GROUP -> groupRepository.getAvatarDownloadUrl(fileId)
            ChatType.CHANNEL -> channelRepository.getAvatarDownloadUrl(fileId)
            else -> return Result.failure(Exception("Invalid profile type"))
        }
        
        return downloadUrlResult.fold(
            onSuccess = { downloadUrl ->
                runCatching {
                    downloaderManager.download(
                        url = downloadUrl,
                        fileId = fileId,
                        fileName = fileId
                    )
                }.onSuccess {
                    /*
                     * Файл лёг на диск, и это единственное место, куда приходят аватарки
                     * всех типов чатов. Значит здесь же удобно обновить иконку ярлыка,
                     * если он закреплён на рабочем столе: аватарку могли поменять.
                     */
                    shortcutManager.refreshPinnedChatShortcut(profileId)
                }
            },
            onFailure = { error ->
                /*
                 * Аватарку заменили или удалили с другого устройства. Строку надо убрать
                 * из Room, иначе экран будет бесконечно дёргать мёртвый fileId при каждом
                 * открытии профиля. Остальные ошибки (нет сети, 5xx) кэш не трогают.
                 */
                if (error is AvatarNotFoundException) {
                    forgetAvatar(fileId)
                }
                
                Result.failure(error)
            }
        )
    }
    
    private suspend fun forgetAvatar(fileId: String) {
        Log.i(TAG, "Аватарки $fileId нет на сервере, удаляю из кэша")
        
        avatarDao.deleteAvatarByFileId(fileId)
        fileRepository.deleteFile(fileId)
    }
    
    private companion object {
        const val TAG = "DownloadAvatarUseCase"
    }
}
