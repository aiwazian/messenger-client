/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.DownloaderManager
import javax.inject.Inject

class DownloadAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val channelRepository: ChannelRepository,
    private val downloaderManager: DownloaderManager
) {
    suspend operator fun invoke(profileId: Long, fileId: String): Result<Unit> {
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
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}