package com.example.estapallorarlegest.productos

import android.animation.Animator
import android.content.Intent
import android.graphics.Matrix
import android.icu.number.Scale
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Usuario
import com.example.estapallorarlegest.Utilidades
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.RangeSlider
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class VerProductos : AppCompatActivity() {

    val recycler : RecyclerView by lazy {
        findViewById(R.id.recycler_prods)
    }

    val ir_crear_prod : FloatingActionButton by lazy {
        findViewById(R.id.add_prod_btn)
    }

    val priceSlider : RangeSlider by lazy {
        findViewById(R.id.precios_range_slider)
    }

    //No switch es euros si dolares
    val divisa : Switch by lazy {
        findViewById(R.id.divisas_switch)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    val constraint : ConstraintLayout by lazy {
        findViewById(R.id.constraint_filter_prices)
    }

    private lateinit var lista : MutableList<Producto>
    private lateinit var adaptador: ProductosAdaptador

    var query = ""
    private var cont = 0
    private var lista_bool = MutableList(Utilidades.lista_categorias.size){true}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_productos)

        if (Utilidades.esAdmin(applicationContext)){
            title = "Administrar Productos"
            ir_crear_prod.setOnClickListener {
                startActivity(Intent(applicationContext, CrearProductos::class.java))
            }
        }else{
            title = "Comprar Productos"
            ir_crear_prod.visibility = View.GONE
        }

        lista = mutableListOf<Producto>()

        //SHARED PREFERENCES DIVISA
        val ID = applicationContext.getString(R.string.app_id)
        val sp_name = "${ID}_Datos_Usuarios"
        val SP = applicationContext.getSharedPreferences(sp_name, 0)

        var prodmin = 0.0f
        var prodmax = 5.0f
        val semaforo = CountDownLatch(1)

        lifecycleScope.launch (Dispatchers.IO){
            db_ref.child("tienda").child("productos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        lista.clear()
                        snapshot.children.forEach {
                            val pojo = it.getValue(Producto::class.java)!!
                            if (Utilidades.esAdmin(applicationContext)) {
                                lista.add(pojo)
                                semaforo.countDown()
                            } else {
                                if (pojo.disponible!!) {
                                    lista.add(pojo)
                                    semaforo.countDown()
                                }
                            }
                        }
                        semaforo.await()
                        //SETEO DE VALORES DEL SLIDER INICIALES
                        prodmin = lista.minOf { it.precio!! }.toFloat() - 1
                        prodmax = lista.maxOf { it.precio!! }.toFloat() + 3
                        priceSlider.valueFrom = prodmin
                        priceSlider.valueTo = prodmax
                        priceSlider.setValues(prodmin, prodmax)

                        //Inicializando el adaptador dentro de onDataChange
                        divisa.isChecked = SP.getBoolean("divisa", false)
                        println("#############-SETEO DEL INICIO DIVISA: "+divisa.isChecked)
                        adaptador = ProductosAdaptador(lista, lifecycleScope, divisa.isChecked)
                        adaptador.divisa_dol = divisa.isChecked
                        adaptador.precio_min = prodmin
                        adaptador.precio_max = prodmax
                        recycler.adapter = adaptador
                        recycler.layoutManager = LinearLayoutManager(applicationContext)
                        recycler.setHasFixedSize(true)
                        adaptador.notifyDataSetChanged()

                        //Configuración del rango de deslizador en onDataChange
                        priceSlider.addOnChangeListener { slider, _ , _ ->
                            val values = slider.values

                            var min = values[0]
                            var max = values[1]


                            adaptador.precio_min = min
                            adaptador.precio_max = max
                            adaptador.filter.filter(query)
                            adaptador.notifyDataSetChanged()
                        }

                        divisa.setOnCheckedChangeListener { _, b ->
                            adaptador.divisa_dol = b
                            adaptador.notifyDataSetChanged()
                            with(SP.edit()){
                                putBoolean("divisa", b)
                                commit()
                            }
                            println("#########-DOLARES: "+SP.getBoolean("divisa", false))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.filtros_temas_prods -> {
                multipleChoiceCategories()
                adaptador.notifyDataSetChanged()
                true
            }

            R.id.filtros_precios_prods -> {
                cont++

                if( cont % 2 != 0){
                    constraint.visibility = View.VISIBLE
                }else{
                    constraint.visibility = View.GONE
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun multipleChoiceCategories(){
        AlertDialog.Builder(this@VerProductos)
            .setTitle("Filtrar por Categorías")
            .setMultiChoiceItems(Utilidades.lista_categorias.toTypedArray(), lista_bool.toBooleanArray()){
                _, it, isChecked ->
                lista_bool[it] = isChecked
            }
            .setPositiveButton("Aplicar"){_, _, ->
                adaptador.selectedCategories.clear()
                adaptador.selectedCategories.addAll(lista_bool)
                adaptador.filter.filter(query)
            }.create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_prods, menu)
        val item = menu?.findItem(R.id.app_bar_search_prods)
        val searchView = item?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                query = p0!!
                adaptador.filter.filter(p0)
                return true
            }
        })

        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                query = ""
                adaptador.filter.filter("")
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        cont = 0
        super.onBackPressed()
    }
}