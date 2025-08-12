package com.example.doantest1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.sqrt
import android.view.View

class FaceCheckActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var statusIcon: ImageView
    private val storedStudents = mutableListOf<Student>()
    private val REQUEST_IMAGE_CAPTURE = 1

    /* ---------- ID phòng thi truyền từ RoomAttendanceActivity ---------- */
    private lateinit var examPeriodId:  String
    private lateinit var examDateId:    String
    private lateinit var examSessionId: String
    private lateinit var roomId:        String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_check)

        /* ---- lấy ID ---- */
        examPeriodId  = intent.getStringExtra("EXAM_PERIOD_ID")  ?: ""
        examDateId    = intent.getStringExtra("EXAM_DATE_ID")    ?: ""
        examSessionId = intent.getStringExtra("EXAM_SESSION_ID") ?: ""
        roomId        = intent.getStringExtra("ROOM_ID")         ?: ""

        imageView = findViewById(R.id.imageView)
        resultTextView = findViewById(R.id.resultTextView)
        statusIcon = findViewById(R.id.statusIcon)
        //val btnCapture: Button = findViewById(R.id.btnCapture)

        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<Button>(R.id.btnFinishCheck).setOnClickListener {
            finish() // Trở về RoomAttendanceActivity
        }

        loadStudentInfoInRoom()
    }

    /** Lấy studentIds trong exam_rooms/…/roomId/studentIds → tải thông tin */
    private fun loadStudentInfoInRoom() {                          //  NEW
        val root = FirebaseDatabase.getInstance().reference
        val roomRef = root.child("exam_rooms")
            .child(examPeriodId).child(examDateId)
            .child(examSessionId).child(roomId)

        roomRef.child("studentIds").get().addOnSuccessListener { idSnap ->
            val idList = idSnap.children.mapNotNull { it.getValue(String::class.java) }
            if (idList.isEmpty()) {
                resultTextView.text = "Phòng thi chưa có sinh viên."
                return@addOnSuccessListener
            }
            // Tải thông tin từng sinh viên
            root.child("students").get().addOnSuccessListener { stuSnap ->
                storedStudents.clear()
                for (sid in idList) {
                    val stu = stuSnap.child(sid).getValue(Student::class.java)
                    stu?.let { storedStudents.add(it) }
                }
                resultTextView.text = "Đã tải ${storedStudents.size} sinh viên trong phòng."
            }
        }.addOnFailureListener {
            resultTextView.text = "Lỗi tải sinh viên trong phòng."
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(packageManager)?.also {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
            processFaceDetection(imageBitmap)
        }
    }

    private fun processFaceDetection(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    extractFaceEmbedding(imageBitmap, face) { embedding ->
                        if (embedding != null) {
                            compareFace(embedding)
                        }
                    }
                } else {
                    resultTextView.text = "Không tìm thấy khuôn mặt."
                }
            }
    }

    private fun extractFaceEmbedding(bitmap: Bitmap, face: Face, callback: (FloatArray?) -> Unit) {
        val faceEmbedding = FloatArray(128) { Math.random().toFloat() }
        callback(faceEmbedding)
    }

    private fun compareFace(newEmbedding: FloatArray) {
        if (storedStudents.isEmpty()) {
            resultTextView.text = "Chưa có dữ liệu sinh viên trong phòng."
            return
        }
        val normalizedNew = normalizeEmbedding(newEmbedding)
        var bestMatch: Student? = null
        var minDist = Float.MAX_VALUE
        val threshold = 0.75f

        storedStudents.forEach { stu ->
            stu.faceIDs.forEach { embStr ->
                val saved = embStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                if (saved.isEmpty()) return@forEach
                val dist = euclideanDistance(normalizedNew, normalizeEmbedding(saved))
                if (dist < minDist) {
                    minDist = dist
                    bestMatch = stu
                }
            }
        }

        if (bestMatch != null && minDist < threshold) {
            val s = bestMatch!!
            resultTextView.text = """
        Họ tên     : ${s.hoTen}
        MSSV       : ${s.maSinhVien}
        Ngày sinh  : ${s.ngaySinh}
        SĐT        : ${s.sdt}
        Email      : ${s.email}
    """.trimIndent()

            // Hiện icon trạng thái "có mặt"
            statusIcon.visibility = View.VISIBLE
            // Lưu trạng thái "present" về Firebase
            val attendanceRef = FirebaseDatabase.getInstance().reference
                .child("exam_rooms")
                .child(examPeriodId)
                .child(examDateId)
                .child(examSessionId)
                .child(roomId)
                .child("studentStatuses")
                .child(s.maSinhVien)

            val statusData = mapOf(
                "trangThai" to "present",
                "ghiChu" to ""
            )
            attendanceRef.setValue(statusData)
        } else {
            resultTextView.text = "Không tìm thấy sinh viên."
        }
    }

        private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        return if (norm > 0) embedding.map { it / norm }.toFloatArray() else embedding
    }

    private fun euclideanDistance(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) return Float.MAX_VALUE
        var sum = 0.0
        for (i in vec1.indices) {
            sum += (vec1[i] - vec2[i]).toDouble() * (vec1[i] - vec2[i]).toDouble()
        }
        return sqrt(sum).toFloat()
    }
}
