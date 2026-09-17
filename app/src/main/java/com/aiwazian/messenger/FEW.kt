package com.aiwazian.messenger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem

@Preview(showBackground = true)
@Composable
fun FEW() {
    MaterialTheme {
        Scaffold {
            Column(
                Modifier
                    .padding(it)
                    .padding(10.dp)
            ) {
                var expanded by remember { mutableStateOf(true) }
                var exp by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.Apps, contentDescription = null)
                    }
                    
                    AnimatedPopup(expanded = expanded, onDismissRequest = {
                        if (exp) {
                            exp = false
                        } else {
                            expanded = false
                        }
                    }) {
                        Box(contentAlignment = Alignment.TopCenter) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 40.dp)
                                    .width(IntrinsicSize.Max),
                            ) {
                                AnimatedVisibility(visible = !exp) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        DropdownMenuGroup(
                                            shapes = MenuGroupShapes(
                                                MaterialTheme.shapes.medium,
                                                MaterialTheme.shapes.medium
                                            ),
                                            contentPadding = PaddingValues.Zero,
                                            modifier = Modifier.width(IntrinsicSize.Max)
                                        ) {
                                            AppDropdownMenuItem("few", onClick = {})
                                            AppDropdownMenuItem("few", onClick = {})
                                            AppDropdownMenuItem("few", onClick = {})
                                        }
                                        DropdownMenuGroup(
                                            shapes = MenuGroupShapes(
                                                MaterialTheme.shapes.medium,
                                                MaterialTheme.shapes.medium
                                            ),
                                            contentPadding = PaddingValues.Zero,
                                            modifier = Modifier.width(IntrinsicSize.Max)
                                        ) {
                                            Text(" fhjew fj", modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                            
                            var defaultHeight by remember { mutableStateOf(24.dp) }
                            
                            DropdownMenuGroup(
                                shapes = MenuGroupShapes(
                                    MaterialTheme.shapes.medium,
                                    MaterialTheme.shapes.medium
                                ),
                                contentPadding = PaddingValues.Zero,
                            ) {
                                AnimatedContent(exp) { exp ->
                                    if (exp) {
                                        LazyRow(
                                            modifier = Modifier.widthIn(max = 200.dp).background(Color.DarkGray)
                                        ) {
                                            items(10) {
                                                Text("O")
                                            }
                                        }
                                    }
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(6),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = defaultHeight)
                                        .widthIn(max = 200.dp)
                                        .heightIn(max = 200.dp)
                                        .animateContentSize()
                                ) {
                                    items(5) { num ->
                                        Text(
                                            num.toString(),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                    
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .animateItem()
                                        ) {
                                            Text(
                                                "5",
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .animateItem()
                                                    .fillMaxSize()
                                            )
                                            if (!exp) {
                                                Text(
                                                    "^",
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .animateItem()
                                                        .fillMaxSize()
                                                        .background(Color.Gray)
                                                        .clickable {
                                                            exp = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (exp) {
                                        items(50) { num ->
                                            Text(
                                                num.toString(),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .animateItem()
                                                    .size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    
    visibleState.targetState = expanded
    
    if (visibleState.currentState || visibleState.targetState) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(200)
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(150)
                )
            ) {
                content()
            }
        }
    }
}