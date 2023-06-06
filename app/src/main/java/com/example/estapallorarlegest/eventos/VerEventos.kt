package com.example.estapallorarlegest.eventos

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Utilidades
import com.example.estapallorarlegest.asistenciaseventos.Solicitud
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class  VerEventos : AppCompatActivity() {

    val recycler : RecyclerView by lazy {
        findViewById(R.id.recycler_events)
    }

    val crear_evento : Button by lazy {
        findViewById(R.id.btn_crear_evento)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    var query = ""

    private lateinit var lista : MutableList<Evento>
    private lateinit var adaptador : EventoAdaptador
    private var checked = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_eventos)

        lista = mutableListOf<Evento>()

        todosEventos()

        adaptador = EventoAdaptador(lista, lifecycleScope)
        recycler.adapter = adaptador
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.setHasFixedSize(true)


        if (Utilidades.esAdmin(applicationContext)){
            crear_evento.visibility = View.VISIBLE
            crear_evento.setOnClickListener {
                startActivity(Intent(applicationContext, CrearEventos::class.java))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!Utilidades.esAdmin(applicationContext)){
            return when(item.itemId){
                R.id.filtros_events -> {
                    onlyChoiceBuilder()
                    adaptador.notifyDataSetChanged()
                    true
                }else -> super.onOptionsItemSelected(item)
            }
        }else{
            return when(item.itemId){
                R.id.filtros_events -> {
                    Toast.makeText(applicationContext, "El administrador no tiene filtros ", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    fun onlyChoiceBuilder(){
        val opcion : Array<String> = arrayOf("Ver Todos", "Solicitados", "Inscritos")

        AlertDialog.Builder(this@VerEventos)
            .setTitle("Filtros")
            .setSingleChoiceItems(opcion, checked){ _, which ->
                checked = which
                println("////////////////Valor checked: "+checked+" -which: "+which)
            }
            .setPositiveButton("Aplicar"){ _, _ ->
                if (checked == 0){
                    todosEventos()
                }else{
                    filtrarEventos(checked)
                }
            }
            .create().show()
    }

    fun todosEventos(){
        val hoy = Calendar.getInstance().time
        println(hoy)
        val formato = SimpleDateFormat("yyyy-MM-dd")
        val hoy_f = formato.format(hoy)
        db_ref.child("tienda").child("eventos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()

                    snapshot.children.forEach{
                        val pojo = it?.getValue(Evento::class.java)!!

                        //SI LA FECHA DEL EVENTO ES MAYOR QUE HOY SETEA EL EVENTO A NO DISPONIBLE
//                        El método compareTo() devuelve:
//
//                        Un número negativo si el objeto actual es menor que otro objeto
//                        Un número positivo si el objeto actual es mayor que otro objeto
//                        Cero si ambos objetos son iguales entre sí.
                        if (hoy_f.compareTo(pojo.fecha!!) > 0){
                            db_ref.child("tienda").child("eventos").child(pojo.id!!).child("activo").setValue(false)
                        }

                        if(Utilidades.esAdmin(applicationContext)){
                            lista.add(pojo)
                        }else{
                            if (pojo.visible!!){
                                lista.add(pojo)
                            }
                        }
                    }
                    recycler.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    println(error.message)
                }
            })
    }

    fun filtrarEventos(check : Int){
        db_ref.child("tienda").child("solicitudes_eventos").orderByChild("id_usuario").equalTo(Utilidades.obtenerIDuser(applicationContext))
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val pojo_sol = it.getValue(Solicitud::class.java)!!
                        println(pojo_sol)
                        println("ESTADO: "+pojo_sol.estado+" CHECK: "+check)
                        if (pojo_sol.estado == check) {
                            db_ref.child("tienda").child("eventos").child(pojo_sol.id_evento!!)
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        lista.clear()
                                        val pojo_ev = snapshot.getValue(Evento::class.java)!!
                                        println(pojo_ev)
                                        lista.add(pojo_ev)
                                        recycler.adapter?.notifyDataSetChanged()
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        TODO("Not yet implemented")
                                    }
                                })
                        }else{
                            lista.clear()
                            recycler.adapter?.notifyDataSetChanged()
                        }


                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_eventos, menu)
        val item = menu?.findItem(R.id.app_bar_search_events)
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


}