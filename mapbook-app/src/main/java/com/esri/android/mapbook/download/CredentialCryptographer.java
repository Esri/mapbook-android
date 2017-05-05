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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.esri.android.mapbook.Constants;
import com.esri.arcgisruntime.security.Credential;
import com.google.common.base.Charsets;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
  private static final String SHARED_PREFERENCE_NAME = "MAPBOOK_PREFERENCES";
  private String mUserName = null;

  @Inject Context mContext;

  public CredentialCryptographer(final Context context){
    mContext   = context;
  }

  /**
   * Entry point for encrypting bytes. Encryption/decryption
   * methods are dependent on underlying OS Version.
   * @param bytes - array of bytes to encrypt
   * @return - File path representing location of encrypted data.
   * @throws - Exception related to encryption/decryption
   */
  public String encrypt(final byte[] bytes, final String filePath) throws Exception{
    return encryptData(bytes, filePath);
  }

  /**
   * Entry point for decrypting a file given
   * @return String representing decrypted data
   */
  public String decrypt() throws Exception{
    return decryptData(Constants.CRED_FILE);
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

    }catch (KeyStoreException | InvalidAlgorithmParameterException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException | IOException  kS){
      Log.e(TAG, kS.getMessage());
    }
  }

  /**
   * Return the absolute file path for a file in the app directory
   * @param fileName - String representing file name
   * @return String representing the absolute path to the file
   */
  private String getFilePath(final String fileName){
    String filepath = null;
    final File [] dirFiles = ContextCompat.getExternalFilesDirs(mContext, null);

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
   * @throws Exception - Throws Exceptions related to encryption
   */
  private String encryptData(final byte [] input,  final String fileName) throws Exception{

    String encryptedDataFilePath = null;

    final KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
    keyStore.load(null);

    // Does the key need to be created?
    if (!keyStore.containsAlias(ALIAS)){
      createNewKey();
    }
    final SecretKey key = (SecretKey) keyStore.getKey(ALIAS, null);

    final Cipher c = Cipher.getInstance(CIPHER_TYPE);
    c.init(Cipher.ENCRYPT_MODE, key);

    // Persist the GCMParamterSpec bytes to file for later use
    GCMParameterSpec spec = c.getParameters().getParameterSpec(GCMParameterSpec.class);
     final FileOutputStream fos = new FileOutputStream(getFilePath(Constants.IV_FILE));
    fos.write(spec.getIV());
    Log.i(TAG, "IV Length is " + spec.getIV().length+ " tag length is " + spec.getTLen());
    fos.close();

    encryptedDataFilePath = getFilePath(fileName);

    final CipherOutputStream cipherOutputStream =
        new CipherOutputStream(
            new FileOutputStream(encryptedDataFilePath), c);
    cipherOutputStream.write(input);
    cipherOutputStream.close();

    return encryptedDataFilePath;
  }

  /**
   * Decrypt contents of File given path and
   * return a string representation of the decrypted data
   * @param encryptedDataFileName String representing file name
   * @return Decrypted string or null if decryption fails
   * @throws Exception related to decryption
   */
  private String decryptData (final String encryptedDataFileName) throws Exception{
    String decryptedString = null;

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
    final FileInputStream fis = new FileInputStream(getFilePath(Constants.IV_FILE));
    final byte [] iv = new byte[ivFileSize];
    fis.read(iv, 0, ivFileSize);
    fis.close();
    GCMParameterSpec spec = new GCMParameterSpec(128, iv);
    Log.i(TAG, "Decrypted spec iv length " +  spec.getIV().length + " tag length = "+ spec.getTLen());
    c.init(Cipher.DECRYPT_MODE, key, spec);

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
    decryptedString = new String(fileContentBytes, 0, index, Charsets.UTF_8);
    Log.v(TAG, "Decrypted string = " + decryptedString);

    return decryptedString;
  }

  /**
   * Set the username based on encrypted credentials
   * @param jsonCredentialCache - A nullable JSON string representing CredentialCache
   *                            If null, then an attempt is made to extract user name from
   *                            encrypted credential file on device.
   * @throws Exception related to decrypting credentials or parsing JSON.
   */
  public void setUserNameFromCredentials(@Nullable String jsonCredentialCache) throws Exception{
    if (jsonCredentialCache == null){
      jsonCredentialCache = this.decrypt();
    }
    if (jsonCredentialCache != null && jsonCredentialCache.length() > 0){

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
    }
  }

  /**
   * Get the user name
   */
  public String getUserName(){
    return mUserName;
  }
}
