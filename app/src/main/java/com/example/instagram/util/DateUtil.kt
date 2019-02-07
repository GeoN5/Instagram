package com.example.instagram.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    fun formatDate(date: Date): String {
        //바꿀 포맷 형식으로 객체 생성.
        val simpleDataFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        //format메소드를 통해 바꿔서 리턴.
        return simpleDataFormat.format(date)
    }
}
