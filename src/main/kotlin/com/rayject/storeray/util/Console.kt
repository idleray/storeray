package com.rayject.storeray.util

object Console {
    fun info(message: String) {
        println("ℹ️ $message")
    }

    fun success(message: String) {
        println("✅ $message")
    }

    fun warning(message: String) {
        println("⚠️ $message")
    }

    fun error(message: String) {
        println("❌ $message")
    }

    fun divider() {
        println("=".repeat(60))
    }

    fun step(message: String) {
        println("\n🚀 $message")
        divider()
    }

    fun detail(message: String) {
        println("   $message")
    }
}
