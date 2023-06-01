package com.example.estapallorarlegest

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import java.util.*
import kotlin.reflect.KFunction1

class Registro : AppCompatActivity() {

    val name : TextInputEditText by lazy {
        findViewById(R.id.nom_reg_user_input)
    }

    val pass1 : TextInputEditText by lazy {
        findViewById(R.id.pass_reg_user_input)
    }

    val pass2 : TextInputEditText by lazy {
        findViewById(R.id.pass_reg_user_input2)
    }

    val email : TextInputEditText by lazy {
        findViewById(R.id.email_reg_user_input)
    }

    val terms : CheckBox by lazy {
        findViewById(R.id.terms_cond_checkbox)
    }

    val register : Button by lazy {
        findViewById(R.id.resgister_btn)
    }

    val img_user : ImageView by lazy {
        findViewById(R.id.add_img_user_reg)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    val sto_ref : StorageReference by lazy {
        FirebaseStorage.getInstance().getReference()
    }

    val validador_inputs : Map<TextInputEditText, KFunction1<TextInputEditText, Boolean>> by lazy {
        mapOf(
            name to this::validarNombre,
            pass1 to this::validarPass1,
            email to this::validarEmail,
        )
    }

    private var url_img : Uri?= null
    private lateinit var this_activity : AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        setTitle("Registro")

        this_activity = this

        register.setOnClickListener {
            if (validarCampos() && validarTerms(terms)) {
                if(pass1.text.toString().trim() != pass2.text.toString().trim()) {
                    pass2.error = "Las contraseñas no coinciden"
                }else if(url_img == null){
                    Toast.makeText(applicationContext, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
                }else {
                    db_ref.child("tienda").child("usuarios")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val nom = name.text.toString().trim()
                                val correo = email.text.toString().trim()

                                val res = snapshot.children.singleOrNull {
                                    val pojo = it.getValue(Usuario::class.java)
                                    pojo!!.nombre == nom &&
                                            pojo!!.correo == correo
                                }

                                if (res == null) {
                                    val id_user = db_ref.child("tienda")
                                                        .child("usuarios").push().key!!

                                    GlobalScope.launch(Dispatchers.IO) {
                                        val url_imagen =
                                            Utilidades.guardarImagen(sto_ref, id_user, url_img!!, "usuarios")
                                        val fecha_ins = LocalDate.now().toString()

                                        db_ref.child("tienda").child("usuarios").child(id_user)
                                            .setValue(
                                                Usuario(
                                                    id_user,
                                                    nom,
                                                    pass1.text.toString().trim(),
                                                    correo,
                                                    url_imagen,
                                                    false,
                                                    fecha_ins,
                                                    false
                                                )
                                            )

                                        this_activity.runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                "Usuario Creado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    startActivity(Intent(applicationContext, MainActivity::class.java))
                                }else{
                                    Toast.makeText(applicationContext, "Ese usuario ya existe", Toast.LENGTH_SHORT).show()
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

        img_user.setOnClickListener {
            accesoGaleria.launch("image/*")
        }

        img_user.setOnLongClickListener {
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
            img_user.setImageURI(url_img)
        }else{
            Toast.makeText(applicationContext, "No has hecho ninguna foto", Toast.LENGTH_LONG)
        }
    }

    private val accesoGaleria = registerForActivityResult(ActivityResultContracts.GetContent()){
        if (it != null){
            url_img = it
            img_user.setImageURI(url_img)
        }
    }

    fun validarCampos():Boolean{
        var correcto = true
        validador_inputs.forEach { entrada, funcion ->
            correcto = correcto && funcion(entrada)
        }
        return correcto
    }

    fun validarNombre(e: TextInputEditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "El nombre es obligatorio"
        }

        return correcto
    }

    fun validarEmail(e: TextInputEditText):Boolean{
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

    fun validarPass1(e : TextInputEditText):Boolean{
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

    fun validarTerms(c : CheckBox):Boolean{
        var valor = c.isChecked

       if (!valor){
           c.error="Para registrarte debes aceptar los terminos y condiciones"
       }

        return valor
    }
}