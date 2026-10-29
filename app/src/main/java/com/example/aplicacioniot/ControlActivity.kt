package com.example.aplicacioniot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.UUID

class ControlActivity : AppCompatActivity() {

    private val miUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    // Se declara el adaptador como nullable (?) por si el emulador no tiene Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isConnected: Boolean = false

    private lateinit var tvEstadoBluetooth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        tvEstadoBluetooth = findViewById(R.id.tvEstadoBluetooth)
        val btnConectar = findViewById<Button>(R.id.btnConectar)
        val btnEncender = findViewById<Button>(R.id.btnEncender)
        val btnApagar = findViewById<Button>(R.id.btnApagar)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        btnConectar.setOnClickListener {
            conectarBluetooth()
        }

        btnEncender.setOnClickListener {
            enviarComando("A")
        }

        btnApagar.setOnClickListener {
            enviarComando("B")
        }
    }

    @SuppressLint("MissingPermission")
    private fun conectarBluetooth() {
        // PROTECCIÓN 1: Evita que el emulador colapse por no tener Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo (o emulador) no tiene Bluetooth físico", Toast.LENGTH_LONG).show()
            return
        }

        try {
            if (bluetoothAdapter?.isEnabled == false) {
                Toast.makeText(this, "Por favor, enciende el Bluetooth", Toast.LENGTH_SHORT).show()
                return
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Toast.makeText(this, "No hay dispositivos vinculados.", Toast.LENGTH_LONG).show()
                return
            }

            var moduloDispositivo: BluetoothDevice? = null
            for (device in pairedDevices) {
                if (device.name == "BT05" || device.name == "HC-05") {
                    moduloDispositivo = device
                    break
                }
            }

            if (moduloDispositivo == null) {
                moduloDispositivo = pairedDevices.first()
            }

            bluetoothSocket = moduloDispositivo.createRfcommSocketToServiceRecord(miUUID)
            bluetoothSocket?.connect()
            isConnected = true

            tvEstadoBluetooth.text = "Estado: Conectado a ${moduloDispositivo.name}"
            tvEstadoBluetooth.setTextColor(Color.parseColor("#388E3C"))
            Toast.makeText(this, "¡Conectado exitosamente!", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            // PROTECCIÓN 2: Evita el cierre si Android bloquea la app por falta de permisos manuales
            Toast.makeText(this, "Error de Permisos. Ve a Ajustes del celular > Apps > Permisos y activa 'Dispositivos Cercanos' y 'Ubicación'", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            isConnected = false
            tvEstadoBluetooth.text = "Estado: Error de conexión"
            Toast.makeText(this, "Fallo al conectar. Revisa el módulo Arduino.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar conexión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarComando(comando: String) {
        if (bluetoothSocket != null && isConnected) {
            try {
                bluetoothSocket!!.outputStream.write(comando.toByteArray())
                Toast.makeText(this, "Enviando comando: $comando", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error de transmisión de datos", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Primero presiona CONECTAR BLUETOOTH", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}