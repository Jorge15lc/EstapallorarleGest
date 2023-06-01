package com.example.estapallorarlegest

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Mensaje(var id:String?=null,
                   var id_emisor:String?=null,
                   var contenido:String?=null,
                   var fecha_hora:String?=null,
                   var id_dispositivo:String?=null,
                   var id_evento:String?="",
                   var id_prod:String?="",
                   var imagen_emisor:String?=null): Parcelable