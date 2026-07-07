package com.toppis.app.data.supabase

import com.toppis.erp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

/**
 * Cliente Supabase centralizado (Singleton).
 *
 * Provee acceso a:
 *  - Auth: autenticación de usuarios (login/logout)
 *  - Postgrest: queries CRUD a la base de datos PostgreSQL
 *  - Realtime: suscripciones a cambios en tiempo real
 *
 * Las credenciales se leen desde BuildConfig (configuradas en local.properties).
 */
object SupabaseClient {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Functions)
            install(Storage)
        }
    }
}
