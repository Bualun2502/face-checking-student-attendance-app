package com.example.doantest1

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeacherExamRoomAdapter(
    private val rooms: List<TeacherRoomInfo>,
    private val onClick: (TeacherRoomInfo) -> Unit
) : RecyclerView.Adapter<TeacherExamRoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtRoomName: TextView = view.findViewById(R.id.txtTeacherRoomName)
        val txtRoomInfo: TextView = view.findViewById(R.id.txtTeacherRoomInfo)

        init {
            view.setOnClickListener {
                val roomInfo = rooms[adapterPosition]
                val context = view.context
                val intent = Intent(context, RoomAttendanceActivity::class.java).apply {
                    putExtra("EXAM_PERIOD_ID", roomInfo.examPeriodId)
                    putExtra("EXAM_DATE_ID", roomInfo.examDateId)
                    putExtra("EXAM_SESSION_ID", roomInfo.examSessionId)
                    putExtra("ROOM_ID", roomInfo.roomId)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.txtRoomName.text = room.roomName
        holder.txtRoomInfo.text = "${room.examSessionName} - ${room.examDate} - ${room.examPeriodName}"
    }

    override fun getItemCount(): Int = rooms.size
}
