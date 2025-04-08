package com.gichehafarm.registry.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import com.gichehafarm.registry.CasualWorker
import com.gichehafarm.registry.Supervisor
import com.gichehafarm.registry.data.WorkerAttendance
import com.gichehafarm.registry.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DatabaseHelper private constructor(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "casual_registry.db"
        private const val DATABASE_VERSION = 45// Incremented version

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }

        private const val TAG = "DatabaseHelper"

        // Table names
        const val TABLE_REGISTER_CASUALS = "register_casuals"
        const val TABLE_RECORD_ATTENDANCE = "record_attendance"
        const val TABLE_REGISTER_SUPERVISORS = "register_supervisors"
        const val TABLE_MONTHLY_ATTENDANCE = "monthly_attendance"
        const val TABLE_HR_ATTENDANCE = "hr_attendance"

        // Column names
        const val COLUMN_ID = "id"
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_SURNAME = "surname"
        const val COLUMN_DATE_OF_BIRTH = "date_of_birth"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ID_NUMBER = "id_number"
        const val COLUMN_WORK_NUMBER = "work_number"
        const val COLUMN_PAYMENT_NUMBER = "payment_number"
        const val COLUMN_DATE = "date"
        const val COLUMN_ATTENDED = "attended"
        const val COLUMN_DELETED = "deleted"
        const val COLUMN_WORKER_ID = "worker_id"
        private const val COLUMN_OPERATIONAL_MONTH = "operational_month"
        const val COLUMN_OPERATIONAL_YEAR = "operational_year"
        const val COLUMN_DAYS_ATTENDED = "days_attended"

        private const val PREFS_NAME = "AttendancePrefs"
        private const val LAST_SAVED_DATE_KEY = "last_saved_date"
    }


    override fun onCreate(db: SQLiteDatabase) {
        val createRegisterCasualsTable = """
            CREATE TABLE $TABLE_REGISTER_CASUALS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRST_NAME TEXT NOT NULL,
                $COLUMN_SURNAME TEXT NOT NULL,
                $COLUMN_DATE_OF_BIRTH TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_WORK_NUMBER TEXT NOT NULL UNIQUE,
                $COLUMN_ID_NUMBER TEXT NOT NULL,
                $COLUMN_PAYMENT_NUMBER TEXT NOT NULL,
                $COLUMN_DATE DATE DEFAULT (CURRENT_DATE),
                $COLUMN_DELETED INTEGER DEFAULT 0
            )
        """.trimIndent()

        // SQL statement to create monthly_attendance table (constant-based structure)
        val createMonthlyAttendanceTable = """
        CREATE TABLE $TABLE_MONTHLY_ATTENDANCE (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_WORKER_ID INTEGER NOT NULL,
            $COLUMN_OPERATIONAL_MONTH INTEGER NOT NULL,
            $COLUMN_OPERATIONAL_YEAR INTEGER NOT NULL,
            $COLUMN_DAYS_ATTENDED INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY($COLUMN_WORKER_ID) REFERENCES $TABLE_REGISTER_CASUALS($COLUMN_ID) ON DELETE CASCADE,
            UNIQUE($COLUMN_WORKER_ID, $COLUMN_OPERATIONAL_MONTH, $COLUMN_OPERATIONAL_YEAR)
        )
    """.trimIndent()

        val createHrAttendanceTable = """
    CREATE TABLE $TABLE_HR_ATTENDANCE (
        $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_WORKER_ID INTEGER NOT NULL,
        $COLUMN_WORK_NUMBER TEXT NOT NULL,
        $COLUMN_DATE DATE,
        $COLUMN_ATTENDED BOOLEAN,
        FOREIGN KEY ($COLUMN_WORKER_ID) REFERENCES $TABLE_REGISTER_CASUALS($COLUMN_ID),
        FOREIGN KEY ($COLUMN_ATTENDED) REFERENCES $TABLE_RECORD_ATTENDANCE($COLUMN_ATTENDED)
        UNIQUE ($COLUMN_WORKER_ID, $COLUMN_DATE)
    )
""".trimIndent()


        val createRecordAttendanceTable = """
            CREATE TABLE $TABLE_RECORD_ATTENDANCE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WORK_NUMBER TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_FIRST_NAME TEXT NOT NULL,
                $COLUMN_SURNAME TEXT NOT NULL,
                $COLUMN_ATTENDED INTEGER DEFAULT 0,
                $COLUMN_DAYS_ATTENDED INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createRegisterSupervisorsTable = """
            CREATE TABLE $TABLE_REGISTER_SUPERVISORS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRST_NAME TEXT NOT NULL,
                $COLUMN_SURNAME TEXT NOT NULL,
                $COLUMN_DATE_OF_BIRTH TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_WORK_NUMBER TEXT NOT NULL UNIQUE,
                $COLUMN_ID_NUMBER TEXT NOT NULL,
                $COLUMN_PAYMENT_NUMBER TEXT NOT NULL,
                $COLUMN_DATE DATE DEFAULT (CURRENT_DATE),
                $COLUMN_DELETED INTEGER DEFAULT 0
            )
        """.trimIndent()

        db.execSQL(createRegisterCasualsTable)
        db.execSQL(createMonthlyAttendanceTable)
        db.execSQL(createRecordAttendanceTable)
        db.execSQL(createRegisterSupervisorsTable)
        db.execSQL(createHrAttendanceTable)
        Log.d("Database", "Tables created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REGISTER_CASUALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REGISTER_SUPERVISORS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORD_ATTENDANCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MONTHLY_ATTENDANCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HR_ATTENDANCE")

        // Recreate all tables
        onCreate(db)

        Log.d("Database", "Database upgraded and tables recreated")
    }


    fun addCasualWorker(worker: CasualWorker): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, worker.firstName)
            put(COLUMN_SURNAME, worker.surname)
            put(COLUMN_DATE_OF_BIRTH, worker.dateOfBirth)
            put(COLUMN_PHONE, worker.phone)
            put(COLUMN_WORK_NUMBER, worker.workNumber)
            put(COLUMN_ID_NUMBER, worker.idNumber)
            put(COLUMN_PAYMENT_NUMBER, worker.paymentNumber)
        }

        val result = db.insert(TABLE_REGISTER_CASUALS, null, values)
        db.close()

        if (result == -1L) {
            Log.e("Database", "Failed to insert worker: ${worker.firstName}")
            return false
        }

        val uri = Uri.parse("content://com.gichehafarm.registry/$TABLE_REGISTER_CASUALS")
        context.contentResolver.notifyChange(uri, null)
        Log.d("Database", "Worker added: ${worker.firstName}")
        return true
    }

    fun incrementDaysAttended() {
        val db = writableDatabase
        try {
            db.execSQL("""
    UPDATE $TABLE_MONTHLY_ATTENDANCE
    SET $COLUMN_DAYS_ATTENDED = $COLUMN_DAYS_ATTENDED + 1
    WHERE $COLUMN_WORKER_ID IN (
        SELECT rc.$COLUMN_ID
        FROM $TABLE_REGISTER_CASUALS rc
        JOIN $TABLE_RECORD_ATTENDANCE ra 
          ON rc.$COLUMN_WORK_NUMBER = ra.$COLUMN_WORK_NUMBER
        WHERE date(ra.$COLUMN_DATE) = date('now')
          AND ra.$COLUMN_ATTENDED = 1
    )
