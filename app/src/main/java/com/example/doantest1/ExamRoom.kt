package com.example.doantest1

data class ExamRoom(
    var id: String = "",
    var name: String = "",
    var teacherId: String? = null,
    var teacherName: String? = null,
    var studentIds: MutableList<String> = mutableListOf() // danh sách ID sinh viên
)

