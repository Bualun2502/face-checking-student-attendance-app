package com.example.doantest1

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ManageExamRoomsActivity : AppCompatActivity() {

    private lateinit var roomRecyclerView: RecyclerView
    private lateinit var roomList: MutableList<ExamRoom>
    private lateinit var roomAdapter: ExamRoomAdapter
    private lateinit var database: DatabaseReference
    private lateinit var examSessionId: String
    private lateinit var examPeriodId: String
    private lateinit var examDateId: String
    private lateinit var btnAddRoom: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_exam_rooms)

        roomRecyclerView = findViewById(R.id.recyclerViewRooms)
        btnAddRoom = findViewById(R.id.btnAddRoom)

        examPeriodId = intent.getStringExtra("EXAM_PERIOD_ID") ?: ""
        examDateId = intent.getStringExtra("EXAM_DATE_ID") ?: ""
        examSessionId = intent.getStringExtra("SESSION_ID") ?: ""

        roomList = mutableListOf()
        roomAdapter = ExamRoomAdapter(roomList, examPeriodId, examDateId, examSessionId, this)

        roomRecyclerView.layoutManager = LinearLayoutManager(this)
        roomRecyclerView.adapter = roomAdapter

        database = FirebaseDatabase.getInstance()
            .getReference("exam_rooms")
            .child(examPeriodId)
            .child(examDateId)
            .child(examSessionId)

        loadRooms()

        btnAddRoom.setOnClickListener {
            showAddEditRoomDialog(null, null)
        }
    }

    private fun loadRooms() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                roomList.clear()
                for (roomSnap in snapshot.children) {
                    val room = roomSnap.getValue(ExamRoom::class.java)
                    room?.let {
                        it.id = roomSnap.key!!
                        roomList.add(it)
                    }
                }
                roomAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamRoomsActivity, "Lỗi khi tải danh sách phòng.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showAddEditRoomDialog(room: ExamRoom?, roomId: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exam_room, null)
        val edtRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)

        if (room != null) {
            edtRoomName.setText(room.name)
        }

        AlertDialog.Builder(this)
            .setTitle(if (room == null) "Thêm phòng thi" else "Sửa phòng thi")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val roomName = edtRoomName.text.toString().trim()
                if (roomName.isEmpty()) {
                    Toast.makeText(this, "Tên phòng không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newRoom = ExamRoom(roomId ?: database.push().key!!, roomName, room?.teacherId, room?.teacherName)
                database.child(newRoom.id).setValue(newRoom)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun deleteRoom(roomId: String) {
        database.child(roomId).removeValue()
    }

    fun assignTeacherToRoom(roomId: String) {
        val teacherDb = FirebaseDatabase.getInstance().getReference("teachers")
        teacherDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val teachers = snapshot.children.mapNotNull {
                    it.getValue(Teacher::class.java)?.apply { id = it.key!! }
                }

                if (teachers.isEmpty()) {
                    Toast.makeText(this@ManageExamRoomsActivity, "Chưa có giáo viên nào.", Toast.LENGTH_SHORT).show()
                    return
                }

                val teacherNames = teachers.map { "${it.name} (${it.id})" }.toTypedArray()

                AlertDialog.Builder(this@ManageExamRoomsActivity)
                    .setTitle("Chọn giáo viên coi thi")
                    .setItems(teacherNames) { _, which ->
                        val selectedTeacher = teachers[which]
                        val roomRef = database.child(roomId)
                        roomRef.child("teacherId").setValue(selectedTeacher.id)
                        roomRef.child("teacherName").setValue(selectedTeacher.name)
                        Toast.makeText(this@ManageExamRoomsActivity, "Đã gán giáo viên", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamRoomsActivity, "Lỗi khi tải giáo viên", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun assignStudentsToRoom(roomId: String) {
        val studentDb = FirebaseDatabase.getInstance().getReference("students")
        studentDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val students = snapshot.children.mapNotNull {
                    it.getValue(Student::class.java)?.apply { maSinhVien = it.key ?: "" }
                }

                if (students.isEmpty()) {
                    Toast.makeText(this@ManageExamRoomsActivity, "Chưa có sinh viên nào.", Toast.LENGTH_SHORT).show()
                    return
                }

                val selectedStudentIds = mutableListOf<String>()
                val studentNames = students.map { "${it.hoTen} (${it.maSinhVien})" }.toTypedArray()
                val checkedItems = BooleanArray(students.size)

                AlertDialog.Builder(this@ManageExamRoomsActivity)
                    .setTitle("Chọn sinh viên dự thi")
                    .setMultiChoiceItems(studentNames, checkedItems) { _, which, isChecked ->
                        if (isChecked) {
                            selectedStudentIds.add(students[which].maSinhVien)
                        } else {
                            selectedStudentIds.remove(students[which].maSinhVien)
                        }
                    }
                    .setPositiveButton("Gán") { _, _ ->
                        val roomRef = database.child(roomId)
                        roomRef.child("studentIds").setValue(selectedStudentIds)
                        Toast.makeText(this@ManageExamRoomsActivity, "Đã gán sinh viên", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageExamRoomsActivity, "Lỗi khi tải sinh viên", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
