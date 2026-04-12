package com.example.tiorico

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.tiorico.ui.Views.LoginScreen
import com.example.tiorico.ui.Views.RegisterScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tiorico.ui.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiorico.ui.Views.GameScreen
import com.example.tiorico.ui.Views.LobbyScreen
import com.example.tiorico.ui.Views.ResultScreen
import com.example.tiorico.ui.lobby.LobbyViewModel

class MainActivity : ComponentActivity() {



 /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            // ═══════════════════════════════════════════════
            // CONTROL DE PANTALLA (SIN VIEWMODEL)
            // ═══════════════════════════════════════════════
            var currentScreen by remember { mutableStateOf("login") }

            // ═══════════════════════════════════════════════
            // ESTADOS LOCALES (SIMPLES)
            // ═══════════════════════════════════════════════
            var username by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }

            var errorMessage by remember { mutableStateOf<String?>(null) }
            when (currentScreen) {

                // ───────────────────────────────
                // LOGIN
                // ───────────────────────────────
                "login" -> {
                    LoginScreen(
                        email = email, // usamos email aquí
                        password = password,
                        isLoading = isLoading,
                        errorMessage = errorMessage,

                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },

                        onLoginClick = {
                            println("Login pressed")
                        },

                        onRegisterClick = {
                            currentScreen = "register"
                        }
                    )
                }

                // ───────────────────────────────
                // REGISTER
                // ───────────────────────────────
                "register" -> {
                    RegisterScreen(
                        username = username,
                        email = email,
                        password = password,
                        confirmpassword = confirmPassword,
                        isLoading = isLoading,
                        errorMessage = errorMessage,

                        onUsernameChange = { username = it },
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onConfirmPasswordChange = {  confirmPassword = it },
                        // ⚠️ ojo: tu screen actual no tiene onEmailChange ni confirm
                        onRegisterClick = {
                            println("Register pressed")
                        },

                        onBackClick = { // ✅ CAMBIO IMPORTANTE
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
        ---------------------------------------------------------------------

        */
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {

                // ═══════════════════════════════════════════════════════
                // ESTADO DE NAVEGACIÓN
                //
                // Controla qué pantalla se muestra:
                // "login"    -> LoginScreen
                // "register" -> RegisterScreen
                //
                // Luego puedes agregar:
                // "lobby", "game", etc.
                // ═══════════════════════════════════════════════════════
                var currentScreen by remember { mutableStateOf("login") }


                // ═══════════════════════════════════════════════════════
                // ESTADOS DE LOGIN
                // (Temporalmente en Activity, luego van en ViewModel)
                // ═══════════════════════════════════════════════════════
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }


                val viewModel = viewModel<AuthViewModel>()
                val lobbyViewModel = viewModel<LobbyViewModel>()
                val state by viewModel.uiState.collectAsState()

                // ═══════════════════════════════════════════════════════
                // SWITCH DE PANTALLAS
                // ═══════════════════════════════════════════════════════
                when (currentScreen) {

                    // ────────────────────────────────────────────────
                    // LOGIN
                    // ────────────────────────────────────────────────
                    "login" -> {
                        LoginScreen(
                            email = state.email,
                            password = state.password,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,

                            onEmailChange = viewModel::onEmailChange,
                            onPasswordChange = viewModel::onPasswordChange,

                            onLoginClick = { viewModel.login() },

                            onRegisterClick = { currentScreen = "register" },
                            onGoToLobby = { currentScreen = "lobby" } // 👈 AQUÍ
                        )
                    }


                    // ────────────────────────────────────────────────
                    // REGISTER
                    // ────────────────────────────────────────────────
                    "register" -> {

                        var email by remember { mutableStateOf("") }
                        var confirmPassword by remember { mutableStateOf("") }

                        RegisterScreen(
                            username = state.username,
                            email = state.email,
                            password = state.password,
                            confirmpassword = state.confirmPassword,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,

                            onUsernameChange = viewModel::onUsernameChange,
                            onEmailChange = viewModel::onEmailChange,
                            onPasswordChange = viewModel::onPasswordChange,
                            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,

                            onRegisterClick = { viewModel.register() },

                            onBackClick = { currentScreen = "login" }
                        )
                    }
                    "lobby" -> {
                        LobbyScreen(
                            viewModel = lobbyViewModel,
                            onNavigateToGame = {
                                currentScreen = "game"
                            }
                        )
                    }

                    // 🔵 GAME (DEBUG)
                    "game" -> {
                        GameScreen(
                            onFinishGame = { win ->
                                var isWinner = win
                                currentScreen = "result"
                            }
                        )
                    }

                    // 🏆 RESULT (UNIFICADO)
                    "result" -> {
                        val isWinner = false
                        ResultScreen(
                            isWinner = isWinner,
                            onRetry = {
                                currentScreen = "game"
                            },
                            onExit = {
                                currentScreen = "lobby"
                            }
                        )
                    }
                }
            }
        }
 /*
        private var contador = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Layout dinámico (sin XML)
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 50, 50, 50)

            val titulo = TextView(this)
            titulo.text = "App de Prueba 🚀"
            titulo.textSize = 24f

            val input = EditText(this)
            input.hint = "Escribe tu nombre"

            val botonSaludo = Button(this)
            botonSaludo.text = "Saludar"

            val resultado = TextView(this)
            resultado.textSize = 18f

            val botonContador = Button(this)
            botonContador.text = "Sumar +1"

            val textoContador = TextView(this)
            textoContador.text = "Contador: 0"

            // Eventos
            botonSaludo.setOnClickListener {
                val nombre = input.text.toString()
                if (nombre.isNotEmpty()) {
                    resultado.text = "Hola, $nombre 👋"
                } else {
                    resultado.text = "Escribe algo primero 😅"
                }
            }

            botonContador.setOnClickListener {
                contador++
                textoContador.text = "Contador: $contador"
            }

            // Agregar vistas al layout
            layout.addView(titulo)
            layout.addView(input)
            layout.addView(botonSaludo)
            layout.addView(resultado)
            layout.addView(botonContador)
            layout.addView(textoContador)

            setContentView(layout)
        }
        */
}