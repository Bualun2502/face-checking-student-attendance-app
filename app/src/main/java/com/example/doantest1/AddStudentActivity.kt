package com.example.doantest1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class AddStudentActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var maSinhVienEditText: EditText
    private lateinit var hoTenEditText: EditText
    private lateinit var ngaySinhEditText: EditText
    private lateinit var sdtEditText: EditText
    private lateinit var emailEditText: EditText
    private val capturedImages = mutableListOf<String>()
    private val REQUEST_IMAGE_CAPTURE = 1
    private val database = FirebaseDatabase.getInstance().getReference("students")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        imageView = findViewById(R.id.imageView)
        resultTextView = findViewById(R.id.resultTextView)
        maSinhVienEditText = findViewById(R.id.maSinhVienEditText)
        hoTenEditText = findViewById(R.id.hoTenEditText)
        ngaySinhEditText = findViewById(R.id.ngaySinhEditText)
        sdtEditText = findViewById(R.id.sdtEditText)
        emailEditText = findViewById(R.id.emailEditText)

        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveStudentInfo()
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
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build()
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    extractFaceEmbedding(imageBitmap, faces[0]) { embedding ->
                        embedding?.let {
                            if (capturedImages.size < 3) {
                                capturedImages.add(it.joinToString(","))
                                resultTextView.text = "Đã lưu ảnh ${capturedImages.size}/3"
                            } else {
                                resultTextView.text = "Đã đủ 3 ảnh."
                            }
                        }
                    }
                } else {
                    resultTextView.text = "Không phát hiện khuôn mặt."
                }
            }
    }

    private fun extractFaceEmbedding(bitmap: Bitmap, face: Face, callback: (FloatArray?) -> Unit) {
        val embedding = FloatArray(128) { Math.random().toFloat() }
        callback(embedding)
    }

    private fun saveStudentInfo() {
        val student = Student(
            maSinhVienEditText.text.toString(),
            hoTenEditText.text.toString(),
            ngaySinhEditText.text.toString(),
            sdtEditText.text.toString(),
            emailEditText.text.toString(),
            capturedImages.toList()
        )

        if (student.maSinhVien.isNotEmpty() && student.hoTen.isNotEmpty() && capturedImages.size == 3) {
            database.child(student.maSinhVien).setValue(student).addOnSuccessListener {
                resultTextView.text = "Lưu thành công."
                clearInputs()
            }
        } else {
            resultTextView.text = "Điền đủ thông tin và 3 ảnh."
        }
    }

    private fun clearInputs() {
        maSinhVienEditText.text.clear()
        hoTenEditText.text.clear()
        ngaySinhEditText.text.clear()
        sdtEditText.text.clear()
        emailEditText.text.clear()
        imageView.setImageResource(0)
        capturedImages.clear()
    }
}
