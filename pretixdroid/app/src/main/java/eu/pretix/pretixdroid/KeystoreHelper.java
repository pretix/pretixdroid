package eu.pretix.pretixdroid;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class KeystoreHelper {

    private static final String SECURE_KEY_NAME = "prefs_encryption_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    // Works since android M, for previous versions this will simply return the plain value unaltered
    public static String secureValue(String value, boolean encrypt) {
        if (value != null && value.length() > 0 && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                keyStore.load(null);
                SecretKey key = (SecretKey) keyStore.getKey(SECURE_KEY_NAME, null);

                if(key == null) {
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                    keyGenerator.init(new KeyGenParameterSpec.Builder(SECURE_KEY_NAME,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .setRandomizedEncryptionRequired(false)
                            .build());
                    keyGenerator.generateKey();
                    key = (SecretKey) keyStore.getKey(SECURE_KEY_NAME, null);
                }

                /*
                 *  It's quite ok to use ECB here, because:
                 *   - the "plaintext" will most likely fit into one or two encryption blocks
                 *   - the "plaintext" only contains pseudo-random printable characters
                 *   - we don't want to store an additional IV
                 */
                @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_ECB + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7);

                if (encrypt) {
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    byte[] bytes = cipher.doFinal(value.getBytes());
                    return Base64.encodeToString(bytes, Base64.NO_WRAP);
                } else {
                    cipher.init(Cipher.DECRYPT_MODE, key);
                    byte[] bytes = Base64.decode(value, Base64.NO_WRAP);
                    return new String(cipher.doFinal(bytes), "UTF-8");
                }

            } catch (UserNotAuthenticatedException e) {
                Log.d("KeystoreHelper","UserNotAuthenticatedException: " + e.getMessage());
                return value;
            } catch (KeyPermanentlyInvalidatedException e) {
                Log.d("KeystoreHelper","KeyPermanentlyInvalidatedException: " + e.getMessage());
                return value;
            } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                    CertificateException | UnrecoverableKeyException | NoSuchPaddingException |
                    NoSuchProviderException | IOException | InvalidAlgorithmParameterException |
                    NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }
}
