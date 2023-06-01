package com.example.estapallorarlegest.productos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.estapallorarlegest.*
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.foro.ChatGlobalAdaptador
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

class ChatProducto : AppCompatActivity() {
    val recycler : RecyclerView by lazy {
        findViewById(R.id.recycler_chat)
    }

    val mensaje : EditText by lazy {
        findViewById(R.id.input_msg_chat)
    }

    val enviar : Button by lazy {
        findViewById(R.id.enviar_men)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    private lateinit var lista : MutableList<Mensaje>

    override fun onResume() {
        super.onResume()
        if (Utilidades.obtenerIDuser(applicationContext) == ""){
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_global)

        lista = mutableListOf()
        val user_id = Utilidades.obtenerIDuser(applicationContext)
        val user_imagen = Utilidades.obtenerUrlimgUser(applicationContext)
        var pojo_prod = intent.getParcelableExtra<Producto>("chat_prod")!!
        title = pojo_prod.nombre+" chat"


        enviar.setOnClickListener {
            val texto = mensaje.text.toString().trim()

            if (texto != ""){
                val fecha_now = Calendar.getInstance()
                val formateador = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
                val fecha_hora = formateador.format(fecha_now.time)

                val id_mensaje  = db_ref.child("tienda")
                    .child("chat_prods")
                    .push().key

                val nuevo_mens = Mensaje(
                    id_mensaje,
                    user_id,
                    texto,
                    fecha_hora,
                    "",
                    "",
                    pojo_prod.id
                )

                db_ref.child("tienda")
                    .child("chat_prods")
                    .child(id_mensaje!!)
                    .setValue(nuevo_mens)

                mensaje.text.clear()
            }else{
                Toast.makeText(applicationContext, "No has escrito nada", Toast.LENGTH_SHORT).show()
            }
        }

        db_ref.child("tienda").child("chat_prods")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val pojo_mens = snapshot.getValue(Mensaje::class.java)
                        pojo_mens!!.id_dispositivo = user_id
                        //Diferenciamos los mensajes por el mismo tema
                        if (pojo_mens.id_prod == pojo_prod.id
                            && pojo_mens.id_evento == ""){

                            if (pojo_mens.id_emisor == pojo_mens.id_dispositivo) {
                                pojo_mens.imagen_emisor = user_imagen
                            } else {
                                val semaforo = CountDownLatch(1)
                                //Realizamos otra consulta para sacar la imagen
                                db_ref.child("tienda")
                                    .child("usuarios")
                                    .child(pojo_mens.id_emisor!!)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val pojo_usu = snapshot.getValue(Usuario::class.java)
                                            pojo_mens.imagen_emisor = pojo_usu!!.imagen
                                            semaforo.countDown()
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }
                                    })
                                semaforo.await()
                            }
                            runOnUiThread {
                                if(pojo_mens.id_prod == pojo_prod.id){
                                    lista.add(pojo_mens)
                                }

                                lista.sortBy { it.id }
                                recycler.scrollToPosition(lista.size - 1)
                                recycler.adapter!!.notifyDataSetChanged()
                            }
                        }
                    }
                }
                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                    TODO("Not yet implemented")
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    TODO("Not yet implemented")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        recycler.adapter = ChatGlobalAdaptador(lista)
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.setHasFixedSize(true)
    }
}