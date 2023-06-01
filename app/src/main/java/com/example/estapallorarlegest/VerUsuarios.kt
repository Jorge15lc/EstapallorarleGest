package com.example.estapallorarlegest

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class VerUsuarios :AppCompatActivity(){

    val recycler : RecyclerView by lazy {
        findViewById(R.id.recycler_users)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private lateinit var lista : MutableList<Usuario>
    private lateinit var adaptador: UsuariosAdaptador

    override fun onResume() {
        super.onResume()
        if(!Utilidades.esAdmin(applicationContext)){
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_usuarios)
        title = "Ver Usuarios"

        lista = mutableListOf()

        db_ref.child("tienda").child("usuarios")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    snapshot.children.forEach{
                        val pojo = it?.getValue(Usuario::class.java)!!
                        if(pojo.id != Utilidades.obtenerIDuser(applicationContext)){
                            lista.add(pojo)
                        }
                    }
                    recycler.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        adaptador = UsuariosAdaptador(lista)
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