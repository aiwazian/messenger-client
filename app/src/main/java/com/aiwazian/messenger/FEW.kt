package com.aiwazian.messenger

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Preview
@Composable
fun ds() {
    FEW()
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FEW(
    initialColor: Color = Color.Red,
    onDismiss: () -> Unit = {},
    onColorSelected: (Color) -> Unit = {}
) {
    // Внутреннее состояние в HSV
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    
    // Текущий выбранный цвет
    val currentColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation, value)
    }
    Scaffold {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Выберите цвет",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Основная область: Квадрат Sat/Val + Слайдер Hue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Квадрат выбора Saturation & Value (слева)
                        SaturationValueBox(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onSatValChanged = { newSat, newVal ->
                                saturation = newSat
                                value = newVal
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 2. Вертикальная полоса выбора Hue (справа)
                        HueSlider(
                            hue = hue,
                            onHueChanged = { newHue -> hue = newHue },
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Предпросмотр выбранного цвета и кнопки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(currentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "#${currentColor.toHex()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Row {
                            TextButton(onClick = onDismiss) {
                                Text("Отмена")
                            }
                            Button(onClick = {
                                onColorSelected(currentColor)
                                onDismiss()
                            }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChanged: (sat: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            // Белый -> Чистый цвет по горизонтали
            .background(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
            // Прозрачный -> Чёрный по вертикали
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                    val newVal = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatValChanged(newSat, newVal)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                        val newVal = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onSatValChanged(newSat, newVal)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newSat = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newVal = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onSatValChanged(newSat, newVal)
                    }
                )
            }
    ) {
        // Указатель (кружок) выбора
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pointX = saturation * size.width
            val pointY = (1f - value) * size.height
            
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(pointX, pointY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = Offset(pointX, pointY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val rainbowColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green,
            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(rainbowColors))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newHue = (offset.y / size.height).coerceIn(0f, 1f) * 360f
                    onHueChanged(newHue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newHue = (offset.y / size.height).coerceIn(0f, 1f) * 360f
                        onHueChanged(newHue)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newHue = (change.position.y / size.height).coerceIn(0f, 1f) * 360f
                        onHueChanged(newHue)
                    }
                )
            }
    ) {
        // Линия-указатель выбранного тона
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = (hue / 360f) * size.height
            
            drawLine(
                color = Color.White,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 4.dp.toPx()
            )
            drawLine(
                color = Color.Black,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

// Вспомогательная функция для форматирования в HEX
private fun Color.toHex(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("%02X%02X%02X", red, green, blue)
}