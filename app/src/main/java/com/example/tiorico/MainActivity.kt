package com.example.tiorico

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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
}