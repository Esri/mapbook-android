/*
 *  Copyright 2017 Esri
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *  * For additional information, contact:
 *  * Environmental Systems Research Institute, Inc.
 *  * Attn: Contracts Dept
 *  * 380 New York Street
 *  * Redlands, California, USA 92373
 *  *
 *  * email: contracts@esri.com
 *  *
 *
 */

package com.esri.android.mapbook.download;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import com.esri.android.mapbook.Constants;
import com.esri.arcgisruntime.security.Credential;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.Key;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Class that handles AES encryption and decryption of a string and persisting
 * the encrypted string in a file within the application directory.
 */

public class CredentialCryptographer {
  private static final String TAG = CredentialCryptographer.class.getSimpleName();
  private static final String CIPHER_TYPE = "AES/GCM/NoPadding";
  private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";
  private static final String AES_MODE = "AES/ECB/PKCS7Padding";
  private static final String AndroidKeyStore = "AndroidKeyStore";
  private static final String ALIAS = "CRED_KEY";
  private static final String ENCRYPTED_KEY = "ENCRYPTED_KEY";
  private static final String SHARED_PREFENCE_NAME = "MAPBOOK_PREFERENCES";
  private String mUserName = null;

  @Inject Context mContext;

  public CredentialCryptographer(final Context context){
    mContext   = context;
  }

