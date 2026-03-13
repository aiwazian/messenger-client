package com.aiwazian.messenger.viewModels

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(private val groupRepository: GroupRepository) :
    ViewModel() {
    
    private val _groupInfo = MutableStateFlow(GroupInfo())
    val groupInfo = _groupInfo.asStateFlow()
    
    fun changeGroupName(newName: String) {
        _groupInfo.update { it.copy(name = newName) }
    }
    
    fun changeMembers(count: Int) {
        _groupInfo.update { it.copy(members = count) }
    }
    
    fun open(groupInfo: GroupInfo) {
        _groupInfo.update { groupInfo }
    }
    
    fun checkValid(): Boolean {
        return _groupInfo.value.name.isNotBlank()
    }
    
    fun cleanData() {
        _groupInfo.update { GroupInfo() }
    }
    
    suspend fun createGroup(): Long? {
        return groupRepository.create(_groupInfo.value)
    }
}