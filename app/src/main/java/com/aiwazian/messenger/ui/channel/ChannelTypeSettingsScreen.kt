package com.aiwazian.messenger.ui.channel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionRadioItem
import com.aiwazian.messenger.utils.Shape
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.ChannelViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

private data class LinkMessage(
    var text: String,
    var color: Color
)

@Composable
fun ChannelTypeSettingsScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val channelViewModel = hiltViewModel<ChannelViewModel>()
    
    var channelType by remember { mutableStateOf(ChannelType.PRIVATE) }
    var publicLink by remember { mutableStateOf<String?>(null) }
    
    val vibrateService = VibrateService(LocalContext.current)
    
    LaunchedEffect(Unit) {
        channelType = ChannelType.fromInt(channelViewModel.channelInfo.value.channelType)
        publicLink = channelViewModel.channelInfo.value.publicLink.orEmpty()
    }
    
    val scrollState = rememberScrollState()
    
    val scope = rememberCoroutineScope()
    
    var canSave by remember { mutableStateOf(true) }
    
    val action = if (canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Outlined.Check,
                onClick = {
                    scope.launch {
                        if (channelType == ChannelType.PRIVATE) {
                            publicLink = null
                        }
                        
                        channelViewModel.changePublicLink(publicLink)
                        
                        val isChanged = channelViewModel.changeChannelType(channelType)
                        
                        if (!isChanged) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            return@launch
                        }
                        
                        val savedId = channelViewModel.trySaveOrCreate()
                        
                        if (savedId == null) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            return@launch
                        }
                        
                        navViewModel.removeLastScreenInStack()
                    }
                })
        )
    } else {
        emptyList()
    }
    
    Scaffold(
        topBar = {
            TopBar(actions = action)
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionHeader(title = stringResource(R.string.channel_type))
            SectionContainer {
                SectionRadioItem(
                    text = stringResource(R.string.private_channel),
                    selected = channelType == ChannelType.PRIVATE,
                    description = "На частные каналы можно подписаться только по ссылке-приглашению.",
                    onClick = {
                        canSave = true
                        channelType = ChannelType.PRIVATE
                    })
                SectionRadioItem(
                    text = stringResource(R.string.public_channel),
                    selected = channelType == ChannelType.PUBLIC,
                    description = "Публичные каналы можно найти через поиск, подписаться на них может любой пользователь.",
                    onClick = {
                        channelType = ChannelType.PUBLIC
                    })
            }
            
            AnimatedContent(
                targetState = channelType,
                transitionSpec = {
                    scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith scaleOut(tween(200)) + fadeOut(tween(200))
                }) { type ->
                if (type == ChannelType.PUBLIC) {
                    Column {
                        var linkError by remember { mutableStateOf<LinkMessage?>(null) }
                        
                        SectionHeader(title = stringResource(R.string.public_link))
                        
                        val errorColor = MaterialTheme.colorScheme.error
                        val successColor = MaterialTheme.colorScheme.primary
                        
                        SectionContainer {
                            InputField(
                                placeholder = "Ссылка",
                                value = publicLink.orEmpty(),
                                onValueChange = { newLink ->
                                    scope.launch {
                                        publicLink = newLink
                                        
                                        if (newLink.isBlank()) {
                                            canSave = false
                                            linkError = null
                                            return@launch
                                        }
                                        
                                        if (newLink == channelViewModel.channelInfo.value.publicLink) {
                                            canSave = true
                                            linkError = null
                                            return@launch
                                        }
                                        
                                        val isBusy = channelViewModel.checkIsBusyPublicLink(newLink)
                                        
                                        canSave = isBusy == false
                                        
                                        if (isBusy == null) {
                                            linkError = LinkMessage(
                                                text = "Не удалось проверить.",
                                                color = errorColor
                                            )
                                        } else if (isBusy) {
                                            linkError = LinkMessage(
                                                text = "Ссылка занята.",
                                                color = errorColor
                                            )
                                        } else {
                                            linkError = LinkMessage(
                                                text = "$publicLink доступен.",
                                                color = successColor
                                            )
                                        }
                                    }
                                })
                        }
                        
                        AnimatedContent(targetState = linkError) { text ->
                            if (text != null) {
                                Text(
                                    text = text.text,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 8.dp
                                    ),
                                    fontSize = 12.sp,
                                    color = text.color
                                )
                            }
                        }
                        
                        SectionDescription(text = "Если у канала будет постоянная публичная ссылка, другие пользователи смогут найти его и подписаться.")
                    }
                } else if (false) {
                    Column {
                        SectionHeader(title = stringResource(R.string.invite_link))
                        SectionContainer {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .combinedClickable(onClick = { })
                            ) {
                                var expand by remember { mutableStateOf(false) }
                                
                                Text(
                                    text = "me/ggkodps",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp)
                                )
                                
                                IconButton(onClick = { expand = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = null
                                    )
                                    
                                    DropdownMenu(
                                        expanded = expand,
                                        onDismissRequest = { expand = false },
                                        shape = Shape.DropdownMenu
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Получить QR-код") },
                                            onClick = { },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.QrCode,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                }
                            }
                            
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 8.dp,
                                        end = 8.dp,
                                        bottom = 8.dp
                                    ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Скопировать",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        SectionDescription(text = "По этой ссылке можно подписаться на канал. Вы можете сбросить её в любой момент.")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(actions: List<TopBarAction>) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = {
            Text(stringResource(R.string.channel_type))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        ),
        actions = actions
    )
}