package com.example.estapallorarlegest.asistenciaseventos

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
import com.example.estapallorarlegest.eventos.Evento
import com.google.firebase.database.*

class SolicitudesAdaptador(var lista_solicitudes : MutableList<Solicitud>): RecyclerView.Adapter<SolicitudesAdaptador.SolicitudViewHolder>(),
    Filterable {
    private lateinit var contexto : Context
    private var lista_filtrada : MutableList<Solicitud> = lista_solicitudes
    private var db_ref : DatabaseReference = FirebaseDatabase.getInstance().reference
    var ver_usuarios = false

    inner class SolicitudViewHolder(item: View): RecyclerView.ViewHolder(item){
        val nombre_user : TextView = item.findViewById(R.id.nom_user_sol)
        val img : ImageView = item.findViewById(R.id.img_user_sol)
        val accept : ImageView = item.findViewById(R.id.accept_sol_eve)
        val denegate : ImageView = item.findViewById(R.id.denegate_sol_eve)
        val row : ConstraintLayout = item.findViewById(R.id.bottom_soli_ev_row)
        val estado : ImageView = item.findViewById(R.id.status_sol_event)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SolicitudViewHolder {
        val vista_item= LayoutInflater.from(parent.context).inflate(R.layout.fila_solicitud_evento,parent, false)
        //Para poder hacer referencia al contexto de la aplicacion
        contexto = parent.context

        return SolicitudViewHolder(vista_item)
    }

    override fun onBindViewHolder(
        holder: SolicitudViewHolder,
        position: Int
    ) {
        val item = lista_filtrada[position]

        holder.nombre_user.text = item.nom_usuario
        Glide.with(contexto)
            .asBitmap()
            .apply(Utilidades.opcionesGlide(contexto))
            .transition(Utilidades.transicion)
            .load(item.img_usuario)
            .into(holder.img)

        if (!ver_usuarios){
            if (item.estado == 0){
                holder.row.visibility = View.VISIBLE
                holder.accept.setOnClickListener {
                    db_ref.child("tienda").child("solicitudes_eventos").child(item.id!!).child("estado").setValue(1)

                    //Restamos al aforo
                    db_ref.child("tienda").child("eventos").child(item.id_evento!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val pojo = snapshot.getValue(Evento::class.java)!!

                                db_ref.child("tienda").child("eventos").child(pojo.id!!).child("aforo_rest").setValue(pojo.aforo_rest!!-1)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }

                        })

                    holder.row.visibility = View.INVISIBLE
                    holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_check_circle_24))
                    holder.estado.visibility = View.VISIBLE
                    Toast.makeText(contexto, "Solicitud Aceptada", Toast.LENGTH_SHORT).show()
                }

                holder.denegate.setOnClickListener {
                    db_ref.child("tienda").child("solicitudes_eventos").child(item.id!!).child("estado").setValue(2)
                    holder.row.visibility = View.INVISIBLE
                    holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_remove_circle_24))
                    holder.estado.visibility = View.VISIBLE
                    Toast.makeText(contexto, "Solicitud Rechazada", Toast.LENGTH_SHORT).show()
                }

            }else if (item.estado == 1){
                holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_check_circle_24))
                holder.estado.visibility = View.VISIBLE
            }else{
                holder.estado.setImageDrawable(ContextCompat.getDrawable(contexto, R.drawable.ic_baseline_remove_circle_24))
                holder.estado.visibility = View.VISIBLE
            }
        }else{
            holder.row.visibility = View.GONE
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
                    lista_filtrada = lista_solicitudes
                } else {
                    lista_filtrada = (lista_solicitudes.filter {
                        it.nom_usuario!!.lowercase().contains(busqueda)
                    }) as MutableList<Solicitud>
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