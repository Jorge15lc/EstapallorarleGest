package com.example.estapallorarlegest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.estapallorarlegest.pedidos.Pedido
import com.example.estapallorarlegest.pedidos.VerPedidos
import com.example.estapallorarlegest.productos.Producto
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    val input_nom : TextInputEditText by lazy{
        findViewById(R.id.nom_user_input)
    }

    val input_pass : TextInputEditText by lazy {
        findViewById(R.id.pass_user_input)
    }

    val login : Button by lazy {
        findViewById(R.id.iniciar_ses_btn)
    }

    val register : TextView by lazy {
        findViewById(R.id.resgister_enl)
    }

    private lateinit var generador: AtomicInteger


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle("LOGIN")

        //NOTIFICACIONES INICIO
        generador= AtomicInteger(0)
        crearCanalNotificaciones()

        if (Utilidades.obtenerIDuser(applicationContext) != ""){
            startActivity(Intent(applicationContext, Inicio::class.java))
        }

        login.setOnClickListener {
            if (isValid()){
                db_ref.child("tienda").child("usuarios")
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val nombre = input_nom.text.toString()
                            val pass = input_pass.text.toString()

                            val res = snapshot.children.singleOrNull {
                                val pojo = it.getValue(Usuario::class.java)!!
                                pojo.nombre == nombre &&
                                pojo.pass == pass &&
                                !pojo.bloqueado!!
                            }

                            if  (res != null){
                                val pojo = res.getValue(Usuario::class.java)
                                Utilidades.establecerIDuser(applicationContext, pojo!!.id!!)
                                Utilidades.establecerTipoUser(applicationContext, pojo.admin!!)
                                Utilidades.establecerUrlimgUser(applicationContext, pojo.imagen!!)

                                //Unicamente para el toast!!!
                                val ID = applicationContext.getString(R.string.app_id)
                                val sp_name = "${ID}_Registro_Usuarios"
                                val SP = applicationContext.getSharedPreferences("id", 0)
                                input_nom.text!!.clear()
                                input_pass.text!!.clear()
                                startActivity(Intent(applicationContext, Inicio::class.java))
                            }else{
                                Toast.makeText(applicationContext, "Nombre de usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }

                    })
            }
        }

        //NOtificaciones
        val ID = R.string.app_id
        val sp_name = "${ID}_timeStamp_noti"
        val SP = applicationContext.getSharedPreferences(sp_name, Context.MODE_PRIVATE)
        val lastChecked = SP.getLong("ultimoChequeo", 0)

        db_ref.child("tienda").child("pedidos")
            .addChildEventListener(object : ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val pojo_ped = snapshot.getValue(Pedido::class.java)!!
                    val id_noti = generador.incrementAndGet()

                    if  (pojo_ped.estado == 0
                        &&  Utilidades.esAdmin(applicationContext)
                        && pojo_ped.timestamp!! > lastChecked){
                        generarNotificacion(id_noti, pojo_ped, "Nuevo pedido de ${pojo_ped.nom_comprador}", "Nuevo Pedido de ${pojo_ped.nom_producto}", VerPedidos::class.java)

                        val editor = SP.edit()
                        editor.putLong("ultimoChequeo", System.currentTimeMillis())
                        editor.apply()
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val pojo_ped = snapshot.getValue(Pedido::class.java)!!
                    val id_noti = generador.incrementAndGet()

                    if (pojo_ped.estado == 3
                        && Utilidades.esAdmin(applicationContext)
                        && pojo_ped.timestamp!! > lastChecked){
                        generarNotificacion(id_noti, pojo_ped, "El pedido de ${pojo_ped.nom_comprador} ha sido recogido", "Pedido Recogido", VerPedidos::class.java)

                        val editor = SP.edit()
                        editor.putLong("ultimoChequeo", System.currentTimeMillis())
                        editor.apply()
                    }

                    if (pojo_ped.estado == 1
                        && pojo_ped.id_comprador == Utilidades.obtenerIDuser(applicationContext)){
                        generarNotificacion(id_noti, pojo_ped, "Tu pedido de ${pojo_ped.nom_producto} está en preparación", "Pedido Horneandose", VerPedidos::class.java)

                        val editor = SP.edit()
                        editor.putLong("ultimoChequeo", System.currentTimeMillis())
                        editor.apply()
                    }

                    if (pojo_ped.estado == 2
                        && pojo_ped.id_comprador == Utilidades.obtenerIDuser(applicationContext)){
                        generarNotificacion(id_noti, pojo_ped, "Tu pedido de ${pojo_ped.nom_producto} está esperandote 😊", "Pedido Esperandote", VerPedidos::class.java)

                        val editor = SP.edit()
                        editor.putLong("ultimoChequeo", System.currentTimeMillis())
                        editor.apply()
                    }

                    if (pojo_ped.estado == 4
                        && pojo_ped.id_comprador == Utilidades.obtenerIDuser(applicationContext)){
                        generarNotificacion(id_noti, pojo_ped, "Tu pedido de ${pojo_ped.nom_producto} ha sido rechazado", "Pedido Rechazado", VerPedidos::class.java)

                        val editor = SP.edit()
                        editor.putLong("ultimoChequeo", System.currentTimeMillis())
                        editor.apply()
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    TODO("Not yet implemented")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    val editor = SP.edit()
                    editor.putLong("ultimoChequeo", System.currentTimeMillis())
                    editor.apply()
                }
            })

        register.setOnClickListener {
            startActivity(Intent(applicationContext, Registro::class.java))
        }
    }

    private fun generarNotificacion(id_noti : Int, pojo : Parcelable, contenido : String, titulo : String, destino:Class<*>){
        val idcanal = getString(R.string.id_canal)
        val iconolargo = BitmapFactory.decodeResource(resources, R.drawable.cookie_logo)
        val actividad = Intent(applicationContext, destino)
        actividad.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        actividad.putExtra("producto", pojo)
        val pendingIntent = PendingIntent.getActivity(this, 0, actividad, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificacion = NotificationCompat.Builder(this, idcanal)
            .setLargeIcon(iconolargo)
            .setSmallIcon(R.drawable.logo_estapallorarle_round)
            .setContentTitle(titulo)
            .setContentText(contenido)
            .setSubText("Info Pedidos Estapallorarle")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(this)){
            notify(id_noti, notificacion)
        }
    }

    private fun crearCanalNotificaciones(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val nombre = getString(R.string.nombre_canal)
            val idcanal = getString(R.string.id_canal)
            val descripcion = getString(R.string.descripcion_canal)
            val importancia = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(idcanal, nombre, importancia).apply {
                description = descripcion
            }

            val nm : NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun isValid():Boolean{
        var valido : Boolean

        if(input_nom.text!!.isEmpty()){
            input_nom.error = "Falta el nombre de usuario"
            valido = false
        }else if(input_pass.text!!.isEmpty()){
            input_pass.error = "Falta la contraseña"
            valido = false
        }else{
            valido = true
        }
        return valido
    }
}