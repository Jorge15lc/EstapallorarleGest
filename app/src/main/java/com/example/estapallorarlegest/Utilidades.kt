package com.example.estapallorarlegest

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class Utilidades {
    companion object {

        private val clave_id = "id"
        private val clave_tipo = "tipo"
        private val clave_url = "url"
        private val clave_mode = "mode"

        fun obtenerIDuser(contexto: Context): String {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)

            return SP.getString(clave_id, "") ?: ""
        }

        fun establecerIDuser(contexto: Context, id: String) {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)

            with(SP.edit()) {
                putString(clave_id, id)
                commit()
            }
        }

        fun obtenerUrlimgUser(contexto: Context): String {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)

            return SP.getString(clave_url, "") ?: ""
        }

        fun establecerUrlimgUser(contexto: Context, url: String) {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)

            with(SP.edit()) {
                putString(clave_url, url)
                commit()
            }
        }

        fun esAdmin(contexto: Context): Boolean {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)

            return SP.getBoolean(clave_tipo, false)
        }

        fun establecerTipoUser(contexto: Context, tipo: Boolean) {
            val ID = contexto.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = contexto.getSharedPreferences(sp_name, 0)


            with(SP.edit()) {
                putBoolean(clave_tipo, tipo)
                commit()
            }
        }


        fun getModeStatus(context: Context): Boolean {
            val ID = context.getString(R.string.app_id)
            val sp_modo = "${ID}_Datos_Usuarios"
            val SP = context.getSharedPreferences(sp_modo, 0)

            return SP.getBoolean(clave_mode ,false)
        }

        fun changeModeStatus(context: Context, value: Boolean, img : ImageView) {
            if (value) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                img.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_bedtime_24))
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                img.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_wb_sunny_24))
            }
            val ID = context.getString(R.string.app_id)
            val sp_name = "${ID}_Datos_Usuarios"
            val SP = context.getSharedPreferences(sp_name, 0)

            with(SP.edit()) {
                putBoolean(clave_mode, value)
                commit()
            }
        }

        fun animacion(contexto: Context): CircularProgressDrawable {
            val animar = CircularProgressDrawable(contexto)
            animar.strokeWidth = 5f
            animar.centerRadius = 40f
            animar.start()
            return animar
        }

        fun setStatusBorderColor(view: View, borderColor: Int) {
            val borderDrawable = view.background as GradientDrawable
            borderDrawable.setStroke(
                15, borderColor
            )
        }

        val transicion = BitmapTransitionOptions.withCrossFade(500)

        fun opcionesGlide(contexto: Context): RequestOptions {
            val options = RequestOptions()
                .placeholder(animacion(contexto))
                .fallback(R.drawable.baseline_broken_image_24)
                .error(R.drawable.baseline_error_24)
                .circleCrop()
            return options
        }

        suspend fun guardarImagen(
            sto_ref: StorageReference,
            id: String,
            imagen: Uri,
            sitio: String
        ): String {
            var url_img = sto_ref.child(sitio).child(id)
                .putFile(imagen).await().storage.downloadUrl.await()
            return url_img.toString()
        }

        val lista_categorias = listOf<String>(
            "Cookie",
            "Brownie",
            "Cheesecake",
            "Rollitos",
            "Otros"
        )
    }
}