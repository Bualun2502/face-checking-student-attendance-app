package com.example.doantest1

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ShowStudentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var tvNoResults: TextView
    private lateinit var btnReset: Button
    private val students = mutableListOf<Student>()
    private val database = FirebaseDatabase.getInstance().getReference("students")
    private lateinit var adapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_students)

        recyclerView = findViewById(R.id.studentListView)
        searchEditText = findViewById(R.id.searchEditText)
        tvNoResults = findViewById(R.id.tvNoResults)
        btnReset = findViewById(R.id.btnReset)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(students, ::onEditStudent, ::onDeleteStudent)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val query = searchEditText.text.toString()
            searchStudent(query)
        }
        btnReset.setOnClickListener {
            searchEditText.setText("")
            loadStudents()  // Tải lại danh sách đầy đủ
            
        }

        loadStudents()
    }

    private fun loadStudents() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                students.clear()
                snapshot.children.mapNotNullTo(students) { it.getValue(Student::class.java) }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShowStudentActivity, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchStudent(query: String) {
        val filtered = students.filter {
            it.maSinhVien.contains(query, ignoreCase = true) ||
                    it.hoTen.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
        tvNoResults.visibility = if (filtered.isEmpty()) TextView.VISIBLE else TextView.GONE
    }

    private fun onEditStudent(student: Student) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_student, null)
        val etName = dialogView.findViewById<EditText>(R.id.editName)
        val etPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val etEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val etBirth = dialogView.findViewById<EditText>(R.id.editBirth)

        etName.setText(student.hoTen)
        etPhone.setText(student.sdt)
        etEmail.setText(student.email)
        etBirth.setText(student.ngaySinh)

        AlertDialog.Builder(this)
            .setTitle("Sửa thông tin")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedStudent = student.copy(
                    hoTen = etName.text.toString(),
                    sdt = etPhone.text.toString(),
                    email = etEmail.text.toString(),
                    ngaySinh = etBirth.text.toString()
                )
                database.child(student.maSinhVien).setValue(updatedStudent)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã cập nhật sinh viên", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun onDeleteStudent(student: Student) {
        database.child(student.maSinhVien).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Đã xóa sinh viên", Toast.LENGTH_SHORT).show()
        }
    }
}
