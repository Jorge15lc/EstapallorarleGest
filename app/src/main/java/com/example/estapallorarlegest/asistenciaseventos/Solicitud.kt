package com.example.estapallorarlegest.asistenciaseventos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Solicitud(
            var id : String ?= null,
            var id_evento : String ?= null,
            var id_usuario : String ?= null,
            var nom_usuario : String ?= null,
            var img_usuario : String ?= null,
            //0 Pendiente, 1 Aceptado, 2 Rechazado
            var estado : Int ?= 0):Parcelable