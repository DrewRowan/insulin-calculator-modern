package com.example.insulincalculator

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.insulincalculator.data.InsulinRepositoryImpl
import com.example.insulincalculator.ui.*
import com.example.insulincalculator.ui.theme.InsulinCalculatorTheme
import kotlin.math.round
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.app.DatePickerDialog
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.AlertDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsulinCalculatorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val repository = remember(context) { InsulinRepositoryImpl(context) }

                    NavHost(navController = navController, startDestination = "calculator") {
                        composable("calculator") {
                            val viewModel: InsulinCalculatorViewModel = viewModel(
                                factory = InsulinCalculatorViewModelFactory(repository)
                            )
                            InsulinCalculatorScreen(navController, viewModel)
                        }
                        composable("history") {
                            val viewModel: HistoryViewModel = viewModel(
                                factory = HistoryViewModelFactory(repository)
                            )
                            HistoryScreen(navController, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun InsulinCalculatorScreen(
    navController: NavHostController,
    viewModel: InsulinCalculatorViewModel
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Insulin Calculator", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

        Text(
            text = "Total Insulin Dose: ${String.format("%.1f", state.finalInsulinDose)} units",
            fontSize = 22.sp,
            color = MaterialTheme.colors.primary
        )

        // Current BG Slider
        Text("Current Blood Glucose: ${String.format("%.1f", state.currentBG)} mmol/L")
        Slider(
            value = state.currentBG.toFloat(),
            onValueChange = { viewModel.updateCurrentBG((round(it * 10) / 10).toDouble()) },
            valueRange = 2.0f..25.0f,
            steps = ((25.0f - 2.0f) / 0.1f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Carbs Slider
        Text("Carbs in Food: ${state.carbs.toInt()} g")
        Slider(
            value = state.carbs.toFloat(),
            onValueChange = { viewModel.updateCarbs((round(it / 5) * 5).toDouble()) },
            valueRange = 0f..200f,
            steps = (200f / 5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Correction Dose Slider
        Text("Correction Dose: ${String.format("%.1f", state.correctionDose)}")
        Slider(
            value = state.correctionDose.toFloat(),
            onValueChange = { viewModel.updateCorrectionDose((round(it * 10) / 10).toDouble()) },
            valueRange = 0f..1f,
            steps = 10 - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Target BG Slider
        Text("Target BG: ${String.format("%.1f", state.targetBG)} mmol/L")
        Slider(
            value = state.targetBG.toFloat(),
            onValueChange = { viewModel.updateTargetBG((round(it * 2) / 2).toDouble()) },
            valueRange = 4.0f..8.0f,
            steps = ((8.0f - 4.0f) / 0.5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // ICR Slider
        Text("ICR: ${state.icr} g per unit")
        Slider(
            value = state.icr.toFloat(),
            onValueChange = { viewModel.updateICR((round(it / 5) * 5).toInt()) },
            valueRange = 5f..40f,
            steps = ((40f - 5f) / 5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.saveEntry() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Button(
            onClick = { navController.navigate("history") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View History")
        }
    }
}

@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var filterExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<com.example.insulincalculator.data.InsulinEntry?>(null) }
    var showBasalDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dailyTotals = state.entries.groupBy { dateFormat.format(it.timestamp) }
        .mapValues { (_, entries) -> entries.sumOf { it.finalInsulinDose } }
    val averageDailyDose = if (dailyTotals.isNotEmpty()) dailyTotals.values.average() else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            Text("History", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Button(onClick = { filterExpanded = !filterExpanded }) {
                    Text(if (filterExpanded) "Hide Filters" else "Show Filters")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showBasalDialog = true }) {
                    Text("Basal")
                }
            }
        }

        if (filterExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                // Date Picker Button
                Button(onClick = {
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(
                        context,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val newCalendar = Calendar.getInstance()
                            newCalendar.set(selectedYear, selectedMonth, selectedDay)
                            viewModel.updateFilter(state.filterState.copy(selectedDate = newCalendar.time))
                        },
                        year,
                        month,
                        day
                    ).show()
                }) {
                    Text(state.filterState.selectedDate?.let { 
                        "Filter Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)}" 
                    } ?: "Select Date Filter")
                }

                // Add other filter fields here...
                // (BG, Carbs, Correction, Target, ICR, Dose ranges)

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.updatePage(0) }) {
                    Text("Apply Filters")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Column Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                "timestamp" to "Time",
                "currentBG" to "BG",
                "carbs" to "Carbs",
                "correctionDose" to "Corr.",
                "targetBG" to "Target",
                "icr" to "ICR",
                "finalInsulinDose" to "Dose"
            ).forEach { (col, label) ->
                Text(
                    label,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateSort(col) },
                    fontSize = 16.sp,
                    color = if (state.sortColumn == col) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
            }
        }

        Divider()

        // Table Content
        Column(modifier = Modifier.weight(1f)) {
            val pageEntries = state.entries
                .drop(state.currentPage * state.pageSize)
                .take(state.pageSize)

            pageEntries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(entry) {
                            detectTapGestures(
                                onLongPress = {
                                    entryToDelete = entry
                                    showDeleteDialog = true
                                }
                            )
                        }
                ) {
                    listOf(
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(entry.timestamp),
                        String.format("%.1f", entry.currentBG),
                        String.format("%.0f", entry.carbs),
                        String.format("%.1f", entry.correctionDose),
                        String.format("%.1f", entry.targetBG),
                        entry.icr.toString(),
                        String.format("%.1f", entry.finalInsulinDose)
                    ).forEach { value ->
                        Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    }
                }
                Divider()
            }
        }

        // Pagination Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.updatePage(state.currentPage - 1) },
                enabled = state.currentPage > 0
            ) { Text("Previous") }

            Text("Page ${state.currentPage + 1} of ${if (state.entries.isEmpty()) 1 else (state.entries.size + state.pageSize - 1) / state.pageSize}")

            Button(
                onClick = { viewModel.updatePage(state.currentPage + 1) },
                enabled = state.currentPage < (state.entries.size + state.pageSize - 1) / state.pageSize - 1
            ) { Text("Next") }
        }
        if (showDeleteDialog && entryToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Entry") },
                text = { Text("Are you sure you want to delete this entry?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteEntry(entryToDelete!!)
                        showDeleteDialog = false
                    }) { Text("Delete") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
        if (showBasalDialog) {
            AlertDialog(
                onDismissRequest = { showBasalDialog = false },
                title = { Text("Estimated Bolus") },
                text = { Text("Estimated Bolus: ${String.format("%.2f", averageDailyDose)} units") },
                confirmButton = {
                    Button(onClick = { showBasalDialog = false }) { Text("OK") }
                }
            )
        }
    }
} 