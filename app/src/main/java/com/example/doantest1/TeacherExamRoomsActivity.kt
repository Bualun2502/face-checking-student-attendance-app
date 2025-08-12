package com.example.doantest1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class TeacherExamRoomsActivity : AppCompatActivity() {

    /* ------------------- view & adapter ------------------- */
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TeacherExamRoomAdapter
    private lateinit var txtGreeting: TextView
    private val roomList = mutableListOf<TeacherRoomInfo>()

    /* ------------------- firebase & date ------------------ */
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var teacherId: String

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val todayDate: Date by lazy {
        dateFormatter.parse(dateFormatter.format(Date()))!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_exam_rooms)

        /* ---------- hiển thị HÔM NAY ---------- */
        findViewById<TextView>(R.id.txtTodayDate).text =
            "Hôm nay: ${dateFormatter.format(todayDate)}"

        /* ---------- recyclerview & adapter& greeting ---------- */
        txtGreeting = findViewById(R.id.txtGreeting)
        recyclerView = findViewById(R.id.recyclerViewTeacherRooms)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TeacherExamRoomAdapter(roomList) { info ->
            Intent(this, RoomAttendanceActivity::class.java).apply {
                putExtra("examPeriodId", info.examPeriodId)
                putExtra("examDateId", info.examDateId)
                putExtra("examSessionId", info.examSessionId)
                putExtra("roomId", info.roomId)
            }.also { startActivity(it) }
        }
        recyclerView.adapter = adapter

        /* ---------- đổi mật khẩu ---------- */
        findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            val editText = android.widget.EditText(this)
            editText.hint = "Nhập mật khẩu mới"
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(editText)
                .setPositiveButton("Xác nhận") { _, _ ->
                    val newPassword = editText.text.toString().trim()
                    if (newPassword.length < 6) {
                        Toast.makeText(this, "Mật khẩu phải từ 6 ký tự", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }


        /* ---------- logout ---------- */
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        /* ---------- tìm teacherId theo email ---------- */
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email == null) {
            Toast.makeText(this, "Không tìm thấy email giáo viên", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        database.child("teachers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var found = false
                    for (tSnap in snapshot.children) {
                        if (tSnap.child("email").getValue(String::class.java) == email) {
                            teacherId = tSnap.key ?: ""
                            val teacherName = tSnap.child("name").getValue(String::class.java) ?: "Giáo viên"
                            txtGreeting.text = "Xin chào $teacherName"
                            found = true
                            lifecycleScope.launch(Dispatchers.IO) {
                                loadTeacherRooms()
                            }
                            break
                        }
                    }
                    if (!found) {
                        Toast.makeText(
                            this@TeacherExamRoomsActivity,
                            "Không tìm thấy giáo viên $email",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@TeacherExamRoomsActivity,
                        "Lỗi tải dữ liệu giáo viên",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            })
    }

    /* ====================================================== */
    private suspend fun loadTeacherRooms() {
        val parsedDateMap = mutableMapOf<String, Date>()
        val tempList = mutableListOf<TeacherRoomInfo>()

        val rootSnap = database.child("exam_rooms").get().await()
        for (periodSnap in rootSnap.children) {
            val periodId = periodSnap.key ?: continue

            val periodName = database.child("exam_periods")
                .child(periodId).child("name")
                .getValueAsync() ?: periodId

            for (dateSnap in periodSnap.children) {
                val dateId = dateSnap.key ?: continue
                for (sessionSnap in dateSnap.children) {
                    val sessionId = sessionSnap.key ?: continue

                    val sessionName = database.child("exam_periods")
                        .child(periodId).child("dates")
                        .child(dateId).child("sessions")
                        .child(sessionId).child("name")
                        .getValueAsync() ?: sessionId

                    for (roomSnap in sessionSnap.children) {
                        val roomId = roomSnap.key ?: continue
                        if (roomSnap.child("teacherId")
                                .getValue(String::class.java) != teacherId) continue

                        val roomName = roomSnap.child("name")
                            .getValue(String::class.java) ?: "Phòng ?"

                        val examDateStr = database.child("exam_dates")
                            .child(periodId).child(dateId).child("date")
                            .getValueAsync() ?: dateId

                        val examDate: Date? = try {
                            dateFormatter.parse(examDateStr)
                        } catch (e: ParseException) { null }

                        if (examDate != null && !examDate.before(todayDate)) {
                            parsedDateMap[examDateStr] = examDate
                            tempList.add(
                                TeacherRoomInfo(
                                    roomId = roomId,
                                    roomName = roomName,
                                    examPeriodId = periodId,
                                    examDateId = dateId,
                                    examSessionId = sessionId,
                                    examPeriodName = periodName,
                                    examDate = examDateStr,
                                    examSessionName = sessionName
                                )
                            )
                        }
                    }
                }
            }
        }

        runOnUiThread {
            roomList.clear()
            roomList.addAll(tempList.sortedBy { parsedDateMap[it.examDate] })
            adapter.notifyDataSetChanged()

            if (roomList.isEmpty()) {
                Toast.makeText(this@TeacherExamRoomsActivity,
                    "Không có phòng nào được phân công từ hôm nay trở đi",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }


    /* ----------------- tiện ích blocking đơn giản ----------------- */
    private suspend fun DatabaseReference.getValueAsync(): String? {
        return try {
            this.get().await().getValue(String::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
