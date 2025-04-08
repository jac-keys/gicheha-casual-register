package com.gichehafarm.registry

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SupervisorAdapter(
    private val context: Context,
    private val supervisors: MutableList<Supervisor>,
    private val database: SQLiteDatabase
) : RecyclerView.Adapter<SupervisorAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workNumberTextView: TextView = itemView.findViewById(R.id.workNumberTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val attendanceCheckBox: CheckBox = itemView.findViewById(R.id.attendanceCheckBox) // Uncommented
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_casual_worker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val worker = supervisors[position] // Fixed incorrect Supervisor[position]

        holder.workNumberTextView.text = worker.workNumber
        holder.nameTextView.text = "${worker.firstName} ${worker.surname}"


        // Update the database when the checkbox is clicked
        holder.attendanceCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val values = ContentValues().apply {
                put("attended", if (isChecked) 1 else 0)
            }
            database.update(
                "register_supervisors",
                values,
                "work_number = ?",
                arrayOf(worker.workNumber)
            )
        }
    }

    override fun getItemCount(): Int {
        return supervisors.size // Fixed incorrect supervisors.size
    }
}
