package com.example.estapallorarlegest.asistenciaseventos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.eventos.Evento
import com.google.firebase.database.*

class VerUsuariosApuntados : AppCompatActivity() {

    val recycler : RecyclerView by lazy{
        findViewById(R.id.recycler_usuarios_apuntados)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private lateinit var lista : MutableList<Solicitud>
    private lateinit var adaptador: SolicitudesAdaptador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_usuarios_apuntados)

        val pojo = intent.getParcelableExtra<Evento>("evento")!!
        title = "Asistentes a "+pojo.nombre
        lista = mutableListOf()

        db_ref.child("tienda").child("solicitudes_eventos").orderByChild("id_evento").equalTo(pojo.id)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    snapshot.children.forEach {
                        val pojo = it.getValue(Solicitud::class.java)!!
                        if (pojo.estado == 1){
                            lista.add(pojo)
                        }
                    }
                    recycler.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        adaptador = SolicitudesAdaptador(lista)
        adaptador.ver_usuarios = true
        recycler.adapter = adaptador
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.setHasFixedSize(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu?.findItem(R.id.app_bar_search)
        val searchView = item?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adaptador.filter.filter(p0)
                return true
            }
        })

        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                adaptador.filter.filter("")
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }
}