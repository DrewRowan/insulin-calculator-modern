package com.example.insulincalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
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

@Composable
fun InsulinCalculatorScreen() {
    // State variables
    var currentBG by remember { mutableStateOf(6.0f) } // mmol/L
    var carbs by remember { mutableStateOf(0f) } // grams
    var targetBG by remember { mutableStateOf("6.0") }
    var isf by remember { mutableStateOf("2.8") }
    var icr by remember { mutableStateOf("10") }

    // Parse input fields safely
    val targetBGValue = targetBG.toFloatOrNull() ?: 0f
    val isfValue = isf.toFloatOrNull() ?: 1f
    val icrValue = icr.toFloatOrNull() ?: 1f

    // Calculation
    val insulinDose =
        if (isfValue > 0 && icrValue > 0) {
            ((currentBG - targetBGValue) / isfValue) + (carbs / icrValue)
        } else 0f

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

        // Target BG
        OutlinedTextField(
            value = targetBG,
            onValueChange = { targetBG = it },
            label = { Text("Target BG (mmol/L)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        // ISF
        OutlinedTextField(
            value = isf,
            onValueChange = { isf = it },
            label = { Text("ISF (mmol/L per unit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        // ICR
        OutlinedTextField(
            value = icr,
            onValueChange = { icr = it },
            label = { Text("ICR (g per unit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total Insulin Dose: ${if (insulinDose.isFinite()) String.format("%.2f", insulinDose) else "-"} units",
            fontSize = 22.sp,
            color = MaterialTheme.colors.primary
        )
    }
} 