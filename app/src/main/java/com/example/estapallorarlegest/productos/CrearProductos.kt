package com.example.estapallorarlegest.productos

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
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
import java.util.*
import kotlin.reflect.KFunction1

class CrearProductos : AppCompatActivity() {

    val nombre : TextInputEditText by lazy {
        findViewById(R.id.nom_prod_input)
    }

    val precio : TextInputEditText by lazy {
        findViewById(R.id.precio_prod_input)
    }

    val stock : TextInputEditText by lazy {
        findViewById(R.id.stock_prod_input)
    }

    val descripcion : TextInputEditText by lazy {
        findViewById(R.id.desc_prod_input)
    }

    val categorias : Spinner by lazy {
        findViewById(R.id.select_categories)
    }

    val crear : Button by lazy {
        findViewById(R.id.create_prod_btn)
    }

    val img_prod : ImageView by lazy {
        findViewById(R.id.img_prod_input)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    val sto_ref : StorageReference by lazy {
        FirebaseStorage.getInstance().getReference()
    }

    val validador_inputs : Map<TextInputEditText, KFunction1<TextInputEditText, Boolean>> by lazy {
        mapOf(
            nombre to this::validarNombre,
            precio to this::validarPrecio,
            stock to this::validarStock,
            descripcion to this::validarDesc,
        )
    }

    private var url_img : Uri ?= null
    private lateinit var this_activity : AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_productos)
        title = "Crear Nuevo Producto"
        this_activity = this

        val adap_spinner = ArrayAdapter(applicationContext,
            R.layout.custom_spinner,
            Utilidades.lista_categorias
        )
        adap_spinner.setDropDownViewResource(R.layout.dropdown_spinner_item)
        categorias.adapter = adap_spinner

        crear.setOnClickListener {
            if (validarCampos()){
                if (url_img == null) {
                    Toast.makeText(applicationContext, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
                } else {
                    db_ref.child("tienda").child("productos")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val nom = nombre.text.toString().trim()

                                val res = snapshot.children.singleOrNull {
                                    val pojo = it.getValue(Producto::class.java)!!
                                    pojo.nombre == nom
                                }

                                if (res == null) {
                                    val id_prod = db_ref.child("tienda")
                                        .child("productos").push().key!!

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val url_imagen =
                                            Utilidades.guardarImagen(
                                                sto_ref,
                                                id_prod,
                                                url_img!!,
                                                "productos"
                                            )
                                        var disponibilidad = true
                                        if (stock.text.toString().trim().toInt() == 0) {
                                            disponibilidad = false
                                        }

                                        db_ref.child("tienda").child("productos").child(id_prod)
                                            .setValue(
                                                Producto(
                                                    id_prod,
                                                    nom,
                                                    descripcion.text.toString().trim(),
                                                    precio.text.toString().trim().toDouble(),
                                                    stock.text.toString().trim().toInt(),
                                                    categorias.selectedItem.toString(),
                                                    url_imagen,
                                                    disponibilidad,
                                                )
                                            )

                                        this_activity.runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                "Nuevo Producto Creado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
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

        img_prod.setOnClickListener {
            accesoGaleria.launch("image/*")
        }

        img_prod.setOnLongClickListener {
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

    fun validarPrecio(e: TextInputEditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "Tienes que introducir un nombre"
        }else if(valor.toDouble() <= 0){
            correcto = false
            e.error = "El precio no puede ser negativo o 0"
        }else{
            correcto = true
        }

        return correcto
    }

    fun validarStock(e: TextInputEditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "Tienes que introducir un stock"
        }else if(valor.toInt() < 0){
            correcto = false
            e.error = "El stock no puede ser negativo"
        }else{
            correcto = true
        }

        return correcto
    }

    fun validarDesc(e: TextInputEditText):Boolean{
        var correcto = true
        var valor = e.text.toString().trim()

        if (valor.isEmpty()){
            correcto = false
            e.error = "Tienes que introducir un nombre"
        }else if(valor.length >= 200){
            correcto = false
            e.error = "La descripcion no puede superar los 200 caracteres"
        }else{
            correcto = true
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

    private val accesoCamara = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if (it){
            img_prod.setImageURI(url_img)
        }else{
            Toast.makeText(applicationContext, "No has hecho ninguna foto", Toast.LENGTH_LONG)
        }
    }

    private val accesoGaleria = registerForActivityResult(ActivityResultContracts.GetContent()){
        if (it != null){
            url_img = it
            img_prod.setImageURI(url_img)
        }
    }
}