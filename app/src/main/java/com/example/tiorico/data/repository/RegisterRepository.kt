package com.example.tiorico.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RegisterRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Error UID"))

            val userMap = mapOf(
                "id" to uid,
                "username" to username,
                "email" to email
            )

            db.child("users").child(uid).setValue(userMap).await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}