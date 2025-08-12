package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExamPeriodAdapter(
    private val examPeriods: List<ExamPeriod>,
    private val onEdit: (ExamPeriod) -> Unit,
    private val onDelete: (ExamPeriod) -> Unit,
    private val onClick: (ExamPeriod) -> Unit // ✅ Thêm callback mới
) : RecyclerView.Adapter<ExamPeriodAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.tvExamName)
        val descText: TextView = itemView.findViewById(R.id.tvExamDesc)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditExam)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteExam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam_period, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exam = examPeriods[position]
        holder.nameText.text = exam.name
        holder.descText.text = exam.description

        holder.btnEdit.setOnClickListener { onEdit(exam) }
        holder.btnDelete.setOnClickListener { onDelete(exam) }

        // ✅ Khi bấm vào toàn bộ item -> mở ngày thi
        holder.itemView.setOnClickListener {
            onClick(exam)
        }
    }

    override fun getItemCount(): Int = examPeriods.size
}
