package com.example.doantest1

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import android.util.Log
import android.content.Intent
import android.widget.Button
import android.widget.ImageButton
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView

class RoomAttendanceActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAttendanceAdapter
    private lateinit var btnFaceCheck: ImageButton
    private val studentList = mutableListOf<StudentAttendance>()

    private lateinit var examPeriodId: String
    private lateinit var examDateId: String
    private lateinit var examSessionId: String
    private lateinit var roomId: String

    private lateinit var statusRoot: DatabaseReference

    private var invigilatorName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_attendance)

        examPeriodId = intent.getStringExtra("EXAM_PERIOD_ID") ?: ""
        examDateId = intent.getStringExtra("EXAM_DATE_ID") ?: ""
        examSessionId = intent.getStringExtra("EXAM_SESSION_ID") ?: ""
        roomId = intent.getStringExtra("ROOM_ID") ?: ""

        Log.d("FIREBASE_PATH", "Path: /exam_rooms/$examPeriodId/$examDateId/$examSessionId/$roomId")

        statusRoot = FirebaseDatabase.getInstance().reference
            .child("exam_rooms")
            .child(examPeriodId)
            .child(examDateId)
            .child(examSessionId)
            .child(roomId)
            .child("studentStatuses")

        recyclerView = findViewById(R.id.recyclerViewAttendance)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAttendanceAdapter(studentList, statusRoot)
        recyclerView.adapter = adapter

        btnFaceCheck = findViewById(R.id.btnFaceCheck)
        btnFaceCheck.setOnClickListener {
            openFaceCheckActivity()
        }

        val btnExportPdf: Button = findViewById(R.id.btnExportPdf)
        btnExportPdf.setOnClickListener {
            fetchExamDetails { periodName, date, sessionName, roomName ->
                exportAttendanceToPDF(periodName, date, sessionName, roomName)
            }
        }

        fetchInvigilatorName()
        loadStudentIdsFromRoom()
    }

    private fun fetchInvigilatorName() {
        val roomRef = FirebaseDatabase.getInstance().getReference("exam_rooms")
            .child(examPeriodId)
            .child(examDateId)
            .child(examSessionId)
            .child(roomId)

        roomRef.child("teacherName").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                invigilatorName = snapshot.getValue(String::class.java) ?: ""
                findViewById<TextView>(R.id.txtInvigilator)?.text = "Giám thị coi thi: $invigilatorName"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadStudentIdsFromRoom() {
        val roomRef = FirebaseDatabase.getInstance().getReference("exam_rooms")
            .child(examPeriodId)
            .child(examDateId)
            .child(examSessionId)
            .child(roomId)

        roomRef.child("studentIds").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val studentIds = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                if (studentIds.isEmpty()) {
                    Toast.makeText(this@RoomAttendanceActivity, "Không có sinh viên trong phòng", Toast.LENGTH_SHORT).show()
                } else {
                    loadStudentInfo(studentIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RoomAttendanceActivity, "Lỗi khi tải danh sách sinh viên", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadStudentInfo(ids: List<String>) {
        val root = FirebaseDatabase.getInstance().reference

        root.child("students").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(studentSnap: DataSnapshot) {
                statusRoot.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(statusSnap: DataSnapshot) {
                        studentList.clear()
                        for (id in ids) {
                            val stu = studentSnap.child(id).getValue(Student::class.java) ?: continue
                            val statusNode = statusSnap.child(id)
                            val trangThai = statusNode.child("trangThai").getValue(String::class.java) ?: "absent"
                            val ghiChu = statusNode.child("ghiChu").getValue(String::class.java) ?: ""

                            studentList.add(
                                StudentAttendance(
                                    maSinhVien = id,
                                    hoTen = stu.hoTen ?: "",
                                    trangThai = trangThai,
                                    ghiChu = ghiChu
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RoomAttendanceActivity, "Lỗi khi tải thông tin sinh viên", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openFaceCheckActivity() {
        val intent = Intent(this, FaceCheckActivity::class.java)
        intent.putExtra("EXAM_PERIOD_ID", examPeriodId)
        intent.putExtra("EXAM_DATE_ID", examDateId)
        intent.putExtra("EXAM_SESSION_ID", examSessionId)
        intent.putExtra("ROOM_ID", roomId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadStudentIdsFromRoom()
    }

    private fun fetchExamDetails(
        onComplete: (examPeriodName: String, examDate: String, examSessionName: String, roomName: String) -> Unit
    ) {
        val db = FirebaseDatabase.getInstance().reference

        var examPeriodName = "Kỳ thi"
        var examDateText = "Ngày thi"
        var examSessionName = "Ca thi"
        var roomName = "Phòng thi"

        db.child("exam_periods").child(examPeriodId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    examPeriodName = snapshot.getValue(String::class.java) ?: examPeriodName

                    db.child("exam_dates").child(examPeriodId).child(examDateId).child("date")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dateSnap: DataSnapshot) {
                                examDateText = dateSnap.getValue(String::class.java) ?: examDateText

                                db.child("exam_periods").child(examPeriodId)
                                    .child("dates").child(examDateId)
                                    .child("sessions").child(examSessionId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(sessionSnap: DataSnapshot) {
                                            examSessionName = sessionSnap.child("name").getValue(String::class.java) ?: examSessionName

                                            db.child("exam_rooms").child(examPeriodId)
                                                .child(examDateId)
                                                .child(examSessionId)
                                                .child(roomId)
                                                .child("name")
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(roomSnap: DataSnapshot) {
                                                        roomName = roomSnap.getValue(String::class.java) ?: roomName
                                                        onComplete(examPeriodName, examDateText, examSessionName, roomName)
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {}
                                                })
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun exportAttendanceToPDF(
        examPeriodName: String,
        examDate: String,
        examSessionName: String,
        roomName: String
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 60
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BÁO CÁO ĐIỂM DANH PHÒNG THI", 150f, y.toFloat(), paint)

        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        y += 40
        canvas.drawText("Kỳ thi      : $examPeriodName", 40f, y.toFloat(), paint)
        y += 25
        canvas.drawText("Ngày thi    : $examDate", 40f, y.toFloat(), paint)
        y += 25
        canvas.drawText("Ca thi      : $examSessionName", 40f, y.toFloat(), paint)
        y += 25
        canvas.drawText("Phòng thi   : $roomName", 40f, y.toFloat(), paint)
        y += 25
        canvas.drawText("Giám thị    : $invigilatorName", 40f, y.toFloat(), paint)

        y += 40
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("MSSV           Họ tên                        Trạng thái     Ghi chú", 40f, y.toFloat(), paint)
        y += 20

        studentList.forEachIndexed { index, stu ->
            val status = if (stu.trangThai == "present") "Có mặt" else "Vắng mặt"
            val line = "${stu.maSinhVien.padEnd(15)}" +
                    "${stu.hoTen.padEnd(30)}" +
                    "${status.padEnd(15)}" +
                    stu.ghiChu
            canvas.drawText(line, 40f, y.toFloat(), paint)
            y += 20
        }

        pdfDocument.finishPage(page)

        val pdfDir = getExternalFilesDir("pdf")
        if (pdfDir != null && !pdfDir.exists()) pdfDir.mkdirs()

        val file = File(pdfDir, "DiemDanh_${System.currentTimeMillis()}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(this, "Đã lưu PDF tại: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
