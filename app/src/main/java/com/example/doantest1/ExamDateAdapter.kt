package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import android.content.Intent


class ExamDateAdapter(
    private val examDates: List<ExamDate>,
    private val onEdit: (ExamDate) -> Unit,
    private val onDelete: (ExamDate) -> Unit,
    private val examPeriodId: String
) : RecyclerView.Adapter<ExamDateAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvExamDate)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditExamDate)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteExamDate)
        val btnOpenSessions: Button = itemView.findViewById(R.id.btnOpenExamSessions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam_date, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = examDates[position]
        holder.tvDate.text = date.date

        holder.btnEdit.setOnClickListener { onEdit(date) }
        holder.btnDelete.setOnClickListener { onDelete(date) }

        holder.btnOpenSessions.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ManageExamSessionsActivity::class.java)
            intent.putExtra("EXAM_PERIOD_ID", examPeriodId)
            intent.putExtra("EXAM_DATE_ID", date.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = examDates.size
}
