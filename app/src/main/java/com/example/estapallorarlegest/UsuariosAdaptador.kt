package com.example.estapallorarlegest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UsuariosAdaptador(var lista_usuarios : MutableList<Usuario>): RecyclerView.Adapter<UsuariosAdaptador.UsuarioViewHolder>(), Filterable {
    private lateinit var contexto : Context
    private var lista_filtrada : MutableList<Usuario> = lista_usuarios
    private val db_ref : DatabaseReference = FirebaseDatabase.getInstance().reference

    inner class UsuarioViewHolder(item: View): RecyclerView.ViewHolder(item){
        val img : ImageView = item.findViewById(R.id.img_user_fila)
        val nombre : TextView = item.findViewById(R.id.nombre_user_fila)
        val correo : TextView = item.findViewById(R.id.correo_user_fila)
        val fecha : TextView = item.findViewById(R.id.fecha_user_fila)
        val activo : Switch = item.findViewById(R.id.ban_user)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UsuarioViewHolder {
        val vista_item= LayoutInflater.from(parent.context).inflate(R.layout.fila_usuario,parent, false)
        //Para poder hacer referencia al contexto de la aplicacion
        contexto = parent.context

        return UsuarioViewHolder(vista_item)
    }

    override fun onBindViewHolder(
        holder: UsuarioViewHolder,
        position: Int
    ) {
        val item = lista_filtrada[position]
        holder.nombre.text = item.nombre
        holder.correo.text = item.correo
        holder.fecha.text = item.fecha_registro
        holder.activo.isChecked = !item.bloqueado!!

        Glide.with(contexto)
            .asBitmap()
            .apply(Utilidades.opcionesGlide(contexto))
            .transition(Utilidades.transicion)
            .load(item.imagen)
            .into(holder.img)

        holder.activo.setOnCheckedChangeListener { button, b ->
            db_ref.child("tienda").child("usuarios").child(item.id!!).child("bloqueado").setValue(!b)
        }

    }

    override fun getItemCount(): Int {
        return lista_filtrada.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(busqueda: CharSequence?): FilterResults {
                var buscado = busqueda!!.toString().lowercase()

                if (buscado.isEmpty()) {
                    lista_filtrada = lista_usuarios
                } else {
                    lista_filtrada = (lista_usuarios.filter {
                        it.nombre!!.lowercase().contains(busqueda)
                    }) as MutableList<Usuario>
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