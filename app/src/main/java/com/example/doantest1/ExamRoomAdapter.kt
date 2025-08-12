package com.example.doantest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent

class ExamRoomAdapter(
    private val rooms: List<ExamRoom>,
    private val examPeriodId: String,
    private val examDateId: String,
    private val examsessionId: String,
    private val activity: ManageExamRoomsActivity
) : RecyclerView.Adapter<ExamRoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRoomName: TextView = itemView.findViewById(R.id.txtRoomName)
        val txtTeacherName: TextView = itemView.findViewById(R.id.txtTeacherName)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditRoom)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteRoom)
        val btnAssignTeacher: Button = itemView.findViewById(R.id.btnAssignTeacher)
        val btnAssignStudents: Button = itemView.findViewById(R.id.btnAssignStudents)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.txtRoomName.text = room.name
        holder.txtTeacherName.text = room.teacherName ?: ""

        holder.btnEdit.setOnClickListener {
            activity.showAddEditRoomDialog(room, room.id)
        }

        holder.btnDelete.setOnClickListener {
            activity.deleteRoom(room.id)
        }

        holder.btnAssignTeacher.setOnClickListener {
            activity.assignTeacherToRoom(room.id)
        }

        holder.btnAssignStudents.setOnClickListener {
            val context = holder.itemView.context
            if (context is ManageExamRoomsActivity) {
                context.assignStudentsToRoom(room.id)
            }
        }

        // TODO: Thêm sự kiện vào phòng thi → mở danh sách sinh viên & điểm danh
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RoomAttendanceActivity::class.java)
            intent.putExtra("EXAM_PERIOD_ID", examPeriodId)
            intent.putExtra("EXAM_DATE_ID", examDateId)
            intent.putExtra("EXAM_SESSION_ID", examsessionId)
            intent.putExtra("ROOM_ID", room.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rooms.size
}
