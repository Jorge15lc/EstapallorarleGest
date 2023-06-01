package com.example.estapallorarlegest.productos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Producto(
        var id: String ?= null,
        var nombre : String ?= null,
        var descripcion : String  ?= null,
        var precio : Double ?= null,
        var stock : Int ?= null,
        var categoria : String ?= null,
        var url_foto : String ?= null,
        var disponible : Boolean ?= true): Parcelable