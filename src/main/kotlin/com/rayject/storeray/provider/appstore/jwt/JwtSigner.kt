package com.rayject.storeray.provider.appstore.jwt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

object JwtSigner {
    
    /**
     * 生成 App Store Connect API 所需的 JWT Token
     *
     * @param keyId 密钥 ID
     * @param issuerId 颁发者 ID
     * @param keyFilePath .p8 密钥文件路径
     * @param expirationMinutes 有效期（分钟），最长不超过 20 分钟
     */
    fun generateToken(
        keyId: String,
        issuerId: String,
        keyFilePath: String,
        expirationMinutes: Long = 20
    ): String {
        val privateKey = loadPrivateKey(keyFilePath)
        
        val header = buildJsonObject {
            put("alg", "ES256")
            put("kid", keyId)
            put("typ", "JWT")
        }
        
        val now = Instant.now().epochSecond
        val exp = now + (expirationMinutes * 60)
        
        val payload = buildJsonObject {
            put("iss", issuerId)
            put("iat", now)
            put("exp", exp)
            put("aud", "appstoreconnect-v1")
        }
        
        val headerB64 = base64UrlEncode(Json.encodeToString(header).toByteArray())
        val payloadB64 = base64UrlEncode(Json.encodeToString(payload).toByteArray())
        
        val dataToSign = "$headerB64.$payloadB64"
        val signatureBytes = sign(dataToSign.toByteArray(), privateKey)
        
        val signatureB64 = base64UrlEncode(signatureBytes)
        
        return "$dataToSign.$signatureB64"
    }

    private fun loadPrivateKey(filePath: String): ECPrivateKey {
        val file = File(filePath.replaceFirst("~", System.getProperty("user.home")))
        if (!file.exists()) {
            throw IllegalArgumentException("私钥文件不存在: ${file.absolutePath}")
        }
        
        // 移除 PEM 头部和尾部
        val keyContent = file.readText()
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
            
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        
        return keyFactory.generatePrivate(keySpec) as ECPrivateKey
    }

    private fun sign(data: ByteArray, privateKey: ECPrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        val asn1DerSignature = signature.sign()
        
        // ECDSA in JWT uses raw R and S concatenation (64 bytes for P-256), not ASN.1 DER format.
        return convertDerToRaw(asn1DerSignature)
    }
    
    private fun convertDerToRaw(der: ByteArray): ByteArray {
        // Simple ASN.1 DER to IEEE P1363 (Raw) format parser for ES256
        var offset = 2
        var rLength = der[offset + 1].toInt()
        if (rLength < 0) rLength += 256
        var rOffset = offset + 2
        
        // Skip leading zero byte if present
        if (der[rOffset].toInt() == 0) {
            rLength--
            rOffset++
        }
        
        offset = rOffset + rLength
        var sLength = der[offset + 1].toInt()
        if (sLength < 0) sLength += 256
        var sOffset = offset + 2
        
        // Skip leading zero byte if present
        if (der[sOffset].toInt() == 0) {
            sLength--
            sOffset++
        }
        
        val raw = ByteArray(64)
        System.arraycopy(der, rOffset, raw, 32 - rLength, rLength)
        System.arraycopy(der, sOffset, raw, 64 - sLength, sLength)
        
        return raw
    }

    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }
}
