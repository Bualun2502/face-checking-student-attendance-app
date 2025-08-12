package com.example.doantest1

data class Student(
    var maSinhVien: String = "",
    var hoTen: String = "",
    var ngaySinh: String = "",
    var sdt: String = "",
    var email: String = "",
    var faceIDs: List<String> = emptyList()
)

