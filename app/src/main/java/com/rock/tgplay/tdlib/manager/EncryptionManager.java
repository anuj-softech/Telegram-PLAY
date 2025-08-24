package com.rock.tgplay.tdlib.manager;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionManager {

    private Context applicationContext;

    public EncryptionManager(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public byte[] setEncryptionkey() {
        byte[] encryptionkey = retrieveKey();
        if (encryptionkey == null) {
            encryptionkey = generateSecureRandomKey();
            storeKey(encryptionkey);
        }
        return encryptionkey;
    }

    private final String ENCRYPTION_KEY = "encryption_key";

    public byte[] generateSecureRandomKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // Adjust the size based on your encryption algorithm's requirements
        secureRandom.nextBytes(key);
        return key;
    }

    public void storeKey(byte[] key) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences("client", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Encrypt the key before storing it
        String encryptedKey = Base64.getEncoder().encodeToString(key);
        editor.putString(ENCRYPTION_KEY, encryptedKey);

        editor.apply();
    }

    public byte[] retrieveKey() {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences("client", Context.MODE_PRIVATE);
        String encryptedKey = sharedPreferences.getString(ENCRYPTION_KEY, null);
        if (encryptedKey != null) {
            return Base64.getDecoder().decode(encryptedKey);
        } else {
            return null;
        }
    }
}
