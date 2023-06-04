package com.example.estapallorarlegest.pedidos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Utilidades
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PedidosAdaptador(var lista_pedidos : MutableList<Pedido>):RecyclerView.Adapter<PedidosAdaptador.PedidoViewHolder>(), Filterable {
    private lateinit var contexto : Context
    private var lista_filtrada : MutableList<Pedido> = lista_pedidos
    private var db_ref : DatabaseReference = FirebaseDatabase.getInstance().reference

    inner class PedidoViewHolder(item:View): RecyclerView.ViewHolder(item){
        val nombre_prod : TextView = item.findViewById(R.id.nom_pedido_prod)
        val img_ped : ImageView = item.findViewById(R.id.img_pedido_prod)
        val estado_tv : TextView = item.findViewById(R.id.ped_estado_tv)
        val estado_img : ImageView = item.findViewById(R.id.ped_estado_img)
        val cliente : TextView = item.findViewById(R.id.ped_nom_cli_tv)
        val cliente_tv : TextView = item.findViewById(R.id.ped_nom_cli_ex)
        val accept_btn : ImageView = item.findViewById(R.id.acept_ped_img)
        val constraint_accept : ConstraintLayout = item.findViewById(R.id.bottom_ped_row)
        val denegate_btn : ImageView = item.findViewById(R.id.denegate_ped_img)
        val recogido_btn : Button = item.findViewById(R.id.ped_recogido_btn)
        val listo_btn : Button = item.findViewById(R.id.ped_listo_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val vista_item= LayoutInflater.from(parent.context).inflate(R.layout.fila_item_pedido,parent, false)
        //Para poder hacer referencia al contexto de la aplicacion
        contexto = parent.context

        return PedidoViewHolder(vista_item)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val item_actual = lista_filtrada[position]

        holder.nombre_prod.text = item_actual.nom_producto
        //Adaptar en funcion de lo que sea

        //Diferencia para admins y normales
        var status = ""
        if (Utilidades.esAdmin(contexto)){
            when (item_actual.estado){
                0 -> {
                    status = "Esperando..."
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_watch_later_24))
                    holder.constraint_accept.visibility = View.VISIBLE
                }
                1 -> {
                    status = "Aceptado"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_check_circle_24))
                    holder.listo_btn.visibility = View.VISIBLE
                }
                2 -> {
                    status = "Listo Para Recoger"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_store_24))
                    holder.recogido_btn.visibility = View.VISIBLE
                }
                3 -> {
                    status = "Recogido"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_cookie_24))
                }
                4 -> {
                    status = "Rechazado"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_remove_circle_24))
                }
            }
            //Comportamiento btn
            //Paso 1 aceptar o denegar ESTADO = 0

            holder.accept_btn.setOnClickListener {
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("estado").setValue(1)
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("notificado").setValue(false)
                holder.constraint_accept.visibility = View.INVISIBLE
                holder.listo_btn.visibility = View.VISIBLE
            }

            holder.denegate_btn.setOnClickListener {
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("estado").setValue(4)
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("notificado").setValue(false)
                holder.constraint_accept.visibility = View.INVISIBLE
            }

            //Paso 2 pasar a listo para recoger ESTADO = 1

            holder.listo_btn.setOnClickListener {
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("estado").setValue(2)
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("notificado").setValue(false)
                holder.listo_btn.visibility = View.INVISIBLE
                holder.recogido_btn.visibility = View.VISIBLE
            }

            //Paso 3 pasar a recogido ESTADO = 2

            holder.recogido_btn.setOnClickListener {
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("estado").setValue(3)
                db_ref.child("tienda").child("pedidos").child(item_actual.id!!).child("notificado").setValue(false)
                holder.recogido_btn.visibility = View.INVISIBLE
            }
        }else{
            holder.constraint_accept.visibility = View.GONE
            holder.cliente.visibility = View.GONE
            holder.cliente_tv.visibility = View.GONE
            when (item_actual.estado){
                0 -> {
                    status = "Esperando..."
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_watch_later_24))
                }
                1 -> {
                    status = "Aceptado"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_check_circle_24))
                }
                2 -> {
                    status = "Listo Para Recoger"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_store_24))
                }
                3 -> {
                    status = "Recogido"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_cookie_24))
                }
                4 -> {
                    status = "Rechazado"
                    holder.estado_img.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_remove_circle_24))
                }
            }
        }

        holder.estado_tv.text = status
        holder.cliente.text = item_actual.nom_comprador
        Glide.with(contexto)
            .asBitmap()
            .apply(Utilidades.opcionesGlide(contexto))
            .transition(Utilidades.transicion)
            .load(item_actual.url_prod)
            .into(holder.img_ped)

    }

    override fun getItemCount(): Int {
        return  lista_filtrada.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(busqueda: CharSequence?): FilterResults {
                var buscado = busqueda!!.toString().lowercase()

                if (buscado.isEmpty()) {
                    lista_filtrada = lista_pedidos
                } else {
                    lista_filtrada = (lista_pedidos.filter {
                        it.nom_producto!!.lowercase().contains(busqueda)
                    }) as MutableList<Pedido>
                }

                val res_filter = FilterResults()
                res_filter.values = lista_filtrada
                return res_filter
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                notifyDataSetChanged()
            }

        }
    }
}