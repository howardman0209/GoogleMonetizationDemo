package com.demo.google.monetization

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object Security {
    fun verifyPurchase(publicKeyBase64: String, signedData: String, signature: String): Boolean {
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val decodedKey = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(decodedKey)) as PublicKey

            val signatureBytes = Base64.decode(signature, Base64.DEFAULT)
            val sign = Signature.getInstance("SHA1withRSA")
            sign.initVerify(publicKey)
            sign.update(signedData.toByteArray())
            return sign.verify(signatureBytes)
        } catch (e: Exception) {
            return false
        }
    }
}