package com.example.estapallorarlegest

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Usuario(
    var id: String ?= null,
    var nombre : String ?= null,
    var pass : String ?= null,
    var correo : String ?= null,
    var imagen : String ?= null,
    var admin : Boolean ?= false,
    var fecha_registro : String ?= null,
    var bloqueado : Boolean ?= false
    ):Parcelable