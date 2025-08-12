package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(
    private var students: MutableList<Student>,
    private val onEditClick: (Student) -> Unit,
    private val onDeleteClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvStudentId: TextView = itemView.findViewById(R.id.tvStudentId)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val tvBirth: TextView = itemView.findViewById(R.id.tvBirth)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun getItemCount(): Int = students.size

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = "Họ tên: ${student.hoTen}"
        holder.tvStudentId.text = "Mã SV: ${student.maSinhVien}"
        holder.tvEmail.text = "Email: ${student.email}"
        holder.tvPhone.text = "SĐT: ${student.sdt}"
        holder.tvBirth.text = "Ngày sinh: ${student.ngaySinh}"

        holder.btnEdit.setOnClickListener {
            onEditClick(student)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(student)
        }
    }

    fun updateList(newList: List<Student>) {
        students.clear()
        students.addAll(newList)
        notifyDataSetChanged()
    }
}
