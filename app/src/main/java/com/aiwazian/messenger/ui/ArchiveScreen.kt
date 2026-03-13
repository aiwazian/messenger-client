package com.aiwazian.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.DropdownMenuAction
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SwipeableChatCard
import com.aiwazian.messenger.viewModels.ArchiveViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun ArchiveScreen(mainViewModel: MainViewModel) {
    Content(mainViewModel)
}

@Composable
private fun Content(mainViewModel: MainViewModel) {
    val navViewModel = viewModel<NavigationViewModel>()
    val archiveViewModel = viewModel<ArchiveViewModel>()
    
    val archiveInfoDialog = archiveViewModel.archiveInfoBottomDialog
    
    val chatList by mainViewModel.archivedChats.collectAsState()
    
    val action = listOf(
        TopBarAction(
            icon = Icons.Outlined.MoreVert,
            dropdownActions = listOf(
                DropdownMenuAction(
                    icon = Icons.Outlined.Settings,
                    text = stringResource(R.string.archive_settings),
                    onClick = {
                        navViewModel.addScreenInStack {
                        
                        }
                    }),
                DropdownMenuAction(
                    icon = Icons.Outlined.QuestionMark,
                    text = stringResource(R.string.how_does_it_work),
                    onClick = archiveInfoDialog::show
                )
            )
        )
    )
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.archive))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                ),
                actions = action
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            val user by UserManager.user.collectAsState()
            
            LazyColumn {
                items(
                    chatList,
                    key = { it.id }) { chat ->
                    
                    var chatInfo = chat
                    
                    if (chatInfo.id == user.id) {
                        chatInfo = chat.copy(chatName = stringResource(R.string.saved_messages))
                    }
                    
                    SwipeableChatCard(
                        chatInfo = chatInfo,
                        onClick = {
                            navViewModel.addScreenInStack {
                                ChatScreen(chatInfo.id)
                            }
                        },
                        backgroundIcon = Icons.Outlined.Unarchive,
                        onDismiss = {
                            mainViewModel.unarchiveChat(chatInfo.id)
                        })
                }
            }
        }
        
        if (archiveInfoDialog.isVisible) {
            BottomModal(
                onDismissRequest = archiveInfoDialog::hide,
                onConfirmRequest = archiveInfoDialog::hide
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomModal(
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Filled.Archive,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(40.dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            
            Button(
                onClick = onConfirmRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.ok).uppercase(),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
