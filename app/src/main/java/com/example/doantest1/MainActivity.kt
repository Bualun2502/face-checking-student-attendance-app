package com.example.doantest1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnManageTeachers: Button
    private lateinit var btnManageStudents: Button
    private lateinit var btnManageExamPeriods: Button
    private lateinit var btnLogout: Button
    private lateinit var txtWelcome: TextView
    private lateinit var btnManageAttendanceResults: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        txtWelcome = findViewById(R.id.txtWelcome)
        btnManageTeachers = findViewById(R.id.btnManageTeachers)
        btnManageStudents = findViewById(R.id.btnManageStudents)
        btnManageExamPeriods = findViewById(R.id.btnManageExamPeriods)
        btnManageAttendanceResults = findViewById(R.id.btnManageAttendanceResults)
        btnLogout = findViewById(R.id.btnLogout)

        //hiển thị today date
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        txtWelcome.text = "Xin chào Admin,\nHôm nay: $currentDate"


        btnManageTeachers.setOnClickListener {
            startActivity(Intent(this, ManageTeachersActivity::class.java))
        }

        btnManageStudents.setOnClickListener {
            startActivity(Intent(this, StudentManagementActivity::class.java))
        }

        btnManageExamPeriods.setOnClickListener {
            startActivity(Intent(this, ManageExamPeriodsActivity::class.java))
        }

        // Nút quản lý kết quả điểm danh
        btnManageAttendanceResults.setOnClickListener {
            startActivity(Intent(this, AttendanceResultActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
