package com.aiwazian.messenger.ui.group

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.services.GroupService
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.services.UserService
import com.aiwazian.messenger.ui.ProfileScreen
import com.aiwazian.messenger.ui.element.MinimizeChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.utils.Shape
import com.aiwazian.messenger.viewModels.GroupViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupMembersScreen(groupId: Long) {
    val navViewModel = viewModel<NavigationViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    val groupService = GroupService()
    
    var members by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            members = groupService.getMembers(groupId).orEmpty()
        } catch (e: Exception) {
            Log.e(
                "GroupMemberScreen",
                "Не удалось получить участников группы",
                e
            )
        }
    }
    
    val scope = rememberCoroutineScope()
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.members)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = navViewModel::removeLastScreenInStack
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    icon = Icons.Outlined.PersonAdd,
                    text = stringResource(R.string.add_member),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        navViewModel.addScreenInStack {
                            AddMemberScreen(members.map { it.id }.toLongArray()) { users ->
                                val userService = UserService()
                                
                                scope.launch {
                                    val successfullyAddedUsers = mutableListOf<UserInfo>()
                                    
                                    users.forEach { userId ->
                                        try {
                                            groupService.inviteUserToGroup(
                                                groupId,
                                                userId
                                            )
                                        } catch (e: Exception) {
                                            Log.e(
                                                "AddMemberScreen",
                                                "Не удалось добавить пользователя в группу",
                                                e
                                            )
                                        }
                                        
                                        try {
                                            val user = userService.getById(userId)
                                            if (user != null) {
                                                successfullyAddedUsers.add(user)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "AddMemberScreen",
                                                "Не удалось получить информацию о добавленном пользователе",
                                                e
                                            )
                                        }
                                    }
                                    
                                    if (successfullyAddedUsers.isNotEmpty()) {
                                        members = (members + successfullyAddedUsers).distinctBy {
                                            it.id
                                        }
                                        groupViewModel.changeMembers(members.size)
                                    }
                                }
                            }
                        }
                    }
                )
            }
            
            SectionContainer {
                LazyColumn {
                    items(
                        items = members,
                        key = { it.id }) { member ->
                        MinimizeChatCard(
                            chatName = "${member.firstName} ${member.lastName}",
                            trailingContent = {
                                if (member.id != UserManager.user.collectAsState().value.id) {
                                    var expanded by remember { mutableStateOf(false) }
                                    
                                    IconButton(onClick = {
                                        expanded = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = expanded,
                                        shape = Shape.DropdownMenu,
                                        onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Удалить",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                scope.launch {
                                                    val isRemoved =
                                                        groupService.removeUserFromGroup(
                                                            groupId,
                                                            member.id
                                                        )
                                                    
                                                    if (isRemoved) {
                                                        members =
                                                            members.filter { it.id != member.id }
                                                    }
                                                }
                                            })
                                    }
                                }
                            },
                            onClick = {
                                navViewModel.addScreenInStack {
                                    ProfileScreen(member.id)
                                }
                            })
                    }
                }
            }
        }
    }
}
