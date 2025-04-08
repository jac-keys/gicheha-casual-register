package com.gichehafarm.registry
import android.content.ContentValues
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gichehafarm.registry.data.WorkerAttendance

class CasualWorkerAdapter(
    private val context: android.content.Context,
    private val casualWorkers: MutableList<WorkerAttendance>,
    private val database: android.database.sqlite.SQLiteDatabase
) : RecyclerView.Adapter<CasualWorkerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workNumberTextView: TextView = itemView.findViewById(R.id.workNumberTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val attendanceCheckBox: CheckBox = itemView.findViewById(R.id.attendanceCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_casual_worker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val worker = casualWorkers[position]
        holder.workNumberTextView.text = worker.workNumber
        holder.nameTextView.text = "${worker.firstName} ${worker.surname}"
        holder.attendanceCheckBox.isChecked = worker.present

        // Update the database when the checkbox is clicked
        holder.attendanceCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val values = ContentValues().apply {
                put("attended", if (isChecked) 1 else 0)
            }
            database.update(
                "record_attendance",
                values,
                "work_number = ?",
                arrayOf(worker.workNumber))
        }
    }

    override fun getItemCount(): Int {
        return casualWorkers.size
    }
}