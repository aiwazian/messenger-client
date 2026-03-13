package com.aiwazian.messenger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.viewModels.DataUsageViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SettingsDataAndStorageScreen() {
    Content()
}

@Composable
private fun Content() {
    val dataUsageViewModel = viewModel<DataUsageViewModel>()
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        dataUsageViewModel.reload(context)
    }
    
    val clearCacheDialog = dataUsageViewModel.clearCacheDialog
    
    val cacheSize = dataUsageViewModel.cacheSize
    val cacheMb = cacheSize / (1024.0 * 1024.0)
    val cacheMbRounded = BigDecimal(cacheMb).setScale(
        2,
        RoundingMode.HALF_UP
    ).toDouble()
    
    val sizeBytes = dataUsageViewModel.appSize
    val sizeMb = sizeBytes / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(
        2,
        RoundingMode.HALF_UP
    ).toDouble()
    
    Scaffold(
        topBar = { TopBar() }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Использование памяти",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W500
                )
                Text(text = "$sizeMbRounded MB")
            }
            
            SectionContainer {
                Column(Modifier.padding(10.dp)) {
                    Button(
                        onClick = clearCacheDialog::show,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.clear_cache) + " $cacheMbRounded MB",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 16.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        val scope = rememberCoroutineScope()
        
        if (clearCacheDialog.isVisible) {
            ClearCacheDialog(
                onConfirm = {
                    scope.launch {
                        dataUsageViewModel.clearAppData(context)
                        dataUsageViewModel.reload(context)
                        clearCacheDialog.hide()
                    }
                },
                onDismissRequest = clearCacheDialog::hide
            )
        }
    }
}

@Composable
private fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.clear_cache),
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = "Все медиа останутся в облаке, при необходимости Вы сможете заново загрузить их снова.",
                lineHeight = 18.sp
            )
        },
        buttons = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.clear_cache))
            }
        })
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}