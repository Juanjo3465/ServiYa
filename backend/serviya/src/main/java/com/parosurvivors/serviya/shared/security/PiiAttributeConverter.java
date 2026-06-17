package com.parosurvivors.serviya.shared.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AttributeConverter de cifrado AES-256-GCM para campos PII (document_number, phone_number).
 * Convierte {@code String} (claro, en el dominio) &lt;-&gt; {@code byte[]} (cifrado, en BD,
 * columna VARBINARY). Cumple RNF-005.
 *
 * <p>Formato persistido: {@code IV(12 bytes) || ciphertext+tag}. La clave de 256 bits se deriva
 * por SHA-256 de la passphrase {@code ENCRYPTION_KEY} (cargada desde .env por DotEnvConfig); se
 * resuelve de forma perezosa porque Hibernate instancia el converter, no Spring.
 */
@Converter
public class PiiAttributeConverter implements AttributeConverter<String, byte[]> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String DEFAULT_PASSPHRASE = "serviya-dev-pii-key-change-me";

    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKeySpec cachedKey;

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt PII attribute", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] iv = Arrays.copyOfRange(dbData, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(dbData, IV_LENGTH, dbData.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt PII attribute", ex);
        }
    }

    private SecretKeySpec key() {
        SecretKeySpec local = cachedKey;
        if (local == null) {
            synchronized (this) {
                local = cachedKey;
                if (local == null) {
                    String passphrase = System.getProperty("ENCRYPTION_KEY", DEFAULT_PASSPHRASE);
                    try {
                        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                                .digest(passphrase.getBytes(StandardCharsets.UTF_8));
                        local = new SecretKeySpec(keyBytes, "AES");
                        cachedKey = local;
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to derive PII encryption key", ex);
                    }
                }
            }
        }
        return local;
    }
}
