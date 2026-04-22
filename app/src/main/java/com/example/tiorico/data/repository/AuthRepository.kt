package com.example.tiorico.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userEmail = result.user?.email ?: "Usuario desconocido"
            Result.success(userEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    //Leer FIRESTORE
    suspend fun getUserData(): Pair<String, String> {
        val user = auth.currentUser
        val userId = user?.uid ?: ""

        val snapshot = db.collection("users")
            .document(userId)
            .get()
            .await()

        val username = snapshot.getString("username") ?: ""

        return Pair(userId, username)
    }
}