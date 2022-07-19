package com.example.mywallet.core.util

fun isInteger(str: String) = str.toIntOrNull()?.let { true } ?: false