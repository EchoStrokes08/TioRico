package com.example.tiorico.data.repository

// ═══════════════════════════════════════════════════════════════════════════════
// DataResult
//
// Wrapper que usan TODOS los repositorios para retornar resultados.
// El ViewModel nunca recibe excepciones crudas, siempre un DataResult.
//
// Uso en repositorio:
//   return safeCall { firestore.collection(...).get().await() }
//
// Uso en ViewModel:
//   when (result) {
//       is DataResult.Success -> { result.data }
//       is DataResult.Error   -> { result.message }
//   }
// ═══════════════════════════════════════════════════════════════════════════════

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val message: String) : DataResult<Nothing>()
}

suspend fun <T> safeCall(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: Exception) {
    DataResult.Error(e.message ?: "Error desconocido")
}
