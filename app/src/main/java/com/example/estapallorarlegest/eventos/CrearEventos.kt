package com.example.estapallorarlegest.eventos

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.text.isDigitsOnly
import com.example.estapallorarlegest.*
import com.example.estapallorarlegest.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KFunction1

class CrearEventos : AppCompatActivity() {

    val datePicker_ui : TextInputEditText by lazy {
        findViewById(R.id.fecha_event_input)
    }

    val img_event : ImageView by lazy {
        findViewById(R.id.img_event_input)
    }

    val nombre : TextInputEditText by lazy {
        findViewById(R.id.nom_event_input)
    }

    val aforo : TextInputEditText by lazy {
        findViewById(R.id.aforo_max_input)
    }

    val precio : TextInputEditText by lazy {
        findViewById(R.id.precio_event_input)
    }

    val crear : Button by lazy {
        findViewById(R.id.create_event_btn)
    }

    val validar_campos : Map<TextInputEditText, KFunction1<TextInputEditText, Boolean>> by lazy {
        mapOf(
            nombre  to this::validName,
            aforo to this::validAforo,
            precio to this::validPrecio,
            datePicker_ui to this::validDate,
        )
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    val sto_ref : StorageReference by lazy {
        FirebaseStorage.getInstance().getReference()
    }


    private var url_img : Uri?= null
    private lateinit var this_activity : AppCompatActivity


    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var date = LocalDate.now()

    override fun onResume() {
        super.onResume()
        if (!Utilidades.esAdmin(applicationContext)){
            startActivity(Intent(applicationContext, VerEventos::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_eventos)
        this_activity = this

        datePicker_ui.setOnClickListener {
            abrirDatePicker()
        }

        crear.setOnClickListener{
            if (validarCampos()){
                if(url_img == null){
                    Toast.makeText(applicationContext, "Selecciona alguna foto", Toast.LENGTH_SHORT).show()
                }else{
                    db_ref.child("tienda").child("eventos")
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val nombre = nombre.text.toString()

                                val res = snapshot.children.singleOrNull {
                                    val pojo = it.getValue(Evento::class.java)!!
                                    pojo.nombre == nombre &&
                                    pojo.fecha == datePicker_ui.text.toString()
                                }

                                if (res == null){
                                    val id_user = db_ref.child("tienda")
                                        .child("eventos").push().key!!

                                    GlobalScope.launch(Dispatchers.IO) {
                                        val url_imagen = Utilidades.guardarImagen(
                                            sto_ref,
                                            id_user,
                                            url_img!!,
                                            "eventos"
                                        )

                                        db_ref.child("tienda").child("eventos").child(id_user)
                                            .setValue(
                                                Evento(
                                                    id_user,
                                                    nombre,
                                                    datePicker_ui.text.toString(),
                                                    precio.text.toString().toDouble(),
                                                    aforo.text.toString().toInt(),
                                                    aforo.text.toString().toInt(),
                                                    url_imagen,
                                                )
                                            )
                                        this_activity.runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                "Nuevo Evento Creado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        }
                                        
                                    }
                                }else{
                                    Toast.makeText(applicationContext, "Ese Nombre de evento ya existe", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
                }
            }else{
                Toast.makeText(applicationContext, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        img_event.setOnClickListener{
            accesoGaleria.launch("image/*")
        }

        img_event.setOnLongClickListener {
            val fich_temp = crearFicheroImg()
            url_img = FileProvider.getUriForFile(
                applicationContext,
                "com.example.estapallorarlegest.fileprovider",
                fich_temp
            )
            accesoCamara.launch(url_img)
            true
        }

    }

    fun validarCampos():Boolean{
        var correcto = true
        validar_campos.forEach { entrada, funcion ->
            correcto = correcto && funcion(entrada)
        }
        return correcto
    }


    fun validPrecio(e:TextInputEditText):Boolean{
        var correcto: Boolean

        if (e.text.toString().toDouble() <= 0.0){
            e.error = "El precio tiene que ser mayor que 0"
            correcto = false
        }else{
            correcto = true
        }

        return correcto
    }

    fun validAforo(e:TextInputEditText):Boolean{
        var correcto:Boolean

        if (e.text.toString().toInt() <= 0){
            e.error = "El aforo tiene que ser mayor que 0"
            correcto = false
        }else{
            correcto = true
        }

        return correcto
    }

    fun validName(e:TextInputEditText):Boolean{
        var correcto:Boolean
        var valor=e.text.toString().trim()

        if(valor.length>=3 && !valor.isDigitsOnly()){
            correcto=true
        }else{
            e.error="Formato de nombre incorrecto"
            correcto=false
        }

        return correcto
    }

    fun validDate(e:TextInputEditText):Boolean{
        var correcto:Boolean
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        var valor=e.text.toString().trim()

        //Añadir una condicion para verificar que la fecha seleccionada sea mayor que la actual
        if(valor.isEmpty()){
            correcto=false
            e.error = "Debes añadir una fecha"
        }else{
            correcto=true
        }

        return correcto
    }

    fun fechaSeleccionada(day:Int, month:Int, year:Int){
        var fecha_evento =""

        fecha_evento+="$year"

        if(month<10){
            fecha_evento+="-0${month+1}"
        }else{
            fecha_evento+="-${month+1}"
        }

        if(day<10){
            fecha_evento+="-0$day"
        }else{
            fecha_evento+="-$day"
        }

        datePicker_ui.setText(fecha_evento)
        //actualizamos la fecha seleccionada
        date = LocalDate.parse(fecha_evento, formatter)
    }

    fun abrirDatePicker() {
        val selectorFecha= DatePickerFragment{
                dia,mes,año-> fechaSeleccionada(dia,mes,año)
        }

        selectorFecha.show(supportFragmentManager,"fecha")
    }

    private val accesoGaleria = registerForActivityResult(ActivityResultContracts.GetContent()){
        if (it != null){
            url_img = it
            img_event.setImageURI(url_img)
        }
    }

    private fun crearFicheroImg() : File {
        val cal : Calendar? = Calendar.getInstance()
        val timeStamp: String?  = SimpleDateFormat("yyyyMMdd_HHmmss").format(cal!!.time)
        val nombreFichero : String? = "JPGE_"+timeStamp+"_"
        val carpetaDir: File? =
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val ficheroImagen: File? = File.createTempFile(nombreFichero!!, ".jpg", carpetaDir)

        return ficheroImagen!!
    }

    private val accesoCamara = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if (it){
            img_event.setImageURI(url_img)
        }else{
            Toast.makeText(applicationContext, "No has hecho ninguna foto", Toast.LENGTH_LONG)
        }
    }
}