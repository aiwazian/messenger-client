package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.FolderInfo
import com.aiwazian.messenger.database.repository.FolderRepository
import com.aiwazian.messenger.services.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository
) : ViewModel() {
    
    val folders = folderRepository.folders
    
    private val _openFolderInfo = MutableStateFlow(FolderInfo())
    val openFolder = _openFolderInfo.asStateFlow()
    
    private val _canSave = MutableStateFlow(false)
    val canSave = _canSave.asStateFlow()
    
    val removeFolderDialog = DialogController()
    
    fun open(folderId: Int) {
        _openFolderInfo.update {
            _openFolderInfo.value.copy(
                id = folderId
            )
        }
        
        if (folderId == 0) {
            cleanData()
            updateCanSaveState()
            return
        }
        
        val folderList = folders.value.toMutableList()
        val folder = folderList.find { it.id == folderId }
        
        if (folder == null) {
            return
        }
        
        try {
            val folderChats = folderRepository.getFolderChats(folderId)
            
            _openFolderInfo.update {
                _openFolderInfo.value.copy(
                    name = folder.name,
                    chats = folderChats
                )
            }
        } catch (e: Exception) {
            Log.e(
                "FolderViewModel",
                "Error loading folder chats",
                e
            )
        }
    }
    
    fun changeFolderName(newName: String) {
        _openFolderInfo.update { _openFolderInfo.value.copy(name = newName) }
        updateCanSaveState()
    }
    
    suspend fun remove(folderId: Int) {
        try {
            val isDeleted = folderRepository.remove(folderId)
            
            if (isDeleted) {
                cleanData()
                updateCanSaveState()
            }
        } catch (e: Exception) {
            Log.e(
                "FolderViewModel",
                "Error removing folder",
                e
            )
        }
    }
    
    fun updateFolderChats(newChats: List<ChatInfo>) {
        _openFolderInfo.update { _openFolderInfo.value.copy(chats = newChats) }
        updateCanSaveState()
    }
    
    suspend fun save() {
        try {
            val folder = _openFolderInfo.value
            
            folderRepository.saveFolder(folder)
        } catch (e: Exception) {
            Log.e(
                "FolderViewModel",
                "Error creating folder",
                e
            )
        }
    }
    
    private fun cleanData() {
        _openFolderInfo.update { FolderInfo() }
    }
    
    private fun updateCanSaveState() {
        _canSave.update { _openFolderInfo.value.name.isNotBlank() }
    }
}