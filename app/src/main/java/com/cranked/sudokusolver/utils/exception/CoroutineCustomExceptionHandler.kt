package com.cranked.sudokusolver.utils

import kotlinx.coroutines.CoroutineExceptionHandler

object CoroutineCustomExceptionHandler {
    val handler = CoroutineExceptionHandler { _, exception ->

        println("Yakalanamyan Hata${exception.stackTraceToString()}")
    }
}