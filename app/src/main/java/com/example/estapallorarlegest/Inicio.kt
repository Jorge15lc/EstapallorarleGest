package com.example.estapallorarlegest

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.estapallorarlegest.eventos.VerEventos
import com.example.estapallorarlegest.foro.ChatGlobal
import com.example.estapallorarlegest.pedidos.VerPedidos
import com.example.estapallorarlegest.productos.VerProductos
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KFunction1

class Inicio : AppCompatActivity() {

    val btn : Button by lazy{
        findViewById(R.id.editar_datos_btn)
    }

    val ir_ver_pedidos : Button by lazy {
        findViewById(R.id.btn_1_row)
    }

    val ir_ver_productos : Button by lazy {
        findViewById(R.id.btn_2_row)
    }

    val ir_ver_eventos : Button by lazy {
        findViewById(R.id.btn_3_row)
    }

    val ir_ver_foro_small : Button by lazy {
        findViewById(R.id.btn_4_row_foro)
    }

    val ir_ver_foro_large : Button by lazy {
        findViewById(R.id.btn_4_row)
    }

    val ir_ver_usuarios : Button by lazy {
        findViewById(R.id.btn_5_row_us)
    }

    val editar : Switch by lazy {
        findViewById(R.id.switch_editar)
    }

    val btn_guardar : Button by lazy {
        findViewById(R.id.editar_datos_btn)
    }

    val name_tv : TextView by lazy {
        findViewById(R.id.user_name_data)
    }

    val edit_name : EditText by lazy {
        findViewById(R.id.editar_nombre_user)
    }

    val email_tv : TextView by lazy {
        findViewById(R.id.user_email_data)
    }

    val edit_email : EditText by lazy {
        findViewById(R.id.editar_email_user)
    }

    val pass_tv : TextView by lazy {
        findViewById(R.id.user_pass_data)
    }

    val edit_pass : EditText by lazy {
        findViewById(R.id.editar_pass_user)
    }

    val imagen : ImageView by lazy {
        findViewById(R.id.img_user_data)
    }

    val ui_img : ImageView by lazy {
        findViewById(R.id.ui_mode_img)
    }

    val switch_ui : Switch by lazy {
        findViewById(R.id.switch_ui_mode)
    }

    val logout : ImageView by lazy {
        findViewById(R.id.btn_logout)
    }

