package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import android.app.AlertDialog

data class StudentAttendance(
    var maSinhVien: String = "",
    var hoTen: String = "",
    var trangThai: String = "absent",
    var ghiChu:     String  = ""
)

class StudentAttendanceAdapter(
    private val studentList: MutableList<StudentAttendance>,
    private val statusRoot: DatabaseReference
) : RecyclerView.Adapter<StudentAttendanceAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        val tvNote:    TextView = itemView.findViewById(R.id.tvNote)
        val btnToggle: Button = itemView.findViewById(R.id.btnToggleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_attendance, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.tvName.text = "${student.hoTen} (${student.maSinhVien})"
        if (student.trangThai == "present") {
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check_green)
        } else {
            holder.ivStatusIcon.setImageResource(R.drawable.ic_cross_red)
        }
        holder.tvNote.text   = if (student.ghiChu.isBlank()) "—" else student.ghiChu

        holder.btnToggle.text = "Chỉnh sửa"
        holder.btnToggle.setOnClickListener {
            showEditDialog(holder.itemView, student, position)
        }
    }

    /* ----------------------- dialog chỉnh sửa ----------------------- */
    private fun showEditDialog(view: View, student: StudentAttendance, pos: Int) {
        val ctx = view.context
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_attendance, null)

        // radioButton chọn trạng thái
        val rgStatus   = dialogView.findViewById<RadioGroup>(R.id.rgStatus)
        val rbPresent  = dialogView.findViewById<RadioButton>(R.id.rbPresent)
        val rbAbsent   = dialogView.findViewById<RadioButton>(R.id.rbAbsent)

        // EditText ghi chú
        val edtNote    = dialogView.findViewById<EditText>(R.id.edtNote)

        // thiết lập giá trị hiện tại
        if (student.trangThai == "present") rbPresent.isChecked = true else rbAbsent.isChecked = true
        edtNote.setText(student.ghiChu)

        AlertDialog.Builder(ctx)
            .setTitle("Cập nhật điểm danh")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                student.trangThai = if (rgStatus.checkedRadioButtonId == R.id.rbPresent) "present" else "absent"
                student.ghiChu    = edtNote.text.toString()
                notifyItemChanged(pos)
                /* -------- LƯU Firebase ---------- */
                // /exam_rooms/.../studentStatuses/{maSinhVien}/{trangThai, ghiChu}
                statusRoot.child(student.maSinhVien)
                    .setValue(mapOf(
                        "trangThai" to student.trangThai,
                        "ghiChu"    to student.ghiChu
                    ))
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    override fun getItemCount(): Int = studentList.size
}

