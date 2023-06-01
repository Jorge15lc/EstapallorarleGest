package com.example.estapallorarlegest.pedidos

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Utilidades
import com.google.firebase.database.*

class VerPedidos : AppCompatActivity() {

    val recycler : RecyclerView by lazy {
        findViewById(R.id.recycler_pedidos)
    }

    val db_ref : DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference()
    }

    private lateinit var lista : MutableList<Pedido>
    private lateinit var adaptador : PedidosAdaptador
    var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_pedidos_admin)

        if(Utilidades.esAdmin(applicationContext)){
            title = "Administrar Pedidos"
        }else{
            title = "Tus Pedidos"
        }

        //TODO NOTIFICACIONES ADMIN: NUEVO PEDIDO Y ENTREGADO, USER: RECHAZADO O ACEPTADO Y LISTO


        lista = mutableListOf<Pedido>()

        db_ref.child("tienda").child("pedidos")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    snapshot.children.forEach {
                        val pojo = it?.getValue(Pedido::class.java)
                        if (Utilidades.esAdmin(applicationContext)){
                            lista.add(pojo!!)
                        }else{
                            if (Utilidades.obtenerIDuser(applicationContext) == pojo!!.id_comprador){
                                lista.add(pojo!!)
                            }
                        }
                    }
                    recycler.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        adaptador = PedidosAdaptador(lista)
        recycler.adapter = adaptador
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.setHasFixedSize(true)

    }
    //TODO Implementar filtros por estados con checbox y alertdialog (como en categorias y eventos)
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