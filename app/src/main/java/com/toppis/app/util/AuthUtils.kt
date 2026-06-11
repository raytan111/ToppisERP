package com.toppis.app.util

import java.security.MessageDigest

object AuthUtils {

    /** Devuelve el SHA-256 de [password] como cadena hexadecimal de 64 caracteres. */
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Retorna true si [password] produce el mismo hash que [hash]. */
    fun verificarPassword(password: String, hash: String): Boolean =
        hashPassword(password) == hash
}

