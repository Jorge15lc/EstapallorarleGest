package com.example.estapallorarlegest.productos

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Usuario
import com.example.estapallorarlegest.Utilidades
import com.example.estapallorarlegest.pedidos.Pedido
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ProductosAdaptador(var lista_prods : MutableList<Producto>, private val lifecycleScope : LifecycleCoroutineScope):RecyclerView.Adapter<ProductosAdaptador.ProductoViewHolder>(), Filterable {
    private lateinit var contexto : Context
    private var lista_filtrada : MutableList<Producto> = lista_prods
    private val db_ref : DatabaseReference = FirebaseDatabase.getInstance().reference
    var selectedCategories : MutableList<Boolean> = MutableList(Utilidades.lista_categorias.size){true}
    var nombre_us : String = ""
    var precio_min = 0.0f
    var precio_max = 5.0f
    var divisa_eur = true


    inner class ProductoViewHolder(item : View) : RecyclerView.ViewHolder(item) {
        val nombre : TextView = item.findViewById(R.id.item_nom_prod)
        val stock : TextView = item.findViewById(R.id.stock_prod_tv)
        val stock_tv : TextView = item.findViewById(R.id.textView1)
        val precio : TextView = item.findViewById(R.id.precio_prod_tv)
        val img : ImageView = item.findViewById(R.id.img_prod_item)
        val edit : ImageView = item.findViewById(R.id.edit_prod_item)
        val estado : ImageView = item.findViewById(R.id.status_prod_disp)
        val desc : TextView = item.findViewById(R.id.desc_prod_tv)
        val addcart :ImageView = item.findViewById(R.id.add_to_cart_prod)
        val chat : ImageView = item.findViewById(R.id.ir_chat_prod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val vista_item = LayoutInflater.from(parent.context).inflate(R.layout.item_prod_admin_layout, parent, false)

        contexto = parent.context

        return ProductoViewHolder(vista_item)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val item = lista_filtrada[position]

        val URL:String? = when(item.url_foto){
            ""-> null
            else -> item.url_foto
        }

        holder.nombre.text = item.nombre
        holder.stock.text = item.stock.toString()

        println("SETEO DEL ADAPTADOR: $divisa_eur")

        if (divisa_eur){
            holder.precio.text = item.precio.toString()+"€"
        }else{
            val dolares = 1.07
            val res = item.precio!!*dolares
            var formateado = "%.2f".format(res).toDouble()
            holder.precio.text = formateado.toString()+"$"
        }
        holder.desc.text = item.descripcion
        Glide.with(contexto)
            .asBitmap()
            .load(URL)
            .apply(Utilidades.opcionesGlide(contexto))
            .transition(Utilidades.transicion)
            .into(holder.img)

        holder.chat.setOnClickListener {
            contexto.startActivity(Intent(contexto, ChatProducto::class.java).putExtra("chat_prod", item))
        }

        if (Utilidades.esAdmin(contexto)){
            if (item.disponible!! ){
                holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto,
                    R.drawable.ic_baseline_brightness_2_24
                ))
            }else{
                holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto,
                    R.drawable.ic_baseline_brightness_1_24
                ))
            }
            holder.edit.visibility = View.VISIBLE
            holder.edit.setOnClickListener {
                contexto.startActivity(Intent(contexto, InfoProducto::class.java).putExtra("prod", item))
            }
        }else {
            holder.stock.visibility = View.GONE
            holder.stock_tv.visibility = View.GONE
            holder.estado.visibility = View.INVISIBLE
            holder.addcart.visibility = View.VISIBLE
            holder.addcart.setOnClickListener {
                Toast.makeText(contexto, "Manten pulsado para reservar", Toast.LENGTH_SHORT).show()
            }
            holder.addcart.setOnLongClickListener {
                val id_pedido = db_ref.child("tienda").child("pedidos").push().key!!

                lifecycleScope.launch(Dispatchers.IO) {
                    db_ref.child("tienda").child("usuarios").child(Utilidades.obtenerIDuser(contexto))
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val pojo = snapshot.getValue(Usuario::class.java)!!

                                db_ref.child("tienda").child("pedidos").child(id_pedido).setValue(
                                    Pedido(
                                        id_pedido,
                                        0,
                                        item.id,
                                        item.nombre,
                                        item.url_foto,
                                        pojo.id,
                                        pojo.nombre,
                                    )
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })

                }

                Toast.makeText(contexto, "Producto Pedido", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    private fun cambioDivisa(){

    }

    override fun getItemCount(): Int {
        return lista_filtrada.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(busqueda: CharSequence?): FilterResults {
                var buscado = busqueda!!.toString().lowercase()

                var listaFiltradaTemp: MutableList<Producto>

                if (buscado.isEmpty()) {
                    listaFiltradaTemp = lista_prods.toMutableList() // Hacer una copia de la lista original
                } else {
                    listaFiltradaTemp = lista_prods.filter {
                        it.nombre!!.lowercase().contains(busqueda)
                    } as MutableList<Producto>
                }

                listaFiltradaTemp = listaFiltradaTemp.filter {
                    it.precio!! in precio_min..precio_max
                } as MutableList<Producto>

                listaFiltradaTemp = listaFiltradaTemp.filter {
                    val category_index = Utilidades.lista_categorias.indexOf(it.categoria)
                    category_index != -1 && selectedCategories[category_index]
                } as MutableList<Producto>

                val res_filter = FilterResults()
                res_filter.values = listaFiltradaTemp
                return res_filter
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                lista_filtrada = p1?.values as MutableList<Producto>
                notifyDataSetChanged()
            }

        }

    }
}