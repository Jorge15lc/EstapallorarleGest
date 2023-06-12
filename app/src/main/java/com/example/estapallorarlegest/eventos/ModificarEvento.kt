package com.example.estapallorarlegest.eventos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.estapallorarlegest.MainActivity
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Utilidades
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

class ModificarEvento :AppCompatActivity() {

    val img : ImageView by lazy{
        findViewById(R.id.img_event_input_mod)
    }

    val nombre : TextInputEditText by lazy {
        findViewById(R.id.nom_event_input_mod)
    }

    val aforo : TextInputEditText by lazy {
        findViewById(R.id.aforo_max_input_mod)
    }

    val precio : TextInputEditText by lazy {
        findViewById(R.id.precio_event_input_mod)
    }

    val fecha : TextInputEditText by lazy {
        findViewById(R.id.fecha_event_input_mod)
    }

    val guardar : Button by lazy {
        findViewById(R.id.mod_event_btn)
    }

    val visible : Switch by lazy {
        findViewById(R.id.visibility_event_mod)
    }

    val disponible : Switch by lazy {
        findViewById(R.id.disponible_event_mod)
    }

    val validar_campos : Map<TextInputEditText, KFunction1<TextInputEditText, Boolean>> by lazy {
        mapOf(
            nombre  to this::validName,
            aforo to this::validAforo,
            precio to this::validPrecio,
            fecha to this::validDate,
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
        if (!Utilidades.esAdmin(applicationContext)
            || Utilidades.obtenerIDuser(applicationContext) == ""){
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_evento)

        this_activity = this

        val pojo_ev = intent.getParcelableExtra<Evento>("evento")!!
        title = "Modificar "+pojo_ev.nombre

        nombre.setText(pojo_ev.nombre)
        precio.setText(pojo_ev.precio.toString())
        aforo.setText(pojo_ev.aforo.toString())
        aforo.isEnabled = false
        fecha.setText(pojo_ev.fecha)
        visible.isChecked = pojo_ev.visible!!
        disponible.isChecked = pojo_ev.activo!!

        Glide.with(applicationContext)
            .asBitmap()
            .apply(Utilidades.opcionesGlide(applicationContext))
            .transition(Utilidades.transicion)
            .load(pojo_ev.imagen)
            .into(img)

        guardar.setOnClickListener {
            var url_img_fire : String?
            if (validarCampos()){
                db_ref.child("tienda").child("eventos").child(pojo_ev.id!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var pojo = snapshot.getValue(Evento::class.java)!!

                            lifecycleScope.launch(Dispatchers.IO) {
                                if (url_img == null){
                                    url_img_fire = pojo_ev.imagen
                                }else{
                                    sto_ref.child("eventos").child(pojo_ev.id!!).delete()
                                    url_img_fire = Utilidades.guardarImagen(
                                        sto_ref,
                                        pojo.id!!,
                                        url_img!!,
                                        "eventos"
                                    )
                                }
                                //FIXME Desactivar cuando el aforo este completo o haya pasado la fecha
                                db_ref.child("tienda").child("eventos").child(pojo.id!!)
                                    .setValue(
                                        Evento(
                                            pojo.id,
                                            nombre.text.toString(),
                                            fecha.text.toString(),
                                            precio.text.toString().toDouble(),
                                            aforo.text.toString().toInt(),
                                            pojo.aforo_rest,
                                            url_img_fire,
                                            disponible.isChecked,
                                            visible.isChecked
                                        )
                                    )
                                this_activity.runOnUiThread {
                                    Toast.makeText(applicationContext, "Evento Modificado", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
            }
        }



        fecha.setOnClickListener {
            abrirDatePicker()
        }

        img.setOnClickListener{
            accesoGaleria.launch("image/*")
        }

        img.setOnLongClickListener {
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
        var fecha_evento:String=""

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

        fecha.setText(fecha_evento)
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
            img.setImageURI(url_img)
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
            img.setImageURI(url_img)
        }else{
            Toast.makeText(applicationContext, "No has hecho ninguna foto", Toast.LENGTH_LONG)
        }
    }
}