  /**
   * Entry point for encrypting bytes. Encryption/decryption
   * methods are dependent on underlying OS Version.
   * @param bytes - array of bytes to encrypt
   * @param filePath - the name of the file with encrypted bytes
   * @return - File path representing location of encrypted data.
   */
  public String encrypt(final byte[] bytes, final String filePath){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return encryptData(bytes, filePath);
    }else{
      return rsaEncryptData(bytes, filePath);
    }
  }

  /**
   * Entry point for decrypting a file given
   * @return String representing decrypted data
   */
  public String decrypt(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return decryptData(Constants.CRED_FILE);
    }else{
      return rsaDecryptData();
    }
  }

  /**
   * Delete credential file
   * @return boolean, true for successful delete, false for unsuccessful deletion
   */
  public boolean deleteCredentialFile(){
    final String filePath = getFilePath(Constants.CRED_FILE);
    final File f = new File(filePath);
    return f.delete();
  }

  /**
   * Create a new key in the Keystore
   */
  @TargetApi(23)
  private void createNewKey(){
    try {
      final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);

      // Build one key to be used for encrypting and decrypting the file
      keyGenerator.init(
          new KeyGenParameterSpec.Builder(ALIAS,
              KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      keyGenerator.generateKey();
      Log.i(TAG, "Key created in Keystore");

    }catch (KeyStoreException | InvalidAlgorithmParameterException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException  kS){
      Log.e(TAG, kS.getMessage());
    } catch (final IOException io){
      Log.e(TAG, io.getMessage());
    }
  }

  /**
   * Return the absolute file path for a file in the app directory
   * @param fileName - String representing file name
   * @return String representing the absolute path to the file
   */
  private final String getFilePath(final String fileName){
    String filepath = null;
    final File [] dirFiles = ContextCompat.getExternalFilesDirs(mContext, null);
    for (int x=0 ; x < dirFiles.length ; x++){
      final File f = dirFiles[x];
      Log.i(TAG, f.getAbsolutePath());
      if (f.isDirectory()){
        final File [] contents = f.listFiles();
        for (int y = 0; y < contents.length; y++){
          final File dirContent = contents[y];
          Log.i(TAG, dirContent.getAbsolutePath());
        }
      }
    }
    // We don't encrypt files if we can't store them...
    if (dirFiles.length == 0){
      Log.i(TAG, "Data cannot be encrypted because no app directories were found.");
      return filepath;
    }else{
      final File f = dirFiles[0];
      filepath = f.getAbsolutePath() + File.separator + fileName;
    }
    return filepath;
  }
  /**
   * Encrypt given bytes and persist contents to File with given filename.
   * @param input - byte[] to encrypt
   * @param fileName - String name of the encrypted file
   * @return String representing encrypted file or null if encryption fails.
   */
  @TargetApi(23)
  private String encryptData(final byte [] input,  final String fileName){

    String encryptedDataFilePath = null;
    try {
      final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      // Does the key need to be created?
      if (!keyStore.containsAlias(ALIAS)){
        createNewKey();
      }
      final SecretKey key = (SecretKey) keyStore.getKey(ALIAS, null);

      final Cipher c = Cipher.getInstance(CIPHER_TYPE);
      c.init(Cipher.ENCRYPT_MODE, key);

      // Persist the GCMParamterSpec src bytes to file for later use
       final FileOutputStream fos = mContext.openFileOutput(Constants.IV_FILE, Context.MODE_PRIVATE);
      fos.write(c.getIV());
      fos.close();

      encryptedDataFilePath = getFilePath(fileName);

      final CipherOutputStream cipherOutputStream =
          new CipherOutputStream(
              new FileOutputStream(encryptedDataFilePath), c);
      cipherOutputStream.write(input);
      cipherOutputStream.close();

    }catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchPaddingException
        | UnrecoverableKeyException | InvalidKeyException ke){
      Log.e(TAG, ke.getMessage());
    }catch (final IOException io){
      Log.e(TAG, io.getMessage());
    }
    return encryptedDataFilePath;
  }

  /**
   * Decrypt contents of File given path and
   * return a string representation of the decrypted data
   * @param encryptedDataFileName String representing file name
   * @return Decrypted string or null if decryption fails
   */
  @TargetApi(23)
  private String decryptData (final String encryptedDataFileName){
    String decryptedString = null;
    try {

      final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);
      final SecretKey key = (SecretKey) keyStore.getKey(ALIAS, null);

      final Cipher c = Cipher.getInstance(CIPHER_TYPE);

      final File file = new File(getFilePath(encryptedDataFileName));
      final int fileSize = (int)file.length();

      // Need to provide the GCMSpec used by the
      // encryption method when decrypting
      final File ivFile = new File(getFilePath(Constants.IV_FILE));
      final int ivFileSize =  (int) ivFile.length();
      final FileInputStream fis = mContext.openFileInput(Constants.IV_FILE);
      final byte [] GMspec = new byte[ivFileSize];
      fis.read(GMspec, 0, ivFileSize);
      fis.close();
      c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, GMspec));

       final CipherInputStream cipherInputStream =
          new CipherInputStream(new FileInputStream(getFilePath(encryptedDataFileName)),
              c);
      final byte[] fileContentBytes = new byte[fileSize];

      int index = 0;
      int nextByte;
      while ((nextByte = cipherInputStream.read()) != -1) {
        fileContentBytes[index] = (byte) nextByte;
        index++;
      }
      decryptedString = new String(fileContentBytes, 0, index, "UTF-8");
      Log.v(TAG, "Decrypted string = " + decryptedString);

    }catch (KeyStoreException | NoSuchAlgorithmException | CertificateException |
        NoSuchPaddingException | UnrecoverableKeyException | InvalidKeyException |
        InvalidAlgorithmParameterException |UnsupportedEncodingException ke){
      Log.e(TAG, ke.getMessage());
    } catch (final IOException io) {
      Log.e(TAG, io.getMessage());
    }

    return decryptedString;
  }
  /**  Workflow for RSA based encryption for pre-M devices
   *
   Step One: Key Generation
    1. Generate a pair of RSA keys;
    2. Generate a random AES key;
    3. Encrypt the AES key using the RSA public key;
    4. Store the encrypted AES key in Preferences.

   Step Two: Encrypting and Storing the data
    1. Retrieve the encrypted AES key from Preferences;
    2. Decrypt the above to obtain the AES key using the private RSA key;
    3. Encrypt the data using the AES key;

   Step Three: Retrieving and decrypting the data
    1. Retrieve the encrypted AES key from Preferences;
    2. Decrypt the above to obtain the AES key using the private RSA key;
    3. Decrypt the data using the AES key
   */

  /**
   * Encrypt data with RSA generated keys
   * @param data - Array of bytes to encrypt
   * @param filename - String name of file
   * @return - String representing absolute path to encrypted file or
   * null if an error occurs.
   */
  private String rsaEncryptData(final byte[] data, final String filename){
    String encryptedDataFilePath = null;
    try {
      final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      // Do the keys need to be created?
      if (!keyStore.containsAlias(ALIAS)) {
        generateRsaKeys();
        generateAndStoreAESKey();
      }

      final byte [] encryptedBytes = encrypt(mContext, data);

      // Write contents to file in app directory
      encryptedDataFilePath = getFilePath(Constants.CRED_FILE);

      final FileOutputStream fos = new FileOutputStream(new File(encryptedDataFilePath));
      fos.write(encryptedBytes);
      fos.close();


    }catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ke){
      Log.e(TAG, ke.getMessage());
    } catch (final Exception e){
      Log.e(TAG, e.getMessage());
    }
    return encryptedDataFilePath;
  }

  /**
   * Decrypt the credentials file and return decrypted data as a string
   * @return - String representing decrypted data or null if an exception occurs
   */
  private String rsaDecryptData( ){
    String decryptedData = null;
    try {
      final File credFle = new File(getFilePath(Constants.CRED_FILE));
      final byte [] data = Files.toByteArray(credFle);
      final byte[] decryptedBytes = decrypt(mContext, data);
      decryptedData = new String(decryptedBytes, Charsets.UTF_8);

    }catch (final IOException e){
      Log.e(TAG,e.getMessage());
    }
    return decryptedData;
  }

  /**
   * Generate RSA keys in the keystore
   */
  private void generateRsaKeys(){
    try {
      final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);
      // Generate the RSA key pairs
      if (!keyStore.containsAlias(ALIAS)) {
        // Generate a key pair for encryption
        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);
        final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
            .setAlias(ALIAS)
            .setSubject(new X500Principal("CN=" + ALIAS))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(start.getTime())
            .setEndDate(end.getTime())
            .build();
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);
        kpg.initialize(spec);
        kpg.generateKeyPair();
      }
    }catch (KeyStoreException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | CertificateException ke){
        Log.e(TAG, ke.getMessage());
    }catch (final IOException e){
      Log.e(TAG, e.getMessage());
    }

  }
  private byte[] rsaEncrypt(final byte[] secret) throws Exception{
    final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
    keyStore.load(null);
    final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(ALIAS, null);

    final Cipher inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
    inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
    cipherOutputStream.write(secret);
    cipherOutputStream.close();

    return outputStream.toByteArray();
  }

  /**
   * Use RSA keys to encrypt an array of bytes
   * @param encrypted - array of bytes to be encrypted
   * @return encrypted byte array
   * @throws Exception - Exception
   */
  private byte[] rsaDecrypt(final byte[] encrypted) throws Exception {
    final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
    keyStore.load(null);

    final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(ALIAS, null);
    final Cipher output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
    output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
     final CipherInputStream cipherInputStream = new CipherInputStream(
        new ByteArrayInputStream(encrypted), output);
    final List<Byte> values = new ArrayList<>();
    int nextByte;
    while ((nextByte = cipherInputStream.read()) != -1) {
      values.add((byte)nextByte);
    }

    final byte[] bytes = new byte[values.size()];
    for(int i = 0; i < bytes.length; i++) {
      bytes[i] = values.get(i);
    }
    return bytes;
  }

  /**
   * This is done one time only, when data is first encrypted.
   */
  private void generateAndStoreAESKey(){
    final SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
    String encryptedKey64 = pref.getString(ENCRYPTED_KEY, null);
    if (encryptedKey64 == null) {
      final byte[] key = new byte[16];
      final SecureRandom secureRandom = new SecureRandom();
      secureRandom.nextBytes(key);
      byte[] encryptedKey = new byte[0];
      try {
        encryptedKey = rsaEncrypt(key);
      } catch (final Exception e) {
        Log.e(TAG, e.getMessage());
      }
      encryptedKey64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
      final SharedPreferences.Editor edit = pref.edit();
      edit.putString(ENCRYPTED_KEY, encryptedKey64);
      edit.commit();

      // Log out the encrypted key
      if (pref.contains(ENCRYPTED_KEY)){
        Log.i(TAG,pref.getString(ENCRYPTED_KEY,"Not found"));
      }
    }
  }

  /**
   * Return bytes that have been encrypted
   * with a key stored in the SharedPreferences.
   * @param context - Context
   * @param input - array of bytes
   * @return Encrypted array of bytes or null if an exception is encountered
   */
  private byte[] encrypt(final Context context, final byte[] input) {
    Cipher c = null;
    byte[] encodedBytes = null;
    try {
      c = Cipher.getInstance(AES_MODE, "BC");
      // Encrypt bytes with a key stored in SharedPreferences
      c.init(Cipher.ENCRYPT_MODE, getSecretKey(mContext));
      encodedBytes = new byte[0];
      encodedBytes = c.doFinal(input);

    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException |
        NoSuchProviderException | NoSuchPaddingException e) {
      Log.e(TAG, e.getMessage());
    } catch (final Exception e){
      Log.e(TAG, e.getMessage());
    }
    return encodedBytes;
  }

  /**
   * Decrypt a given set of encrypted bytes
   * @param context - Context
   * @param encrypted - array of bytes
   * @return Array of decoded bytes or null if exception encountered
   */
  private byte[] decrypt(final Context context, final byte[] encrypted) {
    byte [] decodedBytes = null;
    try{
      final Cipher c = Cipher.getInstance(AES_MODE, "BC");
      c.init(Cipher.DECRYPT_MODE, getSecretKey(context));
      decodedBytes = c.doFinal(encrypted);
    }catch ( final IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException |
        NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e) {
      Log.e(TAG, e.getMessage());
    } catch ( final Exception e){
      Log.e(TAG, e.getMessage());
    }
    return decodedBytes;
  }

  /**
   * Retrieve encrypted key from SharedPreferences
   * @param context - Context
   * @return - decrpyted Key
   * @throws Exception
   */
  private Key getSecretKey(final Context context) throws Exception{
    final SharedPreferences pref = context.getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
    final String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
    // need to check null, omitted here
    final byte[] encryptedKey = Base64.decode(enryptedKeyB64, Base64.DEFAULT);
    final byte[] key = rsaDecrypt(encryptedKey);
    return new SecretKeySpec(key, "AES");
  }

  /**
   * Set the username based on encrypted credentials
   * @param jsonCredentialCache - A nullable JSON string representing CredentialCache
   *                            If null, then an attempt is made to extract user name from
   *                            encrypted credential file on device.
   */
  public void setUserNameFromCredentials(@Nullable String jsonCredentialCache){
    if (jsonCredentialCache == null){
      jsonCredentialCache = this.decrypt();
    }
    if (jsonCredentialCache != null){
      try {
        final JSONArray jsonArray = new JSONArray(jsonCredentialCache);
        if (jsonArray.get(0) != null){
          final JSONObject jsonCredentials = jsonArray.getJSONObject(0);
          if (jsonCredentials.has("credential")){
            final String jCred = jsonCredentials.getString("credential");
            final Credential credential = Credential.fromJson(jCred);

            if (credential != null){
              mUserName = credential.getUsername();
              Log.i(TAG, "****SETTING user name from credentials " + mUserName);
            }
          }
        }
      } catch (final JSONException e) {
        Log.e(TAG, "JSON exception " + e.getMessage());
      }
    }
  }

  /**
   * Get the user name
   */
  public String getUserName(){
    return mUserName;
  }
}