    val validar_campos : Map<EditText, KFunction1<EditText, Boolean>> by lazy {
        mapOf(
            edit_name to this::validarNombre,
            edit_email to this::validarEmail,
            edit_pass to this::validarPass,
        )
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    val sto_ref : StorageReference by lazy {
        FirebaseStorage.getInstance().reference
    }

    private var url_img : Uri?= null
    private lateinit var this_activity : AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        this_activity = this

        val id = Utilidades.obtenerIDuser(applicationContext)

        //Seteo de datos
        db_ref.child("tienda")
            .child("usuarios")
            .child(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pojo = snapshot.getValue(Usuario::class.java)
                    name_tv.text = pojo!!.nombre
                    edit_name.setText(pojo!!.nombre)
                    email_tv.text = pojo!!.correo
                    edit_email.setText(pojo!!.correo)
                    pass_tv.text = pojo!!.pass
                    edit_pass.setText(pojo!!.pass)
                    imagen.let {
                        Glide.with(applicationContext)
                            .asBitmap()
                            .load(pojo.imagen)
                            .apply(Utilidades.opcionesGlide(applicationContext))
                            .transition(Utilidades.transicion)
                            .into(it)
                    }
                    title = "Tus datos "+pojo.nombre

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        editar.setOnCheckedChangeListener { button, b ->
            visibilidad()
            if (b) {
                imagen.setOnClickListener {
                    accesoGaleria.launch("image/*")
                }

                imagen.setOnLongClickListener {
                    val fich_temp = crearFicheroImg()
                    url_img = FileProvider.getUriForFile(
                        applicationContext,
                        "com.example.estapallorarlegest.fileprovider",
                        fich_temp
                    )
                    accesoCamara.launch(url_img)
                    true
                }
                //Hacemos el guardado de datos
                btn_guardar.setOnClickListener {
                    var url_img_fire: String?
                    if (validarCampos()) {
                        db_ref.child("tienda").child("usuarios").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    var pojo_user = snapshot.getValue(Usuario::class.java)!!

                                    GlobalScope.launch(Dispatchers.IO) {
                                        if (url_img == null) {
                                            url_img_fire = Utilidades.obtenerUrlimgUser(applicationContext)
                                        } else {
                                            db_ref.child("usuarios").child(id).removeValue()
                                            url_img_fire = Utilidades.guardarImagen(
                                                sto_ref,
                                                id,
                                                url_img!!,
                                                "usuarios"
                                            )
                                            Utilidades.establecerUrlimgUser(applicationContext, url_img_fire!!)
                                        }
                                        db_ref.child("tienda").child("usuarios").child(id)
                                            .setValue(
                                                Usuario(
                                                    id,
                                                    edit_name.text.toString(),
                                                    edit_pass.text.toString(),
                                                    edit_email.text.toString(),
                                                    url_img_fire,
                                                    pojo_user.admin,
                                                    pojo_user.fecha_registro,
                                                    pojo_user.bloqueado
                                                )
                                            )



                                        this_activity.runOnUiThread {
                                            name_tv.text = edit_name.text.toString()
                                            pass_tv.text = edit_pass.text.toString()
                                            email_tv.text = edit_email.text.toString()
                                            Toast.makeText(
                                                applicationContext,
                                                "Datos modificados",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    TODO("Not yet implemented")
                                }
                            })
                    }
                    editar.isChecked = false
                }
            }
        }

        //Funcionamiento de los botones
        switch_ui.isChecked = Utilidades.getModeStatus(applicationContext)
        Utilidades.changeModeStatus(applicationContext, switch_ui.isChecked, ui_img)

        switch_ui.setOnCheckedChangeListener { button, b ->

            println("MODO NOCHE CHECKED CHANGE: "+Utilidades.getModeStatus(applicationContext))
            Utilidades.changeModeStatus(applicationContext, b, ui_img)
        }

        ir_ver_productos.setOnClickListener {
            startActivity(Intent(applicationContext, VerProductos::class.java))
        }

        ir_ver_pedidos.setOnClickListener {
            startActivity(Intent(applicationContext, VerPedidos::class.java))
        }

        ir_ver_eventos.setOnClickListener {
            startActivity(Intent(applicationContext, VerEventos::class.java))
        }


        if (Utilidades.esAdmin(applicationContext)){
            ir_ver_foro_large.visibility = View.INVISIBLE
            ir_ver_usuarios.visibility = View.VISIBLE
            ir_ver_foro_small.visibility = View.VISIBLE
            ir_ver_foro_small.setOnClickListener {
                startActivity(Intent(applicationContext, ChatGlobal::class.java))
            }

            ir_ver_usuarios.setOnClickListener {
                startActivity(Intent(applicationContext, VerUsuarios::class.java))
            }
        }else{
            ir_ver_foro_large.visibility = View.VISIBLE
            ir_ver_usuarios.visibility = View.INVISIBLE
            ir_ver_foro_small.visibility = View.INVISIBLE
            ir_ver_foro_large.setOnClickListener {
                startActivity(Intent(applicationContext, ChatGlobal::class.java))
            }
        }

        logout.setOnClickListener {
            val clave_id = "id"
            val clave_tipo = "tipo"
            val clave_url = "url"
            val ID = applicationContext.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = applicationContext.getSharedPreferences(sp_name, 0)

            with(SP.edit()){
                putString(clave_id, "")
                commit()
            }
            with(SP.edit()){
                putBoolean(clave_tipo, false)
                commit()
            }
            with(SP.edit()){
                putString(clave_url, "")
                commit()
            }
            finish()
        }
    }

    fun visibilidad(){
        if(editar.isChecked){
            //Enseñar
            btn_guardar.visibility = View.VISIBLE
            edit_email.visibility = View.VISIBLE
            edit_pass.visibility = View.VISIBLE
            //Esconder
            if(Utilidades.esAdmin(applicationContext)){
                name_tv.visibility = View.VISIBLE
                edit_name.visibility = View.INVISIBLE
            }else{
                edit_name.visibility = View.VISIBLE
                edit_pass.visibility = View.VISIBLE
                name_tv.visibility = View.INVISIBLE
                pass_tv.visibility = View.INVISIBLE
            }
            pass_tv.visibility = View.INVISIBLE
            email_tv.visibility = View.INVISIBLE
        }else{
            //Enseñar
            name_tv.visibility = View.VISIBLE
            email_tv.visibility = View.VISIBLE
            pass_tv.visibility = View.VISIBLE

            if(!Utilidades.esAdmin(applicationContext)){
                edit_name.visibility = View.INVISIBLE
            }
            //Esconder
            edit_pass.visibility = View.INVISIBLE
            btn_guardar.visibility = View.INVISIBLE
            edit_email.visibility = View.INVISIBLE
        }
    }

    fun validarPass(e : EditText):Boolean{
        var valor = e.text.toString().trim()
        var correcto : Boolean

        /* La contraseña es valida si contiene:
            - De 8 a 20 caracteres
            - Sin espacios
            - Un caracter en minuscula
            - Un caracter en mayusculas
            - Un digito como minimo
            - Caracter Especial
         */

        if (valor.matches("^(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[^\\w\\d\\s:])([^\\s]){8,16}\$".toRegex())){
            correcto=true
        }else{
            correcto=false
            e.error="La contraseña debe contener como mínimo: " +
                    "- De 8 a 20 caracteres" +
                    "- Un caracter en minúscula" +
                    "- Un caracter en mayúscula" +
                    "- Un digito" +
                    "- Un carácter especial"
        }

        return correcto
    }

    fun validarNombre(e: EditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "El nombre es obligatorio"
        }

        return correcto
    }

    fun validarEmail(e: EditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "El email es un campo obligatorio"
        }else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(valor).matches()){
            correcto = false
            e.error = "Introduce un email correcto"
        }
        return correcto
    }

    fun validarCampos():Boolean{
        var correcto = true
        validar_campos.forEach { entrada, funcion ->
            correcto = correcto && funcion(entrada)
        }
        return correcto
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

    private val accesoCamara = registerForActivityResult(ActivityResultContracts.TakePicture()){ success-> Boolean
        if (success){
            url_img?.let {
                setImageFromUri(it)
            }
        }else{
            Toast.makeText(applicationContext, "No has hecho ninguna foto", Toast.LENGTH_LONG)
        }
    }

    private val accesoGaleria = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if (uri != null) {
            url_img = uri
            setImageFromUri(url_img!!)
        }
    }

    override fun onBackPressed() {
        Toast.makeText(applicationContext, "Pulsa logout", Toast.LENGTH_SHORT).show()
    }

    private fun setImageFromUri(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(imagen)
    }
}