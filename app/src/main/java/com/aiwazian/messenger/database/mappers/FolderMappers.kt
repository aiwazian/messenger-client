package com.aiwazian.messenger.database.mappers
import com.aiwazian.messenger.data.FolderInfo
import com.aiwazian.messenger.database.entity.FolderEntity

fun FolderInfo.toEntity(): FolderEntity {
    return FolderEntity(
        id = this.id,
        folderName = this.name
    )
}

fun FolderEntity.toFolder(): FolderInfo {
    return FolderInfo(
        id = this.id,
        name = this.folderName
    )
}
