package com.markduenas.antonymizer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform