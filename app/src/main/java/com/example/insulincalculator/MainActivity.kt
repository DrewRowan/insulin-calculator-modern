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
import com.example.insulincalculator.ui.theme.InsulinCalculatorTheme
import kotlin.math.round
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.app.DatePickerDialog
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsulinCalculatorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "calculator") {
                        composable("calculator") { InsulinCalculatorScreen(navController) }
                        composable("history") { HistoryScreen(navController) }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun InsulinCalculatorScreen(navController: NavHostController) {
    // State variables
    var currentBG by remember { mutableStateOf(6.0f) } // mmol/L
    var carbs by remember { mutableStateOf(0f) } // grams

    // Dropdown state for Target BG
    val targetBGOptions = (8..16).map { it / 2f } // 4.0, 4.5, ..., 8.0
    var targetBGExpanded by remember { mutableStateOf(false) }
    var targetBG by remember { mutableStateOf(5.5f) }

    // Dropdown state for ISF
    val isfOptions = (1..8).map { it * 5 } // 5, 10, ..., 40
    var isfExpanded by remember { mutableStateOf(false) }

    // Dropdown state for ICR
    val icrOptions = (1..8).map { it * 5 } // 5, 10, ..., 40
    var icrExpanded by remember { mutableStateOf(false) }
    var icr by remember { mutableStateOf(10) }

    // Correction Dose state
    var correctionDose by remember { mutableStateOf(0.5f) }

    // Calculation
    val insulinDoseRaw =
        if (icr > 0) {
            correctionDose * (carbs / icr)
        } else 0f
    // adjust for range e.g. 5-7 = 0 correction, 7-9 = 1, etc
    var rangeCorrection = (currentBG - targetBG) / 2
    if (rangeCorrection < 0) {
        rangeCorrection = 0.0f
    }
    val finalInsulinDose = insulinDoseRaw + rangeCorrection

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Insulin Calculator", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

        Text(
            text = "Total Insulin Dose: ${if (finalInsulinDose.isFinite()) String.format("%.1f", finalInsulinDose) else "-"} units",
            fontSize = 22.sp,
            color = MaterialTheme.colors.primary
        )

        // Current BG Slider
        Text("Current Blood Glucose: ${String.format("%.1f", currentBG)} mmol/L")
        Slider(
            value = currentBG,
            onValueChange = { currentBG = round(it * 10) / 10 },
            valueRange = 2.0f..25.0f,
            steps = ((25.0f - 2.0f) / 0.1f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Carbs Slider
        Text("Carbs in Food: ${carbs.toInt()} g")
        Slider(
            value = carbs,
            onValueChange = { carbs = round(it / 5) * 5 },
            valueRange = 0f..200f,
            steps = (200f / 5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Correction Dose Slider
        Text("Correction Dose: ${String.format("%.1f", correctionDose)}")
        Slider(
            value = correctionDose,
            onValueChange = { correctionDose = round(it * 10) / 10 },
            valueRange = 0f..1f,
            steps = 10 - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // Target BG Slider
        Text("Target BG: ${String.format("%.1f", targetBG)} mmol/L")
        Slider(
            value = targetBG,
            onValueChange = { targetBG = (round(it * 2) / 2) },
            valueRange = 4.0f..8.0f,
            steps = ((8.0f - 4.0f) / 0.5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        // ICR Slider
        Text("ICR: ${icr.toInt()} g per unit")
        Slider(
            value = icr.toFloat(),
            onValueChange = { icr = (round(it / 5) * 5).toInt() },
            valueRange = 5f..40f,
            steps = ((40f - 5f) / 5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val sharedPref = context.getSharedPreferences("insulin_data", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                // Save as a JSON object
                val entry = JSONObject()
                entry.put("timestamp", timestamp)
                entry.put("currentBG", currentBG)
                entry.put("carbs", carbs)
                entry.put("correctionDose", correctionDose)
                entry.put("targetBG", targetBG)
                entry.put("icr", icr)
                entry.put("finalInsulinDose", finalInsulinDose)
                // Get existing history
                val historyStr = sharedPref.getString("history", "[]")
                val historyArr = JSONArray(historyStr)
                historyArr.put(entry)
                editor.putString("history", historyArr.toString())
                editor.apply()
                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
            },
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
fun HistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    var sortColumn by remember { mutableStateOf("timestamp") }
    var ascending by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    // Filter states
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var filterBGMin by remember { mutableStateOf("") }
    var filterBGMax by remember { mutableStateOf("") }
    var filterCarbsMin by remember { mutableStateOf("") }
    var filterCarbsMax by remember { mutableStateOf("") }
    var filterCorrMin by remember { mutableStateOf("") }
    var filterCorrMax by remember { mutableStateOf("") }
    var filterTargetMin by remember { mutableStateOf("") }
    var filterTargetMax by remember { mutableStateOf("") }
    var filterICRMin by remember { mutableStateOf("") }
    var filterICRMax by remember { mutableStateOf("") }
    var filterDoseMin by remember { mutableStateOf("") }
    var filterDoseMax by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10

    val sharedPref = context.getSharedPreferences("insulin_data", Context.MODE_PRIVATE)
    val historyStr = sharedPref.getString("history", "[]") ?: "[]"
    val historyArr = JSONArray(historyStr)
    val entries = remember(historyStr) {
        List(historyArr.length()) { i ->
            val obj = historyArr.getJSONObject(i)
            mapOf(
                "timestamp" to obj.getString("timestamp"),
                "currentBG" to obj.getDouble("currentBG"),
                "carbs" to obj.getDouble("carbs"),
                "correctionDose" to obj.getDouble("correctionDose"),
                "targetBG" to obj.getDouble("targetBG"),
                "icr" to obj.getInt("icr"),
                "finalInsulinDose" to obj.getDouble("finalInsulinDose")
            )
        }
    }
    // Filtering
    val filteredEntries = entries.filter { entry ->
        val entryDate = try { dateFormat.parse(entry["timestamp"] as String) } catch (e: Exception) { null }
        (selectedDate == null || (entryDate != null && selectedDate?.time != null && dateFormat.format(entryDate) == dateFormat.format(selectedDate!!.time))) &&
        (filterBGMin.isBlank() || (entry["currentBG"] as Double) >= filterBGMin.toDoubleOrNull() ?: Double.MIN_VALUE) &&
        (filterBGMax.isBlank() || (entry["currentBG"] as Double) <= filterBGMax.toDoubleOrNull() ?: Double.MAX_VALUE) &&
        (filterCarbsMin.isBlank() || (entry["carbs"] as Double) >= filterCarbsMin.toDoubleOrNull() ?: Double.MIN_VALUE) &&
        (filterCarbsMax.isBlank() || (entry["carbs"] as Double) <= filterCarbsMax.toDoubleOrNull() ?: Double.MAX_VALUE) &&
        (filterCorrMin.isBlank() || (entry["correctionDose"] as Double) >= filterCorrMin.toDoubleOrNull() ?: Double.MIN_VALUE) &&
        (filterCorrMax.isBlank() || (entry["correctionDose"] as Double) <= filterCorrMax.toDoubleOrNull() ?: Double.MAX_VALUE) &&
        (filterTargetMin.isBlank() || (entry["targetBG"] as Double) >= filterTargetMin.toDoubleOrNull() ?: Double.MIN_VALUE) &&
        (filterTargetMax.isBlank() || (entry["targetBG"] as Double) <= filterTargetMax.toDoubleOrNull() ?: Double.MAX_VALUE) &&
        (filterICRMin.isBlank() || (entry["icr"] as Int) >= filterICRMin.toIntOrNull() ?: Int.MIN_VALUE) &&
        (filterICRMax.isBlank() || (entry["icr"] as Int) <= filterICRMax.toIntOrNull() ?: Int.MAX_VALUE) &&
        (filterDoseMin.isBlank() || (entry["finalInsulinDose"] as Double) >= filterDoseMin.toDoubleOrNull() ?: Double.MIN_VALUE) &&
        (filterDoseMax.isBlank() || (entry["finalInsulinDose"] as Double) <= filterDoseMax.toDoubleOrNull() ?: Double.MAX_VALUE)
    }
    val sortedEntries = remember(sortColumn, ascending, filteredEntries) {
        filteredEntries.sortedWith(compareBy<Map<String, Any>> {
            when (sortColumn) {
                "timestamp" -> it["timestamp"] as String
                "currentBG" -> it["currentBG"] as Double
                "carbs" -> it["carbs"] as Double
                "correctionDose" -> it["correctionDose"] as Double
                "targetBG" -> it["targetBG"] as Double
                "icr" -> it["icr"] as Int
                "finalInsulinDose" -> it["finalInsulinDose"] as Double
                else -> it["timestamp"] as String
            }
        })
            .let { if (ascending) it else it.reversed() }
    }
    val pageCount = (sortedEntries.size + pageSize - 1) / pageSize
    val pageEntries = sortedEntries.drop(currentPage * pageSize).take(pageSize)

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
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { filterExpanded = !filterExpanded }) {
                Text(if (filterExpanded) "Hide Filters" else "Show Filters")
            }
        }
        if (filterExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                // Date Picker Button
                Button(onClick = { /* Show DatePickerDialog */
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(context,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val newCalendar = Calendar.getInstance()
                            newCalendar.set(selectedYear, selectedMonth, selectedDay)
                            selectedDate = newCalendar
                        }, year, month, day).show()
                }) {
                    Text(selectedDate?.let { "Filter Date: ${dateFormat.format(it.time)}" } ?: "Select Date Filter")
                }

                OutlinedTextField(
                    value = filterBGMin,
                    onValueChange = { filterBGMin = it },
                    label = { Text("BG min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterBGMax,
                    onValueChange = { filterBGMax = it },
                    label = { Text("BG max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterCarbsMin,
                    onValueChange = { filterCarbsMin = it },
                    label = { Text("Carbs min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterCarbsMax,
                    onValueChange = { filterCarbsMax = it },
                    label = { Text("Carbs max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterCorrMin,
                    onValueChange = { filterCorrMin = it },
                    label = { Text("Correction min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterCorrMax,
                    onValueChange = { filterCorrMax = it },
                    label = { Text("Correction max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterTargetMin,
                    onValueChange = { filterTargetMin = it },
                    label = { Text("Target min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterTargetMax,
                    onValueChange = { filterTargetMax = it },
                    label = { Text("Target max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterICRMin,
                    onValueChange = { filterICRMin = it },
                    label = { Text("ICR min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterICRMax,
                    onValueChange = { filterICRMax = it },
                    label = { Text("ICR max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterDoseMin,
                    onValueChange = { filterDoseMin = it },
                    label = { Text("Dose min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filterDoseMax,
                    onValueChange = { filterDoseMax = it },
                    label = { Text("Dose max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { currentPage = 0 }) { Text("Apply Filters") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
                        .clickable {
                            if (sortColumn == col) ascending = !ascending else sortColumn = col
                        },
                    fontSize = 16.sp,
                    color = if (sortColumn == col) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
            }
        }
        Divider()
        Column(modifier = Modifier.weight(1f)) {
            pageEntries.forEach { entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        entry["timestamp"].toString(),
                        String.format("%.1f", entry["currentBG"] as Double),
                        String.format("%.0f", entry["carbs"] as Double),
                        String.format("%.1f", entry["correctionDose"] as Double),
                        String.format("%.1f", entry["targetBG"] as Double),
                        (entry["icr"] as Int).toString(),
                        String.format("%.1f", entry["finalInsulinDose"] as Double)
                    ).forEach { value ->
                        Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    }
                }
                Divider()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { if (currentPage > 0) currentPage-- },
                enabled = currentPage > 0
            ) { Text("Previous") }
            Text("Page ${currentPage + 1} of ${if (pageCount == 0) 1 else pageCount}")
            Button(
                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                enabled = currentPage < pageCount - 1
            ) { Text("Next") }
        }
    }
} 