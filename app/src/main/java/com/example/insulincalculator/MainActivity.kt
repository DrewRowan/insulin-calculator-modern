package com.example.insulincalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insulincalculator.ui.theme.InsulinCalculatorTheme
import kotlin.math.round

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsulinCalculatorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    InsulinCalculatorScreen()
                }
            }
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun InsulinCalculatorScreen() {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Insulin Calculator", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

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

        // Target BG Dropdown
        ExposedDropdownMenuBox(
            expanded = targetBGExpanded,
            onExpandedChange = { targetBGExpanded = !targetBGExpanded }
        ) {
            OutlinedTextField(
                value = String.format("%.1f", targetBG),
                onValueChange = {},
                readOnly = true,
                label = { Text("Target BG (mmol/L)") },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = targetBGExpanded,
                onDismissRequest = { targetBGExpanded = false }
            ) {
                targetBGOptions.forEach { option ->
                    DropdownMenuItem(onClick = {
                        targetBG = option
                        targetBGExpanded = false
                    }) {
                        Text(String.format("%.1f", option))
                    }
                }
            }
        }

        // ICR Dropdown
        ExposedDropdownMenuBox(
            expanded = icrExpanded,
            onExpandedChange = { icrExpanded = !icrExpanded }
        ) {
            OutlinedTextField(
                value = icr.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("ICR (g per unit)") },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = icrExpanded,
                onDismissRequest = { icrExpanded = false }
            ) {
                icrOptions.forEach { option ->
                    DropdownMenuItem(onClick = {
                        icr = option
                        icrExpanded = false
                    }) {
                        Text(option.toString())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total Insulin Dose: ${if (finalInsulinDose.isFinite()) String.format("%.1f", finalInsulinDose) else "-"} units",
            fontSize = 22.sp,
            color = MaterialTheme.colors.primary
        )
    }
} 