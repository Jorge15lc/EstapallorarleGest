package com.example.estapallorarlegest.productos

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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

class InfoProducto : AppCompatActivity() {
    val nombre : TextInputEditText by lazy {
        findViewById(R.id.nom_prod_input_mod)
    }

    val precio : TextInputEditText by lazy {
        findViewById(R.id.precio_prod_input_mod)
    }

    val stock : TextInputEditText by lazy {
        findViewById(R.id.stock_prod_input_mod)
    }

    val descripcion : TextInputEditText by lazy {
        findViewById(R.id.desc_prod_input_mod)
    }

    val categorias : Spinner by lazy {
        findViewById(R.id.select_categories_mod)
    }

    val modificar : Button by lazy {
        findViewById(R.id.create_prod_btn_mod)
    }

    val img_prod : ImageView by lazy {
        findViewById(R.id.img_prod_input_mod)
    }

    val disponible : Switch by lazy {
        findViewById(R.id.disponibilidad_prod)
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

    private var url_img : Uri?= null
    private lateinit var this_activity : AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_producto)
        this_activity = this

        val adap_spinner = ArrayAdapter(applicationContext,
            R.layout.custom_spinner,
            Utilidades.lista_categorias
        )
        adap_spinner.setDropDownViewResource(R.layout.dropdown_spinner_item)
        categorias.adapter = adap_spinner

        val pojo_prod = intent.getParcelableExtra<Producto>("prod")!!
        title = "Modificar "+pojo_prod.nombre

        //seteamos los valores en los inputs
        nombre.setText(pojo_prod.nombre)
        precio.setText(pojo_prod.precio.toString())
        stock.setText(pojo_prod.stock.toString())
        descripcion.setText(pojo_prod.descripcion)
        disponible.isChecked = pojo_prod.disponible!!
        Glide.with(applicationContext)
            .asBitmap()
            .apply(Utilidades.opcionesGlide(applicationContext))
            .transition(Utilidades.transicion)
            .load(pojo_prod.url_foto)
            .into(img_prod)

        var cont = 0
        Utilidades.lista_categorias.forEach{
            if (it == pojo_prod.categoria){
                categorias.setSelection(cont)
            }
            cont++
        }

        modificar.setOnClickListener{
            var url_img_fire : String?
            if (validarCampos()){
                db_ref.child("tienda").child("productos").child(pojo_prod.id!!)
                    .addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var pojo = snapshot.getValue(Producto::class.java)!!

                            lifecycleScope.launch(Dispatchers.IO) {
                                if (url_img == null){
                                    url_img_fire = pojo.url_foto
                                }else{
                                    sto_ref.child("productos").child(pojo.id!!).delete()
                                    url_img_fire = Utilidades.guardarImagen(
                                        sto_ref,
                                        pojo.id!!,
                                        url_img!!,
                                        "productos"
                                    )
                                }

//                                println("POJO ID: "+pojo.id +
//                                        "\nNombre: "+nombre.text.toString()+
//                                        "\nDescripcion"+descripcion.text.toString()+
//                                        "\nPrecio: "+precio.text.toString()+
//                                        "\nStock: "+stock.text.toString()+
//                                        "\nCategorias: "+categorias.selectedItem.toString()+
//                                        "\nDisponible: "+disponible.isChecked)
                                db_ref.child("tienda").child("productos").child(pojo.id!!)
                                    .setValue(
                                        Producto(
                                            pojo.id,
                                            nombre.text.toString(),
                                            descripcion.text.toString(),
                                            precio.text.toString().toDouble(),
                                            stock.text.toString().toInt(),
                                            categorias.selectedItem.toString(),
                                            url_img_fire,
                                            disponible.isChecked
                                        )
                                    )


                                this_activity.runOnUiThread {
                                    Toast.makeText(applicationContext, "Producto Modificado", Toast.LENGTH_SHORT).show()
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