package com.example.estapallorarlegest.eventos

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.estapallorarlegest.*
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.asistenciaseventos.Solicitud
import com.example.estapallorarlegest.asistenciaseventos.VerSolicitudes
import com.example.estapallorarlegest.asistenciaseventos.VerUsuariosApuntados
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EventoAdaptador(var lista_temas : MutableList<Evento>): RecyclerView.Adapter<EventoAdaptador.EventoViewHolder>(), Filterable {
    private lateinit var contexto : Context
    private var lista_filtrada : MutableList<Evento> = lista_temas
    private var db_ref : DatabaseReference = FirebaseDatabase.getInstance().reference
    var nombre_us = ""
    var img_us = ""
    var opcion_selected = 0

    inner class EventoViewHolder(item : View) : RecyclerView.ViewHolder(item){
        val nombre : TextView = item.findViewById(R.id.titulo_event2)
        val fecha : TextView = item.findViewById(R.id.fecha_event2)
        val precio : TextView = item.findViewById(R.id.precio_event2)
        val imagen : ImageView = item.findViewById(R.id.img_event2)
        val aforo_rest : TextView = item.findViewById(R.id.aforo_rest2)
        val chat : ImageView = item.findViewById(R.id.ir_chat_tema2)
        val mod : ImageView = item.findViewById(R.id.ir_mod_event2)
        val apuntarse : ImageView = item.findViewById(R.id.apuntarse_event_btn)
        val participantes : ImageView = item.findViewById(R.id.ver_solicitudes_events)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val vista_item= LayoutInflater.from(parent.context).inflate(R.layout.new_eventos_fila,parent, false)
        //Para poder hacer referencia al contexto de la aplicacion
        contexto = parent.context

        return EventoViewHolder(vista_item)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val item = lista_filtrada[position]!!

        val URL:String? = when(item.imagen){
            ""->null
            else -> item.imagen
        }

        holder.nombre.text = item.nombre
        holder.fecha.text = "Fecha: "+item.fecha
        holder.precio.text = "Precio: "+item.precio.toString()
        holder.aforo_rest.text = "Aforo Restante: "+item.aforo_rest.toString()

        val options = RequestOptions()
            .placeholder(Utilidades.animacion(contexto))
            .fallback(R.drawable.baseline_broken_image_24)
            .error(R.drawable.baseline_error_24)

        Glide.with(contexto)
            .asBitmap()
            .load(URL)
            .apply(options)
            .transition(Utilidades.transicion)
            .into(holder.imagen)

        if(Utilidades.esAdmin(contexto)){
            holder.mod.visibility = View.VISIBLE
            holder.participantes.visibility = View.VISIBLE

            holder.mod.setOnClickListener {
                contexto.startActivity(Intent(contexto, ModificarEvento::class.java).putExtra("evento", item))
            }
            holder.participantes.visibility = View.VISIBLE
            holder.participantes.setOnClickListener {
                contexto.startActivity(Intent(contexto, VerUsuariosApuntados::class.java).putExtra("evento", item))
            }

            holder.apuntarse.setOnClickListener {
                contexto.startActivity(Intent(contexto, VerSolicitudes::class.java).putExtra("evento", item))
            }
        }else{
            holder.apuntarse.visibility = View.VISIBLE
            holder.mod.visibility = View.GONE
            holder.participantes.visibility = View.GONE
            holder.apuntarse.setOnClickListener {
                Toast.makeText(contexto, "Manten pulsado para solicitar tu asistencia a "+item.nombre!!.uppercase(), Toast.LENGTH_SHORT).show()

            }

            holder.apuntarse.setOnLongClickListener {
                var existe = false
                GlobalScope.launch (Dispatchers.IO){
                    db_ref.child("tienda").child("solicitudes_eventos").orderByChild("id_usuario").equalTo(Utilidades.obtenerIDuser(contexto))
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                snapshot.children.forEach {
                                    val sol = it.getValue(Solicitud::class.java)!!
                                    if (sol.id_evento == item.id) {
                                        existe = true
                                        return@forEach
                                    }
                                }
                                if (item.aforo_rest.toString().toInt() > 0) {
                                    if (item.activo!!) {
                                        if (!existe) {
                                            //Pusheamos el hueco para la solicitud
                                            val id_sol =
                                                db_ref.child("tienda").child("solicitudes_eventos")
                                                    .push().key!!
                                            //consultamos los datos del usuario solicitante
                                            db_ref.child("tienda").child("usuarios")
                                                .child(Utilidades.obtenerIDuser(contexto))
                                                .addListenerForSingleValueEvent(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        val pojous =
                                                            snapshot.getValue(Usuario::class.java)!!
                                                        //seteamos los datos de la solicitud
                                                        db_ref.child("tienda")
                                                            .child("solicitudes_eventos")
                                                            .child(id_sol).setValue(
                                                            Solicitud(
                                                                id_sol,
                                                                item.id,
                                                                pojous.id,
                                                                pojous.nombre,
                                                                pojous.imagen
                                                            )
                                                        )
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        TODO("Not yet implemented")
                                                    }
                                                })
                                            Toast.makeText(
                                                contexto,
                                                "Te has apuntado a " + item.nombre!!.uppercase(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                contexto,
                                                "Ya estas apuntado a " + item.nombre!!,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(contexto, "El evento esta cancelado o ha pasado la fecha", Toast.LENGTH_SHORT).show()
                                    }

                                }else{
                                    Toast.makeText(
                                        contexto,
                                        "No hay mas plazas disponibles",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
                }
                true
            }
        }

        holder.chat.setOnClickListener {
            contexto.startActivity(Intent(contexto, ChatEvento::class.java).putExtra("chat_evento", item))
        }
    }

    override fun getItemCount(): Int {
        return lista_filtrada.size
    }


    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(busqueda: CharSequence?): FilterResults {
                var buscado = busqueda!!.toString().lowercase()

                if (buscado.isEmpty()){
                    lista_filtrada = lista_temas
                }else{
                    lista_filtrada = (lista_temas.filter {
                        it.nombre!!.lowercase().contains(busqueda)
                    }) as MutableList<Evento>
                }
                //
//                lista_filtrada = lista_filtrada.filter {
//                    val deporte_ind = Utilidades.lista_deportes.indexOf(it.categoria)
//                    deporte_ind !=-1 && selectedSports[deporte_ind]
//                } as MutableList<Tema>
//
                val res_filter = FilterResults()
                res_filter.values = lista_filtrada
//                println("Lista filtrada: $lista_filtrada")
                return res_filter
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                notifyDataSetChanged()
            }

        }
    }


}