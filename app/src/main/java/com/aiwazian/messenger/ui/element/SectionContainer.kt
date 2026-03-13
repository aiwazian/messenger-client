package com.aiwazian.messenger.ui.element

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.utils.Shape

@Composable
fun SectionContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .clip(Shape.Section),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        content()
    }
}
