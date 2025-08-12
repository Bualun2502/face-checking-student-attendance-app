package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class TeacherAdapter(
    private val teachers: List<Teacher>,
    private val activity: ManageTeachersActivity
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    class TeacherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvTeacherName)
        val tvFaculty: TextView = view.findViewById(R.id.tvTeacherFaculty)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditTeacher)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTeacher)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }

    override fun getItemCount(): Int = teachers.size

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teachers[position]
        holder.tvName.text = teacher.name
        holder.tvFaculty.text = teacher.faculty

        holder.btnEdit.setOnClickListener {
            activity.showAddEditDialog(teacher)
        }

        holder.btnDelete.setOnClickListener {
            activity.deleteTeacher(teacher.id)
        }
    }
}
