package com.example.estapallorarlegest.eventos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Evento(
    var id : String ?= null,
    var nombre : String ?= null,
    var fecha : String ?= null,
    var precio : Double ?= null,
    var aforo : Int ?= null,
    var aforo_rest : Int ?= null,
    var imagen : String ?= null,
    var activo : Boolean ?= true,
    var visible : Boolean ?= true
) :Parcelable