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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import com.esri.android.mapbook.Constants;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Class that handles AES encryption and decryption of a string and persisting
 * the encrypted string in a file within the application directory.
 */

public class CredentialCryptographer {
  private static final String TAG = CredentialCryptographer.class.getSimpleName();
  private static final String CIPHER_TYPE = "AES/GCM/NoPadding";
  private static final String AndroidKeyStore = "AndroidKeyStore";
  private static final String ALIAS = "CRED_KEY";
  KeyStore keyStore;
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
      keyStore = KeyStore.getInstance(AndroidKeyStore);
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
    String filesDirectory = mContext.getFilesDir().getAbsolutePath();
    return filesDirectory + File.separator + fileName;
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
}
