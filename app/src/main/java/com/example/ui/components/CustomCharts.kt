package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogEntry

@Composable
fun DistanceBarChart(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    // Group logs by vehicle or show last 7 trips
    val lastSeven = logs.take(7).reversed()
    if (lastSeven.isEmpty()) {
        EmptyChartState(message = "Add trip entries to view distance trends.")
        return
    }

    val maxVal = lastSeven.maxOf { it.distance }.coerceAtLeast(10.0)
    val tealColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = 24f
            val availableWidth = width - (spacing * 2)
            val barCount = lastSeven.size
            val barWidth = (availableWidth / barCount) * 0.6f
            val gapWidth = (availableWidth / barCount) * 0.4f

            // Draw Y Grid lines
            for (i in 0..3) {
                val gridY = height * (i / 3f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }

            // Draw Bars
            lastSeven.forEachIndexed { index, log ->
                val barHeight = (log.distance / maxVal) * (height * 0.85f)
                val x = spacing + index * (barWidth + gapWidth)
                val y = height - barHeight.toFloat()

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor, tealColor)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.toFloat())
                )
            }
        }
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            lastSeven.forEach { log ->
                Text(
                    text = "${String.format("%.0f", log.distance)}km",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FuelConsumptionLineChart(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val fuelEntries = logs.filter { it.dieselFilled > 0 }.take(7).reversed()
    if (fuelEntries.isEmpty()) {
        EmptyChartState(message = "Add diesel fill-up details to generate fuel trend.")
        return
    }

    val maxFuel = fuelEntries.maxOf { it.dieselFilled }.coerceAtLeast(5.0)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = 32f
            val stepX = (width - spacing * 2) / (fuelEntries.size - 1).coerceAtLeast(1)

            // Draw Grid
            for (i in 0..3) {
                val gridY = height * (i / 3f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }

            // Draw line
            val path = Path()
            val points = fuelEntries.mapIndexed { index, entry ->
                val x = spacing + index * stepX
                val fuelPct = (entry.dieselFilled / maxFuel).toFloat()
                val y = height - (fuelPct * height * 0.75f) - 10f
                Offset(x, y)
            }

            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = tertiaryColor,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw dots
            points.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            fuelEntries.forEach { entry ->
                Text(
                    text = "${String.format("%.1f", entry.dieselFilled)}L",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MileageTrendChart(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val logsWithMileage = logs.filter { it.mileage > 0 }.take(7).reversed()
    if (logsWithMileage.isEmpty()) {
        EmptyChartState(message = "Refuel multiple times to plot your vehicle mileage trend.")
        return
    }

    val maxMileage = logsWithMileage.maxOf { it.mileage }.coerceAtLeast(5.0)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = 32f
            val stepX = (width - spacing * 2) / (logsWithMileage.size - 1).coerceAtLeast(1)

            // Draw Grid lines
            for (i in 0..3) {
                val gridY = height * (i / 3f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            val points = logsWithMileage.mapIndexed { index, entry ->
                val x = spacing + index * stepX
                val milPct = (entry.mileage / maxMileage).toFloat()
                val y = height - (milPct * height * 0.75f) - 10f
                Offset(x, y)
            }

            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )

            // Fill area under line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw small circles
            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            logsWithMileage.forEach { entry ->
                Text(
                    text = "${String.format("%.1f", entry.mileage)} km/L",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MonthlyFuelCostChart(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    // Extract last 6 calendar months costs
    val calendar = java.util.Calendar.getInstance()
    val monthlyTotals = DoubleArray(6) { 0.0 }
    val monthNames = Array(6) { "" }

    val sdf = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())

    for (i in 0..5) {
        val cal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MONTH, -i)
        }
        monthNames[5 - i] = sdf.format(cal.time)

        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH)

        monthlyTotals[5 - i] = logs.filter { log ->
            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.date }
            logCal.get(java.util.Calendar.YEAR) == year && logCal.get(java.util.Calendar.MONTH) == month
        }.sumOf { it.dieselCost }
    }

    val maxVal = monthlyTotals.maxOrNull()?.coerceAtLeast(10.0) ?: 10.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val barColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = 24f
            val availableWidth = width - (spacing * 2)
            val barCount = 6
            val barWidth = (availableWidth / barCount) * 0.5f
            val gapWidth = (availableWidth / barCount) * 0.5f

            // Draw Y Grid lines
            for (i in 0..3) {
                val gridY = height * (i / 3f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }

            // Draw Bars
            for (i in 0 until barCount) {
                val costVal = monthlyTotals[i]
                val barHeight = (costVal / maxVal) * (height * 0.8f)
                val x = spacing + i * (barWidth + gapWidth)
                val y = height - barHeight.toFloat()

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor, primaryColor)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.toFloat())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthNames[i],
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$${String.format("%.0f", monthlyTotals[i])}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}
