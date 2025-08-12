package com.example.doantest1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StudentManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_students)

        val btnAddStudent = findViewById<Button>(R.id.btnAddStudent)
        val btnViewStudents = findViewById<Button>(R.id.btnViewStudents)

        btnAddStudent.setOnClickListener {
            startActivity(Intent(this, AddStudentActivity::class.java))
        }

        btnViewStudents.setOnClickListener {
            startActivity(Intent(this, ShowStudentActivity::class.java))
        }
    }
}
