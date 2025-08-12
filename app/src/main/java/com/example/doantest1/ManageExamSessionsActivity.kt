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



class ManageExamSessionsActivity : AppCompatActivity() {

    private lateinit var sessionRecyclerView: RecyclerView
    private lateinit var sessionList: MutableList<ExamSession>
    private lateinit var sessionAdapter: ExamSessionAdapter
    private lateinit var btnAddSession: Button
    private lateinit var database: DatabaseReference

    private lateinit var examPeriodId: String
    private lateinit var examDateId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_exam_sessions)

        sessionRecyclerView = findViewById(R.id.recyclerViewSessions)
        btnAddSession = findViewById(R.id.btnAddSession)

        examPeriodId = intent.getStringExtra("EXAM_PERIOD_ID") ?: ""
        examDateId = intent.getStringExtra("EXAM_DATE_ID") ?: ""

        if (examPeriodId.isEmpty() || examDateId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin kỳ thi hoặc ngày thi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        sessionList = mutableListOf()

        sessionAdapter = ExamSessionAdapter(sessionList, examPeriodId, examDateId, this) { session ->
            val intent = Intent(this, ManageExamRoomsActivity::class.java)
            intent.putExtra("EXAM_PERIOD_ID", examPeriodId)
            intent.putExtra("EXAM_DATE_ID", examDateId)
            intent.putExtra("SESSION_ID", session.id)
            startActivity(intent)
        }

        // ✅ Gán adapter và layoutManager sau khi đã khởi tạo RecyclerView
        sessionRecyclerView.layoutManager = LinearLayoutManager(this)
        sessionRecyclerView.adapter = sessionAdapter

        database = FirebaseDatabase.getInstance().getReference("exam_periods")
            .child(examPeriodId)
            .child("dates")
            .child(examDateId)
            .child("sessions")

        loadSessions()

        btnAddSession.setOnClickListener {
            showAddEditDialog(null, null)
        }
    }

    fun loadSessions() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sessionList.clear()
                for (sessionSnapshot in snapshot.children) {
                    val session = sessionSnapshot.getValue(ExamSession::class.java)
                    session?.let {
                        it.id = sessionSnapshot.key ?: ""
                        sessionList.add(it)
                    }
                }
                sessionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamSessionsActivity, "Lỗi tải dữ liệu ca thi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showAddEditDialog(sessionToEdit: ExamSession?, sessionId: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exam_session, null)
        val etSessionName = dialogView.findViewById<EditText>(R.id.etSessionName)
        val etSessionTime = dialogView.findViewById<EditText>(R.id.etSessionTime)

        if (sessionToEdit != null) {
            etSessionName.setText(sessionToEdit.name)
            etSessionTime.setText(sessionToEdit.time)
        }

        AlertDialog.Builder(this)
            .setTitle(if (sessionToEdit == null) "Thêm ca thi" else "Sửa ca thi")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = etSessionName.text.toString()
                val time = etSessionTime.text.toString()

                val id = sessionId ?: database.push().key!!
                val newSession = ExamSession(id, name, time)
                database.child(id).setValue(newSession)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun deleteSession(sessionId: String) {
        val db = FirebaseDatabase.getInstance().reference

        val updates = hashMapOf<String, Any?>(
            "/exam_periods/$examPeriodId/dates/$examDateId/sessions/$sessionId" to null,
            "/exam_rooms/$examPeriodId/$examDateId/$sessionId" to null
        )

        db.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Đã xoá ca thi và các phòng thi bên trong", Toast.LENGTH_SHORT).show()
            loadSessions()
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi khi xoá ca thi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
