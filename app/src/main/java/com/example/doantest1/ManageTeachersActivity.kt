package com.example.doantest1

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ManageTeachersActivity : AppCompatActivity() {

    private lateinit var teacherRecyclerView: RecyclerView
    private lateinit var teacherList: MutableList<Teacher>
    private lateinit var teacherAdapter: TeacherAdapter
    private lateinit var btnAddTeacher: Button
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_teachers)

        teacherRecyclerView = findViewById(R.id.teacherRecyclerView)
        btnAddTeacher = findViewById(R.id.btnAddTeacher)

        teacherList = mutableListOf()
        teacherAdapter = TeacherAdapter(teacherList, this)
        teacherRecyclerView.layoutManager = LinearLayoutManager(this)
        teacherRecyclerView.adapter = teacherAdapter

        database = FirebaseDatabase.getInstance().getReference("teachers")

        loadTeachers()

        btnAddTeacher.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun loadTeachers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                teacherList.clear()
                for (teacherSnapshot in snapshot.children) {
                    val teacher = teacherSnapshot.getValue(Teacher::class.java)
                    teacher?.let { teacherList.add(it) }
                }
                teacherAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageTeachersActivity, "Lỗi khi tải dữ liệu.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showAddEditDialog(teacherToEdit: Teacher?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.etTeacherName)
        val birthEditText = dialogView.findViewById<EditText>(R.id.etBirthDate)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.etPhone)
        val emailEditText = dialogView.findViewById<EditText>(R.id.etEmail)
        val facultyEditText = dialogView.findViewById<EditText>(R.id.etFaculty)

        val isEditMode = teacherToEdit != null
        val dialogTitle = if (isEditMode) "Sửa giáo viên" else "Thêm giáo viên"

        if (isEditMode) {
            nameEditText.setText(teacherToEdit?.name)
            birthEditText.setText(teacherToEdit?.birthDate)
            phoneEditText.setText(teacherToEdit?.phone)
            emailEditText.setText(teacherToEdit?.email)
            facultyEditText.setText(teacherToEdit?.faculty)
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = nameEditText.text.toString()
                val birthDate = birthEditText.text.toString()
                val phone = phoneEditText.text.toString()
                val email = emailEditText.text.toString()
                val faculty = facultyEditText.text.toString()

                val id = teacherToEdit?.id ?: database.push().key!!
                val newTeacher = Teacher(id, name, birthDate, phone, email,faculty)
                database.child(id).setValue(newTeacher)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun deleteTeacher(teacherId: String) {
        database.child(teacherId).removeValue()
    }
}
