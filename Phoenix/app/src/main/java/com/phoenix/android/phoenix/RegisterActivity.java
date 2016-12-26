package com.phoenix.android.phoenix;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.phoenix.android.phoenix.helper.SQLiteHandler;
import com.phoenix.android.phoenix.helper.SessionManager;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by Akash on 24-11-2016.
 */
public class RegisterActivity extends Activity {
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private Button btnRegister;
    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputEmail;
    private EditText inputPhoneNumber;
    private ProgressDialog pDialog;

    //IP and port of the merchant server respectively
    //private static final String hostname = "10.0.0.89";
    private static final String hostname = "192.168.137.1";
    private static final int merchant_portNumber = 9995;

    private SQLiteHandler db;
    int flag_output = -1;

    String username;
    String password;
    String email;
    String phoneNumber;

    String merchant_name="CA_merchant";
    String merchant_file="ca_merchant_certificate.p12";
    KeyStore keyStore = null;
    Certificate merc_c =null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputUsername = (EditText) findViewById(R.id.username);
        inputPassword = (EditText) findViewById(R.id.password);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        db = new SQLiteHandler(getApplicationContext());
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        try {
            keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        InputStream merc_stream=(getResources().openRawResource(R.raw.ca_merchant_certificate));
        try {
            keyStore.load(merc_stream, "password".toCharArray());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        //read merchant's certificate signed by CA
        try {
            merc_c = keyStore.getCertificate(merchant_name);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                //receive data from registration activity
                username = inputUsername.getText().toString().trim();
                password = inputPassword.getText().toString().trim();
                email = inputEmail.getText().toString().trim();
                phoneNumber = inputPhoneNumber.getText().toString().trim();

                // Check for empty data in the form
                if (!username.isEmpty() && !password.isEmpty() && !email.isEmpty() && !phoneNumber.isEmpty()) {
                    new runRegister().execute();
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

    }

    private class runRegister extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showing progress dialog
            pDialog = new ProgressDialog(RegisterActivity.this);
            pDialog.setMessage("Registering...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket merchantSocket = null;
            try {
                PrivateKey clientPrivateKey = readPrivateKey();
                merchantSocket = new Socket(hostname, merchant_portNumber);

                Log.e(TAG, "Sending data to merchant");
                //sends user credentials to merchant server
                DataOutputStream dos_register = new DataOutputStream(merchantSocket.getOutputStream());

                //encrypt identifier separately and then send encrypted registration details
                String identifier = "Registration";
                byte[] cipherTextIdentifier = encrypt(identifier);
                byte[] cipherTextUsername = encrypt(username);
                byte[] cipherTextPassword = encrypt(password);
                byte[] cipherTextEmail = encrypt(email);
                byte[] cipherTextPhoneNumber = encrypt(phoneNumber);
                int byteArrayLength = cipherTextUsername.length + cipherTextPassword.length +
                        cipherTextEmail.length + cipherTextPhoneNumber.length;
                byte[] bigByteArray = new byte[byteArrayLength];
                System.arraycopy(cipherTextUsername, 0, bigByteArray, 0, cipherTextUsername.length);
                System.arraycopy(cipherTextPassword, 0, bigByteArray, cipherTextUsername.length, cipherTextPassword.length);
                System.arraycopy(cipherTextEmail, 0, bigByteArray, cipherTextUsername.length + cipherTextPassword.length, cipherTextEmail.length);
                System.arraycopy(cipherTextPhoneNumber, 0, bigByteArray, cipherTextUsername.length + cipherTextPassword.length + cipherTextEmail.length, cipherTextPhoneNumber.length);

                //send encrypted identifier followed by encrypted registration details
                dos_register.write(cipherTextIdentifier);
                dos_register.write(bigByteArray);
                dos_register.flush();

                //read reply from client
                DataInputStream dis_register = new DataInputStream(merchantSocket.getInputStream());
                int length = 128;
                byte[] cipherText = null;
                if(length>0) {
                    cipherText = new byte[length];
                    dis_register.readFully(cipherText, 0, cipherText.length); // read the message
                }

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, clientPrivateKey);
                byte[] slice_text_message = Arrays.copyOfRange(cipherText, 0, 64);
                String textMessage = new String(cipher.doFinal(slice_text_message));
                byte[] slice_merchantTimeStamp = Arrays.copyOfRange(cipherText, 64, 128);
                String textMerchantTimeStamp = new String(cipher.doFinal(slice_merchantTimeStamp));
                long timeStamp_merchant = Long.parseLong(textMerchantTimeStamp);

                Log.e(TAG,"-Message: "+textMessage);
                Log.e(TAG,"-TimeStampMerchant: "+textMerchantTimeStamp);

                java.util.Date today = new java.util.Date();
                java.sql.Timestamp ts1_reply = new java.sql.Timestamp(today.getTime());
                long tsTime_reply = ts1_reply.getTime();

                //checking time condition
                if(tsTime_reply - timeStamp_merchant < 30000){
                    if(textMessage.equals("Successful")) {
                        Log.e(TAG, "-Connection established!");
                        flag_output = 1;
                    }
                    else{
                        Log.e(TAG, "-Connection denied!");
                        flag_output = 0;
                    }
                }
                else{
                    flag_output = 0;
                    Log.e(TAG, "-Connection denied");
                }

                merchantSocket.close();

            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            //if user is already registered successfully, start login activity
            if (flag_output == 1) {
                Log.e(TAG, "-Login activity is launched.");
                Intent intent = new Intent(RegisterActivity.this,
                        LoginActivity.class);
                startActivity(intent);
                finish();
            }
            //else show registration failed pop-up
            else {
                Toast.makeText(getApplicationContext(),
                        "Registration failed!", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    public byte[] encrypt(String plainText){
        byte[] cipherTextMerchant = null;
        try {
            PublicKey serverMerchantPublicKey = merc_c.getPublicKey();
            Cipher cipherMerchant = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipherMerchant.init(Cipher.ENCRYPT_MODE, serverMerchantPublicKey);
            cipherTextMerchant = cipherMerchant.doFinal(plainText.getBytes());

        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        return cipherTextMerchant;
    }

    public PrivateKey readPrivateKey() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        //InputStream in = new FileInputStream(fileName);
        InputStream client_stream=(getResources().openRawResource(R.raw.serverclientprivatekey));
        ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(client_stream));
        try {
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PrivateKey prKey = fact.generatePrivate(keySpec);
            return prKey;
        } finally {
            oin.close();
        }
    }


}


