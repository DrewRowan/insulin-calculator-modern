package com.example.insulincalculator

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.insulincalculator.data.InsulinRepositoryImpl
import com.example.insulincalculator.data.LibreLinkUpRepository
import com.example.insulincalculator.ui.*
import com.example.insulincalculator.ui.theme.InsulinCalculatorTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.round

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsulinCalculatorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val context = LocalContext.current
                    val repository = remember(context) { InsulinRepositoryImpl(context) }
                    val libreRepository = remember(context) { LibreLinkUpRepository(context) }
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Scaffold(
                        bottomBar = {
                            BottomNavigation {
                                BottomNavigationItem(
                                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                    label = { Text("Calculator") },
                                    selected = currentRoute == "calculator",
                                    onClick = {
                                        navController.navigate("calculator") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                BottomNavigationItem(
                                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                                    label = { Text("History") },
                                    selected = currentRoute == "history",
                                    onClick = {
                                        navController.navigate("history") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                BottomNavigationItem(
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                    label = { Text("Settings") },
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "calculator",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("calculator") {
                                val vm: InsulinCalculatorViewModel = viewModel(
                                    factory = InsulinCalculatorViewModelFactory(repository, libreRepository)
                                )
                                InsulinCalculatorScreen(vm)
                            }
                            composable("history") {
                                val vm: HistoryViewModel = viewModel(
                                    factory = HistoryViewModelFactory(repository)
                                )
                                HistoryScreen(vm)
                            }
                            composable("settings") {
                                val vm: SettingsViewModel = viewModel(
                                    factory = SettingsViewModelFactory(libreRepository)
                                )
                                SettingsScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsulinCalculatorScreen(viewModel: InsulinCalculatorViewModel) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.autoFetchGlucoseIfLinked()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Insulin Calculator", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

        Text(
            text = "Total Insulin Dose: ${String.format("%.1f", state.finalInsulinDose)} units",
            fontSize = 22.sp,
            color = MaterialTheme.colors.primary
        )

        // Current BG with LibreLinkUp fetch button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Current Blood Glucose: ${String.format("%.1f", state.currentBG)} mmol/L")
            if (state.isLoadingGlucose) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = { viewModel.fetchGlucoseFromLibre() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Fetch from LibreLink",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }
        if (state.glucoseError != null) {
            Text(
                text = state.glucoseError!!,
                color = MaterialTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Slider(
            value = state.currentBG.toFloat(),
            onValueChange = { viewModel.updateCurrentBG((round(it * 10) / 10).toDouble()) },
            valueRange = 2.0f..25.0f,
            steps = ((25.0f - 2.0f) / 0.1f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Carbs in Food: ${state.carbs.toInt()} g")
        Slider(
            value = state.carbs.toFloat(),
            onValueChange = { viewModel.updateCarbs((round(it / 5) * 5).toDouble()) },
            valueRange = 0f..200f,
            steps = (200f / 5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Correction Dose: ${String.format("%.1f", state.correctionDose)}")
        Slider(
            value = state.correctionDose.toFloat(),
            onValueChange = { viewModel.updateCorrectionDose((round(it * 10) / 10).toDouble()) },
            valueRange = 0f..1f,
            steps = 10 - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Target BG: ${String.format("%.1f", state.targetBG)} mmol/L")
        Slider(
            value = state.targetBG.toFloat(),
            onValueChange = { viewModel.updateTargetBG((round(it * 2) / 2).toDouble()) },
            valueRange = 4.0f..8.0f,
            steps = ((8.0f - 4.0f) / 0.5f).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.saveEntry() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
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
        Text("History", fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filterExpanded = !filterExpanded }) {
                Text(if (filterExpanded) "Hide Filters" else "Show Filters")
            }
            Button(onClick = { showBasalDialog = true }) {
                Text("Basal")
            }
        }

        if (filterExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val newCalendar = Calendar.getInstance()
                            newCalendar.set(year, month, day)
                            viewModel.updateFilter(state.filterState.copy(selectedDate = newCalendar.time))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text(state.filterState.selectedDate?.let {
                        "Filter Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)}"
                    } ?: "Select Date Filter")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.updatePage(0) }) {
                    Text("Apply Filters")
                }
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
                        .clickable { viewModel.updateSort(col) },
                    fontSize = 16.sp,
                    color = if (state.sortColumn == col) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
            }
        }

        Divider()

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
                        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(entry.timestamp),
                        String.format("%.1f", entry.currentBG),
                        String.format("%.0f", entry.carbs),
                        String.format("%.1f", entry.correctionDose),
                        String.format("%.1f", entry.targetBG),
                        entry.icr.toString(),
                        String.format("%.1f", entry.finalInsulinDose)
                    ).forEach { value ->
                        Text(value, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    }
                }
                Divider()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.updatePage(state.currentPage - 1) },
                enabled = state.currentPage > 0
            ) { Text("Prev") }

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
                title = { Text("Estimated Basal") },
                text = { Text("Estimated Basal: ${String.format("%.2f", averageDailyDose)} units/day") },
                confirmButton = {
                    Button(onClick = { showBasalDialog = false }) { Text("OK") }
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    val regions = listOf(
        "EU" to "Europe (EU)",
        "US" to "United States (US)",
        "AU" to "Australia (AU)",
        "CA" to "Canada (CA)",
        "DE" to "Germany (DE)",
        "JP" to "Japan (JP)"
    )
    var regionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

        Text("LibreLinkUp Account", fontSize = 20.sp)
        Divider()

        if (state.isLinked) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected", color = MaterialTheme.colors.primary, fontSize = 16.sp)
                    Text(state.linkedEmail ?: "")
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.unlink() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlink Account", color = MaterialTheme.colors.onError)
                    }
                }
            }
        } else {
            if (state.expiredEmail != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    elevation = 0.dp
                ) {
                    Text(
                        "Session expired for ${state.expiredEmail}. Please re-link your account.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colors.error,
                        fontSize = 13.sp
                    )
                }
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            // Region dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = regions.find { it.first == state.region }?.second ?: state.region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Region") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                // Transparent overlay to capture clicks on the read-only field
                Box(modifier = Modifier
                    .matchParentSize()
                    .clickable { regionExpanded = true }
                )
                DropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    regions.forEach { (key, label) ->
                        DropdownMenuItem(onClick = {
                            viewModel.updateRegion(key)
                            regionExpanded = false
                        }) {
                            Text(label)
                        }
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colors.error,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = { viewModel.linkAccount() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.onPrimary
                    )
                } else {
                    Text("Link Account")
                }
            }
        }

        if (state.successMessage != null) {
            Text(
                text = state.successMessage!!,
                color = MaterialTheme.colors.primary,
                fontSize = 14.sp
            )
        }
    }
}
