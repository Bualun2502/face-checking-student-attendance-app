package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import androidx.appcompat.app.AlertDialog

class ExamSessionAdapter(
    private val examSessions: List<ExamSession>,
    private val examPeriodId: String,
    private val examDateId: String,
    private val activity: ManageExamSessionsActivity,
    private val onClick: (ExamSession) -> Unit
) : RecyclerView.Adapter<ExamSessionAdapter.ExamSessionViewHolder>() {

    inner class ExamSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.textViewSessionName)
        val tvTime: TextView = itemView.findViewById(R.id.textViewSessionTime)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditSession)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamSessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam_session, parent, false)
        return ExamSessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamSessionViewHolder, position: Int) {
        val session = examSessions[position]
        holder.tvName.text = session.name
        holder.tvTime.text = session.time

        holder.btnEdit.setOnClickListener {
            activity.showAddEditDialog(session, session.id)
        }

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Xác nhận xoá")
                .setMessage("Bạn có chắc chắn muốn xoá ca thi này và toàn bộ phòng thi bên trong không?")
                .setPositiveButton("Xoá") { _, _ ->
                    activity.deleteSession(session.id)
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }


        holder.itemView.setOnClickListener {
            onClick(session)
        }
    }

    override fun getItemCount(): Int = examSessions.size
}
