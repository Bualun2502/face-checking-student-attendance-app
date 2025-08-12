package com.example.doantest1

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceResultActivity : AppCompatActivity() {
    private lateinit var rvPast: RecyclerView
    private lateinit var rvFuture: RecyclerView
    private lateinit var tvToday: TextView

    private lateinit var db: DatabaseReference
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val today = Calendar.getInstance().time

    private lateinit var pastAdapter: TeacherExamRoomAdapter
    private lateinit var futureAdapter: TeacherExamRoomAdapter

    private val pastList = mutableListOf<TeacherRoomInfo>()
    private val futureList = mutableListOf<TeacherRoomInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_results)

        db = FirebaseDatabase.getInstance().reference

        tvToday = findViewById(R.id.tvToday)
        tvToday.text = "Hôm nay: ${dateFormat.format(today)}"

        rvPast = findViewById(R.id.rvPastRooms)
        rvFuture = findViewById(R.id.rvFutureRooms)

        pastAdapter = TeacherExamRoomAdapter(pastList) { openRoom(it) }
        futureAdapter = TeacherExamRoomAdapter(futureList) { openRoom(it) }

        rvPast.layoutManager = LinearLayoutManager(this)
        rvPast.adapter = pastAdapter

        rvFuture.layoutManager = LinearLayoutManager(this)
        rvFuture.adapter = futureAdapter

        loadAllRooms()
    }

    private fun openRoom(info: TeacherRoomInfo) {
        val intent = Intent(this, RoomAttendanceActivity::class.java)
        intent.putExtra("EXAM_PERIOD_ID", info.examPeriodId)
        intent.putExtra("EXAM_DATE_ID", info.examDateId)
        intent.putExtra("EXAM_SESSION_ID", info.examSessionId)
        intent.putExtra("ROOM_ID", info.roomId)
        startActivity(intent)
    }

    private fun loadAllRooms() {
        CoroutineScope(Dispatchers.IO).launch {
            val rootSnap = db.child("exam_rooms").get().await()
            for (periodSnap in rootSnap.children) {
                val periodId = periodSnap.key ?: continue
                val periodName = db.child("exam_periods").child(periodId).child("name").getValueAsync() ?: periodId

                for (dateSnap in periodSnap.children) {
                    val dateId = dateSnap.key ?: continue
                    val examDateStr = db.child("exam_dates").child(periodId).child(dateId).child("date").getValueAsync() ?: dateId
                    val examDate: Date? = try {
                        dateFormat.parse(examDateStr)
                    } catch (e: Exception) {
                        null
                    }

                    for (sessionSnap in dateSnap.children) {
                        val sessionId = sessionSnap.key ?: continue
                        val sessionName = db.child("exam_periods").child(periodId)
                            .child("dates").child(dateId)
                            .child("sessions").child(sessionId).child("name").getValueAsync() ?: sessionId

                        for (roomSnap in sessionSnap.children) {
                            val roomId = roomSnap.key ?: continue
                            val roomName = roomSnap.child("name").getValue(String::class.java) ?: "Phòng ?"

                            val info = TeacherRoomInfo(
                                roomId = roomId,
                                roomName = roomName,
                                examPeriodId = periodId,
                                examDateId = dateId,
                                examSessionId = sessionId,
                                examPeriodName = periodName,
                                examDate = examDateStr,
                                examSessionName = sessionName
                            )

                            if (examDate != null && examDate.before(today)) {
                                pastList.add(info)
                            } else {
                                futureList.add(info)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                pastAdapter.notifyDataSetChanged()
                futureAdapter.notifyDataSetChanged()
            }
        }
    }

    // Extension to get value from Firebase
    private suspend fun DatabaseReference.getValueAsync(): String? {
        return try {
            get().await().getValue(String::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
