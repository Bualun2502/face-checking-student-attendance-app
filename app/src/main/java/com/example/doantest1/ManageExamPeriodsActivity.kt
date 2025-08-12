package com.example.doantest1

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import android.content.Intent


class ManageExamPeriodsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExamPeriodAdapter
    private lateinit var examPeriods: MutableList<ExamPeriod>
    private val database = FirebaseDatabase.getInstance().getReference("exam_periods")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_exam_periods)

        recyclerView = findViewById(R.id.recyclerExamPeriods)
        findViewById<Button>(R.id.btnAddExamPeriod).setOnClickListener {
            showExamPeriodDialog(null)
        }

        examPeriods = mutableListOf()
        adapter = ExamPeriodAdapter(
            examPeriods,
            ::showExamPeriodDialog,
            ::deleteExamPeriod,
            ::openExamDates // thêm hàm mới để mở ngày thi
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadExamPeriods()
    }

    private fun loadExamPeriods() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                examPeriods.clear()
                for (child in snapshot.children) {
                    val exam = child.getValue(ExamPeriod::class.java)
                    exam?.let {
                        it.id = child.key ?: "" // 🔧 Thêm dòng này
                        examPeriods.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamPeriodsActivity, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showExamPeriodDialog(examToEdit: ExamPeriod?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exam_period, null)
        val edtName = dialogView.findViewById<EditText>(R.id.etExamName)
        val edtDesc = dialogView.findViewById<EditText>(R.id.etExamDesc)

        if (examToEdit != null) {
            edtName.setText(examToEdit.name)
            edtDesc.setText(examToEdit.description)
        }

        AlertDialog.Builder(this)
            .setTitle(if (examToEdit == null) "Thêm kỳ thi" else "Sửa kỳ thi")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = edtName.text.toString().trim()
                val desc = edtDesc.text.toString().trim()

                val id = examToEdit?.id ?: database.push().key!!

                if (examToEdit == null) {
                    // Thêm mới
                    val newExam = ExamPeriod(id, name, desc)
                    database.child(id).setValue(newExam)
                } else {
                    // Cập nhật tên/mô tả – KHÔNG ghi đè node
                    val updates = mapOf(
                        "name" to name,
                        "description" to desc
                    )
                    database.child(id).updateChildren(updates)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }


    private fun deleteExamPeriod(exam: ExamPeriod) {
        val db = FirebaseDatabase.getInstance().reference
        val examPeriodId = exam.id

        val updates = hashMapOf<String, Any?>(
            "/exam_periods/$examPeriodId" to null,
            "/exam_dates/$examPeriodId" to null,
            "/exam_rooms/$examPeriodId" to null
        )

        db.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Đã xoá kỳ thi và dữ liệu liên quan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi xoá dữ liệu: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExamDates(exam: ExamPeriod) {
        val intent = Intent(this, ManageExamDatesActivity::class.java)
        intent.putExtra("EXAM_PERIOD_ID", exam.id)
        startActivity(intent)
    }

}
