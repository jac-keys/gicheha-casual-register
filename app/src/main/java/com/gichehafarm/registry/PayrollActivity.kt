package com.gichehafarm.registry

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gichehafarm.registry.data.DatabaseHelper
import com.gichehafarm.registry.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import java.util.*
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
class PayrollActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PayrollContent()
        }
    }
}

@Composable
fun PayrollContent() {
    var dailyWage by remember { mutableStateOf("") }
    var payrollList by remember { mutableStateOf(emptyList<WorkerPayroll>()) }
    var wageError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("sw", "KE")) }
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }

    // Processed list with sorting and filtering
    val processedList = remember(payrollList, searchQuery, sortOption) {
        payrollList
            .asSequence()
            .filter { worker ->
                worker.name.contains(searchQuery, ignoreCase = true) ||
                        worker.workNumber.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(sortOption.comparator)
            .toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Payroll Management",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Company Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dailyWage,
                onValueChange = { value ->
                    dailyWage = value
                    wageError = if (!value.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) {
                        "Enter valid amount (e.g. 500 or 500.50)"
                    } else null
                },
                label = { Text("Daily Wage Amount") },
                isError = wageError != null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )



            wageError?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (validateWage(dailyWage)) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val calculatedPayroll = withContext(Dispatchers.IO) {
                                    calculatePayroll(
                                        dailyWage.toDouble(),
                                        dbHelper,
                                        currencyFormat
                                    )
                                }
                                payrollList = calculatedPayroll
                            } catch (e: Exception) {
                                errorMessage = "Error calculating payroll: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Calculate Payroll")
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color.Red,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SearchSortControls(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                sortOption = sortOption,
                onSortOptionChange = { sortOption = it }
            )

            Spacer(modifier = Modifier.height(16.dp))


            if (payrollList.isNotEmpty()) {
                val totalAmount = payrollList.sumOf { it.monthlyPayRaw }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF455A64)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Total Workers: ${payrollList.size}",
                            color = Color.White
                        )

                        Text(
                            text = "Total Payroll: ${currencyFormat.format(totalAmount)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(processedList) { worker ->
                    WorkerPayrollItem(worker = worker, searchQuery = searchQuery)
                    Divider(color = Color.LightGray)
                }
            }
        }
    }
}
@Composable
fun SearchSortControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search by name or work number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        )

        // Sort Dropdown
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sort by: ${sortOption.displayName}")
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort options")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(onClick = {
                        onSortOptionChange(option)
                        expanded = false
                    }) {
                        Text(option.displayName)
                    }
                }
            }
        }
    }
}

enum class SortOption(
    val displayName: String,
    val comparator: Comparator<WorkerPayroll>
) {
    NAME_ASC("Name (A-Z)", compareBy { it.name.lowercase(Locale.getDefault()) }),
    NAME_DESC("Name (Z-A)", compareByDescending { it.name.lowercase(Locale.getDefault()) }),
    DAYS_ASC("Days (Low-High)", compareBy { it.attendedDays }),
    DAYS_DESC("Days (High-Low)", compareByDescending { it.attendedDays }),
    PAY_ASC("Pay (Low-High)", compareBy { it.monthlyPayRaw }),
    PAY_DESC("Pay (High-Low)", compareByDescending { it.monthlyPayRaw });
}

@Composable
fun WorkerPayrollItem(worker: WorkerPayroll, searchQuery:String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        Text(
            text = "Name: ${worker.name}",
            color = Color.White
        )
        Text(
            text = "Work Number: ${worker.workNumber}",
            color = Color.White
        )
        Text(
            text = "Days Worked: ${worker.attendedDays}",
            color = Color.White
        )
        Text(
            text = "Monthly Pay: ${worker.monthlyPay}",
            color = Color.White
        )
        Text(
            text = "Payment Number: ${worker.paymentNumber}",
            color = Color.White
        )
    }
}
@Composable
fun HighlightableText(label: String, text: String, highlightText: String) {
    val annotatedString = remember(text, highlightText) {
        buildAnnotatedString {
            append(label)
            val startIndex = text.indexOf(highlightText, ignoreCase = true)

            if (highlightText.isNotEmpty() && startIndex != -1) {
                val endIndex = startIndex + highlightText.length
                append(text.substring(0, startIndex))
                withStyle(style = SpanStyle(background = Color.Yellow)) {
                    append(text.substring(startIndex, endIndex))
                }
                append(text.substring(endIndex))
            } else {
                append(text)
            }
        }
    }

    Text(
        text = annotatedString,
        color = Color.White
    )
}
private fun validateWage(wage: String): Boolean {
    return wage.matches(Regex("^\\d+(\\.\\d{1,2})?$")) && wage.toDouble() > 0
}

private fun calculatePayroll(
    dailyWage: Double,
    dbHelper: DatabaseHelper,
    currencyFormat: NumberFormat
): List<WorkerPayroll> {
    val workers = mutableListOf<WorkerPayroll>()
    val db = dbHelper.readableDatabase

    // Debugging: Log current month/year
    val (currentMonth, currentYear) = DateUtils.getCurrentOperationalMonthYear().also {
        Log.d("PayrollDebug", "Calculating payroll for Month: ${it.first}, Year: ${it.second}")
    }

    val cursor = db.rawQuery(
        """
        SELECT 
            rc.first_name, 
            rc.surname,
            rc.work_number,
            rc.payment_number,
            IFNULL(ma.days_attended, 0) as days_attended
        FROM ${DatabaseHelper.TABLE_REGISTER_CASUALS} rc
        LEFT JOIN ${DatabaseHelper.TABLE_MONTHLY_ATTENDANCE} ma 
            ON rc.id = ma.${DatabaseHelper.COLUMN_WORKER_ID}
            AND ma.operational_month = ?
            AND ma.operational_year = ?
        WHERE rc.deleted = 0
        ORDER BY rc.surname, rc.first_name
        """.trimIndent(),
        arrayOf(currentMonth.toString(), currentYear.toString())
    )

    try {
        if (cursor.count == 0) {
            Log.w("PayrollDebug", "No workers found in register_casuals")
        }

        while (cursor.moveToNext()) {
            val daysAttended = cursor.getInt(4)
            Log.d("PayrollDebug",
                "Worker ${cursor.getString(2)}: $daysAttended days")

            workers.add(
                WorkerPayroll(
                    name = "${cursor.getString(0)} ${cursor.getString(1)}",
                    workNumber = cursor.getString(2),
                    paymentNumber = cursor.getString(3),
                    attendedDays = daysAttended,
                    monthlyPay = currencyFormat.format(daysAttended * dailyWage),
                    monthlyPayRaw = daysAttended * dailyWage
                )
            )
        }
    } finally {
        cursor.close()
    }

    return workers
}

data class WorkerPayroll(
    val name: String,
    val workNumber: String,
    val attendedDays: Int,
    val monthlyPay: String,
    val monthlyPayRaw: Double,
    val paymentNumber: String
)
