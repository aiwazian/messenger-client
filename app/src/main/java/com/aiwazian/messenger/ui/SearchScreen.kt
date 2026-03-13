package com.aiwazian.messenger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SearchViewModel

@Composable
fun SearchScreen() {
    val navViewModel = viewModel<NavigationViewModel>()
    val searchViewModel = viewModel<SearchViewModel>()
    
    val searchResults by searchViewModel.searchResults.collectAsState()
    val query by searchViewModel.query.collectAsState()
    
    Scaffold(
        topBar = {
            TopBar(
                query,
                searchViewModel::onQueryChange
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (searchResults.isEmpty()) {
                if (query.isBlank()) {
                    return@Column
                }
                
                Column(
                    modifier = Modifier
                        .imePadding()
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val composition by rememberLottieComposition(
                        spec = LottieCompositionSpec.Asset(LottieAnimation.SEARCH_OUT)
                    )
                    
                    LottieAnimation(
                        composition = composition,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 10.dp),
                        iterations = LottieConstants.IterateForever,
                        isPlaying = true
                    )
                    
                    Text(
                        text = "Нет результатов",
                        textAlign = TextAlign.Center,
                    )
                }
                
                return@Column
            }
            
            LazyColumn {
                items(searchResults) { search ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        onClick = {
                            navViewModel.addScreenInStack {
                                ChatScreen(search.chatId)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = search.name,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            
                            val username = search.publicLink
                            
                            val startIndex = username.indexOf(query)
                            val endIndex = startIndex + query.length
                            
                            Text(
                                text = buildAnnotatedString {
                                    if (startIndex in username.indices) {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append("@")
                                        }
                                        
                                        append(
                                            username.substring(
                                                0,
                                                startIndex
                                            )
                                        )
                                        
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append(
                                                username.substring(
                                                    startIndex,
                                                    endIndex
                                                )
                                            )
                                        }
                                        
                                        append(username.substring(endIndex))
                                    } else {
                                        append("@$username")
                                    }
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = {
            SearchTextField(
                value,
                onValueChange,
            )
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}

@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search),
            )
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        trailingIcon = {
            AnimatedVisibility(
                visible = value.isNotBlank(),
                enter = scaleIn(tween(100)) + fadeIn(tween(100)),
                exit = scaleOut(tween(100)) + fadeOut(tween(100))
            ) {
                IconButton(
                    onClick = {
                        onValueChange("")
                    }) {
                    Icon(
                        Icons.Default.Close,
                        null,
                    )
                }
            }
        })
}