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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import com.esri.android.mapbook.Constants;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

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
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

  @Inject Context mContext;

  public CredentialCryptographer(Context context){
    mContext   = context;
  }

  /**
   * Create a new key in the Keystore
   */
  private void createNewKey(){
    SecretKey key = null;
    try {
      KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);

      // Build one key to be used for encrypting and decrypting the file
      keyGenerator.init(
          new KeyGenParameterSpec.Builder(ALIAS,
              KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      keyGenerator.generateKey();
      Log.i(TAG, "Key created in Keystore");

    }catch (KeyStoreException kS){
      Log.e(TAG, kS.getMessage());
    } catch (InvalidAlgorithmParameterException iape){
      Log.e(TAG, iape.getMessage());
    } catch (NoSuchProviderException prov){
      Log.e(TAG, prov.getMessage());
    } catch (NoSuchAlgorithmException algoE){
      Log.e(TAG, algoE.getMessage());
    } catch (CertificateException cert){
      Log.e(TAG, cert.getMessage());
    } catch (IOException io){
      Log.e(TAG, io.getMessage());
    }
  }

  private String getFilePath(String fileName){
    String filepath = null;
    File [] dirFiles = ContextCompat.getExternalFilesDirs(mContext, null);
    for (int x=0 ; x < dirFiles.length ; x++){
      File f = dirFiles[x];
      Log.i(TAG, f.getAbsolutePath());
      if (f.isDirectory()){
        File [] contents = f.listFiles();
        for (int y = 0; y < contents.length; y++){
          File dirContent = contents[y];
          Log.i(TAG, dirContent.getAbsolutePath());
        }
      }
    }
    // We don't encrypt files if we can't store them...
    if (dirFiles.length == 0){
      Log.i(TAG, "Data cannot be encrypted because no external directories were found.");
      return filepath;
    }else{
      File f = dirFiles[0];
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
  public String encryptData(byte [] input,  String fileName){

    String encryptedDataFilePath = null;
    try {
      KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      // Does the key need to be created?
      if (!keyStore.containsAlias(ALIAS)){
        createNewKey();
      }
      SecretKey key = (SecretKey) keyStore.getKey(ALIAS, null);

      Cipher c = Cipher.getInstance(CIPHER_TYPE);
      c.init(Cipher.ENCRYPT_MODE, key);

      // Persist the GCMParamterSpec src bytes to file for later use
      FileOutputStream fos = mContext.openFileOutput(Constants.IV_FILE, Context.MODE_PRIVATE);
      fos.write(c.getIV());
      fos.close();

      encryptedDataFilePath = getFilePath(fileName);

      CipherOutputStream cipherOutputStream =
          new CipherOutputStream(
              new FileOutputStream(encryptedDataFilePath), c);
      cipherOutputStream.write(input);
      cipherOutputStream.close();

    }catch (KeyStoreException ke){
      Log.e(TAG, ke.getMessage());
    } catch (NoSuchAlgorithmException algoE){
      Log.e(TAG, algoE.getMessage());
    } catch (CertificateException cert){
      Log.e(TAG, cert.getMessage());
    } catch (IOException io){
      Log.e(TAG, io.getMessage());
    } catch (NoSuchPaddingException pad){
      Log.e(TAG, pad.getMessage());
    } catch (UnrecoverableKeyException un){
      Log.e(TAG, un.getMessage());
    }catch (InvalidKeyException key){
      Log.e(TAG, key.getMessage());
    }
    return encryptedDataFilePath;
  }

  /**
   * Decrypt contents of File given path and
   * return a string representation
   * @param encryptedDataFileName String representing file name
   * @return Decrypted string or null if decryption fails
   */
  public String decryptData (String encryptedDataFileName){
    String decryptedString = null;
    try {

      KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);
      SecretKey key = (SecretKey) keyStore.getKey(ALIAS, null);

      Cipher c = Cipher.getInstance(CIPHER_TYPE);

      File file = new File(getFilePath(encryptedDataFileName));
      int fileSize = (int)file.length();

      // Need to provide the GCMSpec used by the
      // encryption method when decrypting
      File ivFile = new File(getFilePath(Constants.IV_FILE));
      int ivFileSize =  (int) ivFile.length();
      FileInputStream fis = mContext.openFileInput(Constants.IV_FILE);
      byte [] GMspec = new byte[ivFileSize];
      fis.read(GMspec, 0, ivFileSize);
      fis.close();
      c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, GMspec));

      CipherInputStream cipherInputStream =
          new CipherInputStream(new FileInputStream(getFilePath(encryptedDataFileName)),
              c);
      byte[] fileContentBytes = new byte[fileSize];

      int index = 0;
      int nextByte;
      while ((nextByte = cipherInputStream.read()) != -1) {
        fileContentBytes[index] = (byte) nextByte;
        index++;
      }
      decryptedString = new String(fileContentBytes, 0, index, "UTF-8");
      Log.v(TAG, "Decrypted string = " + decryptedString);

    }catch (KeyStoreException ke){
      Log.e(TAG, ke.getMessage());
    } catch (NoSuchAlgorithmException algoE){
      Log.e(TAG, algoE.getMessage());
    } catch (CertificateException cert){
      Log.e(TAG, cert.getMessage());
    } catch (IOException io){
      Log.e(TAG, io.getMessage());
    } catch (NoSuchPaddingException pad){
      Log.e(TAG, pad.getMessage());
    } catch (UnrecoverableKeyException un){
      Log.e(TAG, un.getMessage());
    } catch (InvalidKeyException key){
      Log.e(TAG, key.getMessage());
    } catch (InvalidAlgorithmParameterException iape){
      Log.e(TAG, iape.getMessage());
    }

    return decryptedString;
  }
  public String rsaEncryptData(byte[] data, String filename){
    String encryptedDataFilePath = null;
    try {
      KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);

      // Do the keys need to be created?
      if (!keyStore.containsAlias(ALIAS)) {
        generateRsaKeys();
        generateAndStoreAESKey();
      }

      // Do we have an AES key?
      Key aesKey = getSecretKey(mContext);


      byte [] encryptedBytes = encrypt(mContext, data);

      // Write contents to file in app directory
      String encodeFileName = getFilePath(Constants.CRED_FILE);

   //   FileOutputStream fos = mContext.openFileOutput(encodeFileName, Context.MODE_PRIVATE);
      FileOutputStream fos = new FileOutputStream(new File(encodeFileName));
      fos.write(encryptedBytes);
      fos.close();


    }catch (KeyStoreException ke){
      Log.e(TAG, ke.getMessage());
    } catch (NoSuchAlgorithmException algoE){
      Log.e(TAG, algoE.getMessage());
    } catch (CertificateException cert){
      Log.e(TAG, cert.getMessage());
    } catch (IOException io){
      Log.e(TAG, io.getMessage());
    } catch (Exception e){
      Log.e(TAG, e.getMessage());
    }
    return encryptedDataFilePath;
  }

  public String rsaDecrpytData(String encryptedFileName ){
    String decrpyptedData = null;
    try {
      File credFle = new File(getFilePath(Constants.CRED_FILE));
      byte [] data = Files.toByteArray(credFle);
      byte[] decryptedBytes = decrypt(mContext, data);
      decrpyptedData = new String(decryptedBytes, Charsets.UTF_8);

    }catch (IOException e){
      Log.e(TAG,e.getMessage());
    }
    return decrpyptedData;
  }

  private void generateRsaKeys(){
    try {
      KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);
      // Generate the RSA key pairs
      if (!keyStore.containsAlias(ALIAS)) {
        // Generate a key pair for encryption
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);
        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
            .setAlias(ALIAS)
            .setSubject(new X500Principal("CN=" + ALIAS))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(start.getTime())
            .setEndDate(end.getTime())
            .build();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);
        kpg.initialize(spec);
        kpg.generateKeyPair();
      }
    }catch (KeyStoreException ke){
        Log.e(TAG, ke.getMessage());
    } catch (NoSuchAlgorithmException algoE){
        Log.e(TAG, algoE.getMessage());
    } catch (InvalidAlgorithmParameterException iape){
        Log.e(TAG, iape.getMessage());
    }catch(NoSuchProviderException prov) {
      Log.e(TAG, prov.getMessage());
    } catch (CertificateException c){
      Log.e(TAG, c.getMessage());
    } catch (IOException e){
      Log.e(TAG, e.getMessage());
    }

  }
  private byte[] rsaEncrypt(byte[] secret) throws Exception{
    KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
    keyStore.load(null);
    KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(ALIAS, null);
    // Encrypt the text
    Cipher inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
    inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
    cipherOutputStream.write(secret);
    cipherOutputStream.close();

    byte[] vals = outputStream.toByteArray();
    return vals;
  }

  private  byte[]  rsaDecrypt(byte[] encrypted) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
    keyStore.load(null);

    KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(ALIAS, null);
    Cipher output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
    output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
    CipherInputStream cipherInputStream = new CipherInputStream(
        new ByteArrayInputStream(encrypted), output);
    ArrayList<Byte> values = new ArrayList<>();
    int nextByte;
    while ((nextByte = cipherInputStream.read()) != -1) {
      values.add((byte)nextByte);
    }

    byte[] bytes = new byte[values.size()];
    for(int i = 0; i < bytes.length; i++) {
      bytes[i] = values.get(i).byteValue();
    }
    return bytes;
  }

  /**
   * This is done one time only, when data is first encrypted.
   */
  private void generateAndStoreAESKey(){
    SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
    String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
    if (enryptedKeyB64 == null) {
      byte[] key = new byte[16];
      SecureRandom secureRandom = new SecureRandom();
      secureRandom.nextBytes(key);
      byte[] encryptedKey = new byte[0];
      try {
        encryptedKey = rsaEncrypt(key);
      } catch (Exception e) {
        e.printStackTrace();
      }
      enryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
      SharedPreferences.Editor edit = pref.edit();
      edit.putString(ENCRYPTED_KEY, enryptedKeyB64);
      edit.commit();

      if (pref.contains(ENCRYPTED_KEY)){
        Log.i(TAG,pref.getString(ENCRYPTED_KEY,"Not found"));
      }
    }
  }
  public byte[] encrypt(Context context, byte[] input) {
    Cipher c = null;
    byte[] encodedBytes = null;
    try {
      c = Cipher.getInstance(AES_MODE, "BC");

      c.init(Cipher.ENCRYPT_MODE, getSecretKey(mContext));
      encodedBytes = new byte[0];
      encodedBytes = c.doFinal(input);

    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
    } catch (BadPaddingException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
    } catch (Exception e){
      e.printStackTrace();
    }
    return encodedBytes;
  }


  public byte[] decrypt(Context context, byte[] encrypted) {
    byte [] decodedBytes = null;
    try{
      Cipher c = Cipher.getInstance(AES_MODE, "BC");
      c.init(Cipher.DECRYPT_MODE, getSecretKey(context));
      decodedBytes = c.doFinal(encrypted);
    }catch (IllegalBlockSizeException e) {
      e.printStackTrace();
    } catch (BadPaddingException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return decodedBytes;
  }

  private Key getSecretKey(Context context) throws Exception{
    SharedPreferences pref = context.getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
    String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
    // need to check null, omitted here
    byte[] encryptedKey = Base64.decode(enryptedKeyB64, Base64.DEFAULT);
    byte[] key = rsaDecrypt(encryptedKey);
    return new SecretKeySpec(key, "AES");
  }
  /**
   * Key Generation
   Generate a pair of RSA keys;
   Generate a random AES key;
   Encrypt the AES key using the RSA public key;
   Store the encrypted AES key in Preferences.

   Encrypting and Storing the data
   Retrieve the encrypted AES key from Preferences;
   Decrypt the above to obtain the AES key using the private RSA key;
   Encrypt the data using the AES key;

   Retrieving and decrypting the data
   Retrieve the encrypted AES key from Preferences;
   Decrypt the above to obtain the AES key using the private RSA key;
   Decrypt the data using the AES key
   */
}
