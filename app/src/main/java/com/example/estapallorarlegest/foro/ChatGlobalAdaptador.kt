package com.example.estapallorarlegest.foro

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.estapallorarlegest.Mensaje
import com.example.estapallorarlegest.R
import com.example.estapallorarlegest.Utilidades

class ChatGlobalAdaptador(private val lista_mensajes : MutableList<Mensaje>) : RecyclerView.Adapter<ChatGlobalAdaptador.MensajeViewHolder>(){
    private lateinit var contexto : Context

    inner class MensajeViewHolder(itemView : View): RecyclerView.ViewHolder(itemView){
        val mensaje_der: TextView = itemView.findViewById(R.id.contenido_mensaje_der)
        val mensaje_izq : TextView = itemView.findViewById(R.id.contenido_mensaje_izq)
        val imagen_der : ImageView = itemView.findViewById(R.id.image_der)
        val imagen_izq : ImageView = itemView.findViewById(R.id.image_izq)
        val fecha_der : TextView = itemView.findViewById(R.id.fecha_mensaje_der)
        val fecha_izq : TextView = itemView.findViewById(R.id.fecha_mensaje_izq)
        val cons_der : ConstraintLayout = itemView.findViewById(R.id.msg_der)
        val cons_izq : ConstraintLayout = itemView.findViewById(R.id.msg_izq)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val vista_item = LayoutInflater.from(parent.context).inflate(R.layout.mensaje_item, parent, false)

        contexto = parent.context

        return MensajeViewHolder(vista_item)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val item = lista_mensajes[position]

        holder.cons_der.visibility = View.INVISIBLE
        holder.cons_izq.visibility = View.INVISIBLE

        if (item.id_emisor == item.id_dispositivo){
            //Mensaje Propio -> DERECHA
            holder.mensaje_der.text = item.contenido
            holder.fecha_der.text = item.fecha_hora
            holder.imagen_der.visibility = View.VISIBLE
            holder.imagen_izq.visibility = View.INVISIBLE
            holder.cons_der.visibility = View.VISIBLE
            holder.cons_izq.visibility = View.INVISIBLE
            Glide.with(contexto)
                .load(item.imagen_emisor)
                .apply(Utilidades.opcionesGlide(contexto))
                .into(holder.imagen_der)
        }else{
            //Mensaje Otra Persona -> IZQUIERDA
            holder.mensaje_izq.text = item.contenido
            holder.fecha_izq.text = item.fecha_hora
            holder.imagen_der.visibility = View.INVISIBLE
            holder.imagen_izq.visibility = View.VISIBLE
            holder.cons_der.visibility = View.INVISIBLE
            holder.cons_izq.visibility = View.VISIBLE
            Glide.with(contexto)
                .load(item.imagen_emisor)
                .apply(Utilidades.opcionesGlide(contexto))
                .into(holder.imagen_izq)
        }
    }


    override fun getItemCount(): Int = lista_mensajes.size
}