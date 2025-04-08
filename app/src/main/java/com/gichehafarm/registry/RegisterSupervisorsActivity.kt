package com.gichehafarm.registry

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.gichehafarm.registry.data.DatabaseHelper
import com.gichehafarm.registry.data.DatabaseHelper.Companion.TABLE_REGISTER_SUPERVISORS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterSupervisorsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterSupervisorsScreen()
        }
    }
}

@Composable
fun RegisterSupervisorsScreen() {
    var firstName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var surName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var workNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var paymentNumber by remember { mutableStateOf("") }
    var idError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var workNumberError by remember { mutableStateOf<String?>(null) }
    var paymentNumberError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Company Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("Register New Supervisors", fontSize = 22.sp, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            // Input Fields
            CustomTextField(value = firstName, label = "First Name") { firstName = it }
            CustomTextField(value = surName, label = "Surname") { surName = it }
            DatePickerTextField(value = dateOfBirth, label = "Date of Birth") { dateOfBirth = it }
            CustomTextField(
                value = idNumber,
                label = "ID Number",
                isNumber = true
            ) {
                idNumber = it
                idError = if (!it.matches(Regex("^\\d{6,9}$"))) {
                    "ID Number must be 6-9 digits"
                } else null
            }
            idError?.let {
                Text(
                    it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            CustomTextField(
                value = workNumber,
                label = "Work Number",
                isNumber = true
            ) { workNumber = it }
            CustomTextField(
                value = phone,
                label = "Phone Number",
                isNumber = true
            ) {
                phone = it
                phoneError = if (!it.matches(Regex("^(07|01)\\d{8}$"))) {
                    "Phone Number must be 10 digits and start with 07 or 01"
                } else null
            }
            phoneError?.let {
                Text(
                    it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            CustomTextField(
                value = paymentNumber,
                label = "Payment Number",
                isNumber = true
            ) {

                    paymentNumber = it
                    paymentNumberError = if (!it.matches(Regex("^(07|01)\\d{8}$"))) {
                        "Phone Number must be 10 digits and start with 07 or 01"
                    } else null
                }
                paymentNumberError?.let {
                    Text(
                        it,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }


            Spacer(modifier = Modifier.height(10.dp))
            RegisterButton(
                text = "Save to Database",
                icon = R.drawable.ic_save,
                onClick = {
                    // Validate all fields
                    if (firstName.isEmpty() || surName.isEmpty() || dateOfBirth.isEmpty() ||
                        phone.isEmpty() || idNumber.isEmpty() || workNumber.isEmpty() ||
                        paymentNumber.isEmpty()
                    ) {
                        Toast.makeText(
                            context,
                            "Please fill all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (idError != null || phoneError != null ||
                        workNumberError != null || paymentNumberError != null
                    ) {
                        Toast.makeText(
                            context,
                            "Please fix the errors in the form",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Save supervisor to the database
                        CoroutineScope(Dispatchers.IO).launch {
                            val dbHelper = DatabaseHelper.getInstance(context)
                            val db = dbHelper.writableDatabase

                            // Check for duplicates
                            val existsQuery = """
                                SELECT COUNT(*) FROM register_supervisors 
                                WHERE work_number = ? OR phone = ? OR id_number = ? OR payment_number = ?
                            """
                            val cursor = db.rawQuery(existsQuery, arrayOf(workNumber, phone, idNumber, paymentNumber))
                            cursor.moveToFirst()
                            val count = cursor.getInt(0)
                            cursor.close()

                            if (count > 0) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: Duplicate values found!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Insert supervisor
                                val insertQuery = """
                                    INSERT INTO $TABLE_REGISTER_SUPERVISORS 
                                    (work_number, first_name, surname, date_of_birth, phone, id_number, payment_number) 
                                    VALUES (?, ?, ?, ?, ?, ?, ?)
                                """
                                try {
                                    val statement = db.compileStatement(insertQuery).apply {
                                        bindString(1, workNumber)
                                        bindString(2, firstName)
                                        bindString(3, surName)
                                        bindString(4, dateOfBirth)
                                        bindString(5, phone)
                                        bindString(6, idNumber)
                                        bindString(7, paymentNumber)
                                       // bindLong(8, 0) // Default attended value
                                    }
                                    statement.executeInsert()

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Supervisor registered successfully!", Toast.LENGTH_SHORT).show()
                                    }

                                    // Clear fields after successful registration
                                    firstName = ""
                                    surName = ""
                                    dateOfBirth = ""
                                    idNumber = ""
                                    workNumber = ""
                                    phone = ""
                                    paymentNumber = ""
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to register supervisor: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    label: String,
    isNumber: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Black) },
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun DatePickerTextField(value: String, label: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndSetDate(input: String) {
        val regex = Regex("^\\d{2}/\\d{2}/\\d{4}$") // Ensure format is DD/MM/YYYY

        if (!regex.matches(input)) {
            errorMessage = "Invalid format. Use DD/MM/YYYY."
            return
        }

        val parts = input.split("/")
        val day = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()?.minus(1) // Calendar months are 0-based
        val year = parts[2].toIntOrNull()

        if (day == null || month == null || year == null) {
            errorMessage = "Invalid date. Please try again."
            return
        }

        val selectedCalendar = Calendar.getInstance().apply {
            set(year, month, day)
        }

        val age = calculateAge(selectedCalendar.timeInMillis)

        if (age in 18..55) {
            onValueChange(input)
            errorMessage = null
        } else {
            errorMessage = "Age must be between 18 and 55 years."
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            validateAndSetDate(it)
        },
        label = { Text(label, color = Color.Black) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = "Pick a date",
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedDate =
                                    "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                                onValueChange(formattedDate)
                                validateAndSetDate(formattedDate)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.show()
                    }
            )
        },
        singleLine = true
    )

    errorMessage?.let {
        Text(
            it,
            color = Color.Red,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

/**
 * Helper function to calculate age from date in milliseconds.
 */
fun calculateAge(dateInMillis: Long): Int {
    val birthCalendar = Calendar.getInstance().apply { timeInMillis = dateInMillis }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

    if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
        age-- // Adjust if birthday hasn't occurred yet this year
    }

    return age
}

@Composable
fun RegisterButton(text: String, icon: Int, onClick: () -> Unit) {
    val buttonColor = Color.White
    val textColor = Color(0xFF2E7D32)

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                color = textColor
            )
        }
    }
}