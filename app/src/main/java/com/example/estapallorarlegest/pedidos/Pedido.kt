package com.example.estapallorarlegest.pedidos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pedido(
            var id : String ?= null,
    // 0 Peticion, 1 Aceptado, 2 Espera recoger, 3 Recogido, 4 Rechazado
            var estado : Int ?= 0,
            var id_producto : String ?= null,
            var nom_producto : String ?= null,
            var url_prod : String ?= null,
            var id_comprador : String ?= null,
            var nom_comprador : String ?= null,
            var timestamp : Long ?= null
                                        ):Parcelable