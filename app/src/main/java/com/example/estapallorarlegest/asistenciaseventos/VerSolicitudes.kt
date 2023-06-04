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

class VerSolicitudes : AppCompatActivity() {

    val recycler : RecyclerView by lazy{
        findViewById(R.id.recycler_solicitudes_events)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private lateinit var lista : MutableList<Solicitud>
    private lateinit var adaptador: SolicitudesAdaptador
    var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_solicitudes)

        val pojo_ev = intent.getParcelableExtra<Evento>("evento")!!

        //TODO USER VER LOS EVENTOS SOLICITADOS Y VER INSCRITOS HACER DESDE EVENTO CON ALERTDIALOG

        title = "Solicitudes de "+pojo_ev.nombre
        lista = mutableListOf()

        db_ref.child("tienda").child("solicitudes_eventos").orderByChild("id_evento").equalTo(pojo_ev.id)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    snapshot.children.forEach {
                        val pojo = it?.getValue(Solicitud::class.java)!!
                        lista.add(pojo)
                    }
                    recycler.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        adaptador = SolicitudesAdaptador(lista)
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