""".trimIndent())

            Log.d(TAG, "Successfully incremented days attended")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing days attended", e)
        } finally {
            db.close()
        }
    }

    fun getAttendanceCountsByDateRange(startDate: String, endDate: String): List<Pair<String, Int>> {
        val db = readableDatabase
        return db.rawQuery("""
        SELECT 
            $COLUMN_DATE, 
            COUNT(*) as attendance_count 
        FROM $TABLE_HR_ATTENDANCE 
        WHERE $COLUMN_DATE BETWEEN ? AND ?
        GROUP BY $COLUMN_DATE
        ORDER BY $COLUMN_DATE DESC
    """.trimIndent(), arrayOf(startDate, endDate)).use { cursor ->
            val results = mutableListOf<Pair<String, Int>>()
            while (cursor.moveToNext()) {
                results.add(cursor.getString(0) to cursor.getInt(1))
            }
            results
        }
    }


    fun addSupervisor(supervisor: Supervisor): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, supervisor.firstName)
            put(COLUMN_SURNAME, supervisor.surname)
            put(COLUMN_DATE_OF_BIRTH, supervisor.dateOfBirth)
            put(COLUMN_PHONE, supervisor.phone)
            put(COLUMN_WORK_NUMBER, supervisor.workNumber)
            put(COLUMN_ID_NUMBER, supervisor.idNumber)
            put(COLUMN_PAYMENT_NUMBER, supervisor.paymentNumber)
        }

        val result = db.insert(TABLE_REGISTER_SUPERVISORS, null, values)
        db.close()

        if (result == -1L) {
            Log.e("Database", "Failed to insert supervisor: ${supervisor.firstName}")
            return false
        }

        val uri = Uri.parse("content://com.gichehafarm.registry/$TABLE_REGISTER_SUPERVISORS")
        context.contentResolver.notifyChange(uri, null)
        Log.d("Database", "Supervisor added: ${supervisor.firstName}")
        return true
    }

    fun deleteCasualWorker(workerId: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_REGISTER_CASUALS, "$COLUMN_ID = ?", arrayOf(workerId.toString()))
        db.close()
        return if (result > 0) {
            Log.d("Database", "Worker deleted successfully (ID: $workerId)")
            true
        } else {
            Log.e("Database", "Failed to delete worker (ID: $workerId)")
            false
        }
    }

    fun deleteSupervisor(workerId: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_REGISTER_SUPERVISORS, "$COLUMN_ID = ?", arrayOf(workerId.toString()))
        db.close()
        return if (result > 0) {
            Log.d("Database", "Supervisor deleted successfully (ID: $workerId)")
            true
        } else {
            Log.e("Database", "Failed to delete Supervisor (ID: $workerId)")
            false
        }
    }

    fun getRegisteredCasualsByDate(selectedDate: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_REGISTER_CASUALS WHERE $COLUMN_DATE = ? AND $COLUMN_DELETED = 0",
            arrayOf(selectedDate)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getDeletedCasualsByDate(selectedDate: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_REGISTER_CASUALS WHERE $COLUMN_DATE = ? AND $COLUMN_DELETED = 1",
            arrayOf(selectedDate)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun loadCasualWorkers(): List<CasualWorker> {
        val workers = mutableListOf<CasualWorker>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_WORK_NUMBER, $COLUMN_FIRST_NAME, $COLUMN_SURNAME, $COLUMN_DATE_OF_BIRTH, $COLUMN_PHONE, $COLUMN_ID_NUMBER, $COLUMN_PAYMENT_NUMBER FROM $TABLE_REGISTER_CASUALS",
            null
        )
        while (cursor.moveToNext()) {
            workers.add(
                CasualWorker(
                    id = cursor.getInt(0),
                    workNumber = cursor.getString(1),
                    firstName = cursor.getString(2),
                    surname = cursor.getString(3),
                    dateOfBirth = cursor.getString(4),
                    phone = cursor.getString(5),
                    idNumber = cursor.getString(6),
                    paymentNumber = cursor.getString(7)
                )
            )
        }
        cursor.close()
        return workers
    }

    fun loadSupervisors(): List<Supervisor> {
        val supervisors = mutableListOf<Supervisor>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_REGISTER_SUPERVISORS WHERE $COLUMN_DELETED = 0", null
        )
        while (cursor.moveToNext()) {
            supervisors.add(
                Supervisor(
                    id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                    workNumber = cursor.getString(cursor.getColumnIndex(COLUMN_WORK_NUMBER)),
                    firstName = cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME)),
                    surname = cursor.getString(cursor.getColumnIndex(COLUMN_SURNAME)),
                    dateOfBirth = cursor.getString(cursor.getColumnIndex(COLUMN_DATE_OF_BIRTH)),
                    phone = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE)),
                    idNumber = cursor.getString(cursor.getColumnIndex(COLUMN_ID_NUMBER)),
                    paymentNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PAYMENT_NUMBER))
                )
            )
        }
        cursor.close()
        return supervisors
    }

    fun loadAttendanceRecords(databaseHelper: DatabaseHelper, month: Int, year: Int): List<Pair<String, Int>> {
        return try {
            val calendar = Calendar.getInstance().apply {
                set(year, month, 21, 0, 0, 0)
            }
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 20)
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val db = databaseHelper.readableDatabase
            val query = """
                SELECT date, COUNT(*) as count 
                FROM hr_attendance 
                WHERE date BETWEEN ? AND ? 
                GROUP BY date
                ORDER BY date DESC
            """
            db.rawQuery(query, arrayOf(startDate, endDate)).use { cursor ->
                val result = mutableListOf<Pair<String, Int>>()
                while (cursor.moveToNext()) {
                    val date = cursor.getString(0)
                    val count = cursor.getInt(1)
                    result.add(date to count)
                }
                result
            }
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error loading attendance", e)
            emptyList()
        }
    }

    fun updateWorkerAttendance(workNumber: String, attended: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_RECORD_ATTENDANCE SET $COLUMN_ATTENDED = ? WHERE $COLUMN_WORK_NUMBER = ?", arrayOf(if (attended) 1 else 0, workNumber))
        db.close()
    }

    fun searchCasualWorkers(query: String): List<CasualWorker> {
        val workers = mutableListOf<CasualWorker>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
                SELECT $COLUMN_ID, $COLUMN_WORK_NUMBER, $COLUMN_FIRST_NAME, $COLUMN_SURNAME, $COLUMN_DATE_OF_BIRTH, $COLUMN_PHONE, $COLUMN_ID_NUMBER, $COLUMN_PAYMENT_NUMBER, $COLUMN_ATTENDED
                FROM $TABLE_REGISTER_CASUALS
                WHERE $COLUMN_FIRST_NAME LIKE ? 
                   OR $COLUMN_SURNAME LIKE ? 
                   OR $COLUMN_PHONE LIKE ? 
                   OR $COLUMN_WORK_NUMBER LIKE ? 
                   OR $COLUMN_ID_NUMBER LIKE ?
            """.trimIndent(),
            arrayOf("%$query%", "%$query%", "%$query%", "%$query%", "%$query%")
        )
        while (cursor.moveToNext()) {
            workers.add(
                CasualWorker(
                    id = cursor.getInt(0),
                    workNumber = cursor.getString(1),
                    firstName = cursor.getString(2),
                    surname = cursor.getString(3),
                    dateOfBirth = cursor.getString(4),
                    phone = cursor.getString(5),
                    idNumber = cursor.getString(6),
                    paymentNumber = cursor.getString(7)
                )
            )
        }
        cursor.close()
        return workers
    }

    // Corrected getPresentWorkersForDate query to return separate first and surname columns
    fun getPresentWorkersForDate(targetDate: String): List<WorkerAttendance> {
        val presentWorkers = mutableListOf<WorkerAttendance>()
        val db = readableDatabase

        val query = """
            SELECT 
                rc.$COLUMN_WORK_NUMBER,
                rc.$COLUMN_FIRST_NAME,
                rc.$COLUMN_SURNAME,
                ra.$COLUMN_DATE,
                ra.$COLUMN_ATTENDED
            FROM $TABLE_REGISTER_CASUALS rc
            JOIN $TABLE_RECORD_ATTENDANCE ra ON rc.$COLUMN_WORK_NUMBER = ra.$COLUMN_WORK_NUMBER
            WHERE date(ra.$COLUMN_DATE) = date(?)
              AND ra.$COLUMN_ATTENDED = 1
              AND rc.$COLUMN_DELETED = 0
              AND strftime('%H', ra.$COLUMN_DATE) = '19'
            ORDER BY rc.$COLUMN_SURNAME, rc.$COLUMN_FIRST_NAME
        """.trimIndent()

        db.rawQuery(query, arrayOf(targetDate)).use { cursor ->
            while (cursor.moveToNext()) {
                presentWorkers.add(
                    WorkerAttendance(
                        workNumber = cursor.getString(0),
                        firstName = cursor.getString(1),
                        surname = cursor.getString(2),
                        date = cursor.getString(3),
                        present = cursor.getInt(4) == 1
                    )
                )
            }
        }
        return presentWorkers
    }

    fun copyCasualsToAttendance() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM attendance")
            db.execSQL("""
                INSERT INTO attendance (worker_id, name, date, status)
                SELECT id, first_name || ' ' || surname, date('now'), 'absent' 
                FROM $TABLE_REGISTER_CASUALS
            """.trimIndent())
            db.setTransactionSuccessful()
            Log.d(TAG, "Successfully copied casual workers to attendance")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy casual workers to attendance", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }

    fun getLastAttendanceDate(): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(LAST_SAVED_DATE_KEY, "") ?: ""
    }

    fun saveAttendanceToHr(workers: List<CasualWorker>, date: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            workers.forEach { worker ->
                val values = ContentValues().apply {
                    put(COLUMN_WORKER_ID, worker.id)
                    put(COLUMN_WORK_NUMBER, worker.workNumber)
                    put(COLUMN_DATE, date)
                }
                db.insertWithOnConflict(
                    TABLE_HR_ATTENDANCE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            Log.e("Database", "Error saving HR attendance", e)
            return false
        } finally {
            db.endTransaction()
        }
    }
    fun processDailyAttendance() {
        val db = writableDatabase
        val (currentMonth, currentYear) = DateUtils.getCurrentOperationalMonthYear()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.beginTransaction()
        try {
            Log.d("AttendanceProcess", "Processing daily attendance for $today")

            // 1. Get all workers who attended today from RECORD_ATTENDANCE
            val attendedWorkers = db.rawQuery("""
            SELECT id FROM $TABLE_RECORD_ATTENDANCE 
            WHERE $COLUMN_ATTENDED = 1 
        """, null)

            var processedCount = 0

            // 2. Update monthly attendance for each worker
            while (attendedWorkers.moveToNext()) {
                val workerId = attendedWorkers.getInt(0)
                processedCount++

                db.execSQL("""
                INSERT OR REPLACE INTO $TABLE_MONTHLY_ATTENDANCE 
                ($COLUMN_WORKER_ID, $COLUMN_OPERATIONAL_MONTH, $COLUMN_OPERATIONAL_YEAR, $COLUMN_DAYS_ATTENDED)
                VALUES (?, ?, ?, 
                    COALESCE(
                        (SELECT $COLUMN_DAYS_ATTENDED 
                         FROM $TABLE_MONTHLY_ATTENDANCE 
                         WHERE $COLUMN_WORKER_ID = ? 
                           AND $COLUMN_OPERATIONAL_MONTH = ? 
                           AND $COLUMN_OPERATIONAL_YEAR = ?),
                        0
                    ) + 1
                )
            """, arrayOf(workerId, currentMonth, currentYear,
                    workerId, currentMonth, currentYear))
            }

            Log.d("AttendanceProcess", "Updated attendance for $processedCount workers")

            attendedWorkers.close()

            // 3. Reset daily attendance flags
            db.execSQL("UPDATE $TABLE_RECORD_ATTENDANCE SET $COLUMN_ATTENDED = 0")
            Log.d("AttendanceProcess", "Reset daily attendance flags")

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("AttendanceProcess", "Error processing attendance", e)
        } finally {
            db.endTransaction()
        }
    }


    fun checkAndResetMonthlyAttendanceIfNeeded() {
        val (currentMonth, currentYear) = DateUtils.getCurrentOperationalMonthYear()
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (currentMonth != prefs.getInt("last_month", -1) ||
            currentYear != prefs.getInt("last_year", -1)) {
            // Initialize new monthly records for all workers
            initializeMonthlyAttendance(writableDatabase)
            prefs.edit().apply {
                putInt("last_month", currentMonth)
                putInt("last_year", currentYear)
                apply()
            }
        }
    }

    fun recordAttendance(workNumber: String, date: String, attended: Boolean): Boolean {
        val db = writableDatabase
        return try {
            if (attended) {
                // Get worker ID first
                val workerId = db.rawQuery("""
                SELECT $COLUMN_ID FROM $TABLE_REGISTER_CASUALS 
                WHERE $COLUMN_WORK_NUMBER = ?
            """.trimIndent(), arrayOf(workNumber)).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else -1
                }

                if (workerId != -1) {
                    val values = ContentValues().apply {
                        put(COLUMN_WORKER_ID, workerId)
                        put(COLUMN_WORK_NUMBER, workNumber)
                        put(COLUMN_DATE, date)
                    }
                    val success = db.insertWithOnConflict(
                        TABLE_HR_ATTENDANCE,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    ) != -1L
                    if (success) {
                        // Parse the attendance date (format: yyyy-MM-dd)
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val attendanceDate = dateFormat.parse(date) ?: return false
                        val calendar = Calendar.getInstance().apply {
                            time = attendanceDate
                        }

                        val (month, year) = DateUtils.getOperationalMonthYear(calendar)
                        aggregateMonthlyAttendance(workerId, month, year)

                    }

                    success
                } else false
            } else {
                // Remove attendance record if marked absent
                db.delete(
                    TABLE_HR_ATTENDANCE,
                    "$COLUMN_WORK_NUMBER = ? AND $COLUMN_DATE = ?",
                    arrayOf(workNumber, date)
                ) > 0
            }
        } catch (e: Exception) {
            false
        }

    }
    private fun initializeMonthlyAttendance(db: SQLiteDatabase) {
        val (currentMonth, currentYear) = DateUtils.getCurrentOperationalMonthYear()
        val cursor = db.rawQuery("""
            SELECT $COLUMN_ID FROM $TABLE_REGISTER_CASUALS WHERE $COLUMN_DELETED = 0
        """, null)
        while (cursor.moveToNext()) {
            val workerId = cursor.getInt(0)
            // Check if the record already exists
            val existsCursor = db.rawQuery("""
                SELECT COUNT(*) FROM $TABLE_MONTHLY_ATTENDANCE 
                WHERE $COLUMN_WORKER_ID = ? 
                  AND $COLUMN_OPERATIONAL_MONTH = ? 
                  AND $COLUMN_OPERATIONAL_YEAR = ?
            """, arrayOf(workerId.toString(), currentMonth.toString(), currentYear.toString()))
            if (existsCursor.moveToFirst() && existsCursor.getInt(0) == 0) {
                // Only insert if it doesn't exist
                db.execSQL("""
                    INSERT INTO $TABLE_MONTHLY_ATTENDANCE 
                    ($COLUMN_WORKER_ID, $COLUMN_OPERATIONAL_MONTH, $COLUMN_OPERATIONAL_YEAR)
                    VALUES (?, ?, ?)
                """, arrayOf(workerId, currentMonth, currentYear))
            }
            existsCursor.close()
        }
        cursor.close()
    }

    fun resetDailyAttendanceFlags() {
        val db = writableDatabase
        try {
            // Clear today's attendance records
            db.execSQL("""
                DELETE FROM $TABLE_RECORD_ATTENDANCE 
                WHERE date($COLUMN_DATE) = date('now')
            """)
            Log.d(TAG, "Successfully reset attendance records")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting attendance records", e)
        } finally {
            db.close()
        }
    }

    fun getTodaysAttendanceCount(): Int {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return db.rawQuery("""
            SELECT COUNT(*) 
            FROM $TABLE_RECORD_ATTENDANCE 
            WHERE date($COLUMN_DATE) = date(?)
              AND $COLUMN_ATTENDED = 1
        """, arrayOf(today)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getWorkerMonthlyAttendance(workerId: Int, month: Int, year: Int): Int {
        val db = readableDatabase
        return db.rawQuery("""
            SELECT $COLUMN_DAYS_ATTENDED 
            FROM $TABLE_MONTHLY_ATTENDANCE 
            WHERE $COLUMN_WORKER_ID = ?
              AND $COLUMN_OPERATIONAL_MONTH = ?
              AND $COLUMN_OPERATIONAL_YEAR = ?
        """, arrayOf(workerId.toString(), month.toString(), year.toString())).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    // Call this at the end of each month or when calculating payroll
    fun aggregateMonthlyAttendance(workerId: Int, month: Int, year: Int) {
        val db = writableDatabase
        // Calculate date range for the operational month (e.g., 21st to 20th)
        val (startDate, endDate) = DateUtils.getOperationalPeriod(month, year)

        val daysAttended = db.rawQuery(
            """
        SELECT COUNT(*) 
        FROM $TABLE_HR_ATTENDANCE 
        WHERE $COLUMN_WORKER_ID = ? 
         AND $COLUMN_DATE BETWEEN ? AND ?
        """.trimIndent(),
            arrayOf(workerId.toString(),startDate, endDate)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        val values = ContentValues().apply {
            put(COLUMN_WORKER_ID, workerId)
            put("operational_month", month)
            put("operational_year", year)
            put("days_attended", daysAttended)
        }
        db.insertWithOnConflict(
            TABLE_MONTHLY_ATTENDANCE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getWorkerIdByWorkNumber(workNumber: String): Int {
        val db = readableDatabase
        return db.rawQuery("""
        SELECT $COLUMN_ID 
        FROM $TABLE_REGISTER_CASUALS 
        WHERE $COLUMN_WORK_NUMBER = ?
    """, arrayOf(workNumber)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else -1
        }
    }
    fun getWorkerAttendanceStatus(workNumber: String, date: String): Boolean {
        val db = readableDatabase
        return db.rawQuery("""
        SELECT COUNT(*) FROM $TABLE_HR_ATTENDANCE 
        WHERE $COLUMN_WORK_NUMBER = ? 
        AND $COLUMN_DATE = ?
    """.trimIndent(), arrayOf(workNumber, date)).use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) > 0}
        }
    }
