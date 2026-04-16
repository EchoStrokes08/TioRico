package com.example.tiorico.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseUser
class AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userEmail = result.user?.email ?: "Usuario desconocido"
            Result.success(userEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userEmail = result.user?.email ?: "Usuario desconocido"
            Result.success(userEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }


    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }
    suspend fun getUserData(): Pair<String, String> {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""

        val snapshot = com.google.firebase.database.FirebaseDatabase
            .getInstance()
            .reference
            .child("users")
            .child(userId)
            .get()
            .await()

        val username = snapshot.child("username")
            .getValue(String::class.java) ?: ""

        return Pair(userId, username)
    }

}