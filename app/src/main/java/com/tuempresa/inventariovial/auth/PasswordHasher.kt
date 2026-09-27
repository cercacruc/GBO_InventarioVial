package com.tuempresa.inventariovial.auth

import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinVerifier(val salt: String, val pinHash: String, val hashIterations: Int) {
    fun json(): JSONObject = JSONObject().put("salt", salt).put("pinHash", pinHash).put("hashIterations", hashIterations)
    fun validate() {
        require(hashIterations in PasswordHasher.ITERATIONS..1_000_000)
        require(Base64.getDecoder().decode(salt).size == 32)
        require(Base64.getDecoder().decode(pinHash).size == 32)
    }
    companion object {
        fun parse(json: JSONObject) = PinVerifier(json.getString("salt"), json.getString("pinHash"), json.getInt("hashIterations")).also { it.validate() }
    }
}

object PasswordHasher {
    const val ITERATIONS = 210_000
    fun validPin(pin: CharArray) = pin.size in 6..12 && pin.all { it in '0'..'9' }
    fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun create(pin: CharArray): PinVerifier {
        require(validPin(pin)) { "El PIN debe tener entre 6 y 12 dígitos." }
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return PinVerifier(Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(derive(pin, salt, ITERATIONS)), ITERATIONS)
    }
    fun verify(pin: CharArray, verifier: PinVerifier): Boolean {
        verifier.validate()
        val actual = derive(pin, Base64.getDecoder().decode(verifier.salt), verifier.hashIterations)
        return try { MessageDigest.isEqual(actual, Base64.getDecoder().decode(verifier.pinHash)) }
        finally { actual.fill(0) }
    }
}
