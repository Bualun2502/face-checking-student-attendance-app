package com.example.doantest1

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import android.app.DatePickerDialog
import java.util.Calendar

class ManageExamDatesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExamDateAdapter
    private lateinit var examDates: MutableList<ExamDate>
    private lateinit var btnAddDate: Button
    private lateinit var database: DatabaseReference
    private lateinit var examPeriodId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_exam_dates)

        recyclerView = findViewById(R.id.recyclerViewExamDates)
        btnAddDate = findViewById(R.id.btnAddExamDate)

        examPeriodId = intent.getStringExtra("EXAM_PERIOD_ID") ?: ""
        if (examPeriodId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy kỳ thi", Toast.LENGTH_SHORT).show()
            finish()
        }

        database = FirebaseDatabase.getInstance().getReference("exam_dates").child(examPeriodId)

        examDates = mutableListOf()
        adapter = ExamDateAdapter(examDates, ::showDateDialog, ::deleteDate, examPeriodId)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAddDate.setOnClickListener {
            showDateDialog(null)
        }

        loadExamDates()
    }

    private fun loadExamDates() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                examDates.clear()
                for (child in snapshot.children) {
                    val date = child.getValue(ExamDate::class.java)
                    date?.let { examDates.add(it) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamDatesActivity, "Lỗi tải ngày thi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDateDialog(dateToEdit: ExamDate?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_exam_date, null)
        val edtDate = view.findViewById<EditText>(R.id.etExamDate)

        if (dateToEdit != null) {
            edtDate.setText(dateToEdit.date)
        }

        // Mở lịch khi click vào EditText
        edtDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val dateStr = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                    edtDate.setText(dateStr)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (dateToEdit == null) "Thêm ngày thi" else "Sửa ngày thi")
            .setView(view)
            .setPositiveButton("Lưu") { _, _ ->
                val dateText = edtDate.text.toString()
                val id = dateToEdit?.id ?: database.push().key!!
                val date = ExamDate(id, dateText)
                database.child(id).setValue(date)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }


    private fun deleteDate(date: ExamDate) {
        val examPeriodId = intent.getStringExtra("EXAM_PERIOD_ID") ?: return

        val db = FirebaseDatabase.getInstance().reference

        // Xóa các nhánh:
        // - exam_periods/{examPeriodId}/dates/{date.id}
        // - exam_rooms/{examPeriodId}/{date.id}

        val updates = hashMapOf<String, Any?>(
            "/exam_periods/$examPeriodId/dates/${date.id}" to null,         // ca thi (sessions)
            "/exam_dates/$examPeriodId/${date.id}" to null,                 // ngày thi
            "/exam_rooms/$examPeriodId/${date.id}" to null                  // phòng thi thuộc ngày
        )

        db.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Đã xoá ngày thi và dữ liệu liên quan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi khi xoá: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

}