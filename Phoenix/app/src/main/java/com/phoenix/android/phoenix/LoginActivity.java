package com.phoenix.android.phoenix;

/**
 * Created by Akash on 22-10-2016.
 */

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
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;

import com.phoenix.android.phoenix.helper.SQLiteHandler;
import com.phoenix.android.phoenix.helper.SessionManager;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class LoginActivity extends Activity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private Button btnLogin;
    private Button btnLinkToRegister;
    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputEmail;
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
    String session_ctom;

    String merchant_name="CA_merchant";
    String merchant_file="ca_merchant_certificate.p12";
    String file_client_private_key="serverclientprivatekey";
    KeyStore keyStore = null;
    Certificate merc_c =null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputUsername = (EditText) findViewById(R.id.username);
        inputPassword = (EditText) findViewById(R.id.password);
        inputEmail = (EditText) findViewById(R.id.email);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLinkToRegister = (Button) findViewById(R.id.btnLinkToRegisterScreen);
        db = new SQLiteHandler(getApplicationContext());
        db.deleteUsers();
        db.deleteProducts();

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

        //progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        //login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                username = inputUsername.getText().toString().trim();
                password = inputPassword.getText().toString().trim();
                email = inputEmail.getText().toString().trim();

                //check for empty data in the form
                if (!username.isEmpty() && !password.isEmpty() && !email.isEmpty()) {
                    new LoginRegister().execute();
                } else {
                    //prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    //function to create session identifier - session_ctom
    public static String generateSession_ctom() throws NoSuchAlgorithmException {

        // Create a secure random number generator
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        // Create two secure number generators with the same seed
        int seedByteCount = 10;
        byte[] seed = sr.generateSeed(seedByteCount);
        sr.setSeed(seed);

        return String.valueOf(sr.nextInt());
    }

    private class LoginRegister extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Logging in...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket merchantSocket = null;
            try {
                PrivateKey clientPrivateKey = readPrivateKey();
                merchantSocket = new Socket(hostname, merchant_portNumber);
                DataOutputStream dos_login = new DataOutputStream(merchantSocket.getOutputStream());

                ////encrypt identifier separately and then encrypt the login details along with session_ctom and time
                session_ctom = generateSession_ctom();
                Log.e(TAG, "-Session_ctom sent: " + session_ctom);

                java.util.Date today = new java.util.Date();
                java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
                long tsTime_login = ts1.getTime();

                String identifier = "Login";
                byte[] cipherTextIdentifier = encrypt(identifier);
                byte[] cipherTextUsername = encrypt(username);
                byte[] cipherTextPassword = encrypt(password);
                byte[] cipherTextEmail = encrypt(email);
                byte[] cipherTextTimeStamp = encrypt(Long.toString(tsTime_login));
                byte[] cipherTextSession_ctom = encrypt(session_ctom);
                int byteArrayLength = cipherTextUsername.length + cipherTextPassword.length +
                        cipherTextEmail.length + cipherTextTimeStamp.length + cipherTextSession_ctom.length;
                byte[] bigByteArray = new byte[byteArrayLength];
                System.arraycopy(cipherTextUsername, 0, bigByteArray, 0, cipherTextUsername.length);
                System.arraycopy(cipherTextPassword, 0, bigByteArray, cipherTextUsername.length, cipherTextPassword.length);
                System.arraycopy(cipherTextEmail, 0, bigByteArray, cipherTextUsername.length + cipherTextPassword.length, cipherTextEmail.length);
                System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, cipherTextUsername.length + cipherTextPassword.length + cipherTextEmail.length, cipherTextTimeStamp.length);
                System.arraycopy(cipherTextSession_ctom, 0, bigByteArray, cipherTextUsername.length + cipherTextPassword.length + cipherTextEmail.length + cipherTextTimeStamp.length, cipherTextSession_ctom.length);

                //send encrypted identifier followed by encrypted registration details
                dos_login.write(cipherTextIdentifier);
                dos_login.write(bigByteArray);
                dos_login.flush();

                //read reply from merchant
                DataInputStream dis_login = new DataInputStream(merchantSocket.getInputStream());
                int length = 256;
                byte[] cipherText = null;
                if(length>0) {
                    cipherText = new byte[length];
                    dis_login.readFully(cipherText, 0, cipherText.length); // read the message
                }

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, clientPrivateKey);
                byte[] slice_text_message = Arrays.copyOfRange(cipherText, 0, 64);
                String textMessage = new String(cipher.doFinal(slice_text_message));
                byte[] slice_merchantTimeStamp = Arrays.copyOfRange(cipherText, 64, 128);
                String textMerchantTimeStamp = new String(cipher.doFinal(slice_merchantTimeStamp));
                long timeStamp_merchant = Long.parseLong(textMerchantTimeStamp);
                byte[] slice_text_session_ctom = Arrays.copyOfRange(cipherText, 128, 192);
                String textSession_ctom = new String(cipher.doFinal(slice_text_session_ctom));
                byte[] slice_phone = Arrays.copyOfRange(cipherText, 192, 256);
                String textPhone = new String(cipher.doFinal(slice_phone));

                Log.e(TAG,"-Message: "+textMessage);
                Log.e(TAG,"-TimeStampMerchant: "+textMerchantTimeStamp);
                Log.e(TAG,"-Session_ctom received: "+textSession_ctom);
                Log.e(TAG,"-Phone: "+textPhone);

                java.util.Date today_reply = new java.util.Date();
                java.sql.Timestamp ts1_reply = new java.sql.Timestamp(today_reply.getTime());
                long tsTime_reply = ts1_reply.getTime();

                //checking time condition
                if(tsTime_reply - timeStamp_merchant < 30000){
                    if(textMessage.equals("Successful")) {
                        Log.e(TAG, "-Connection established!");
                        flag_output = 1;
                        db.addUser(username, email, textPhone, session_ctom);
                    }
                    else{
                        flag_output = 0;
                        Log.e(TAG, "-Connection denied!");
                    }
                }
                else{
                    flag_output = 0;
                    Log.e(TAG, "-Connection denied");
                }

                merchantSocket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
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

            //if login fails show pop-up
            if (flag_output == 0) {
                Toast.makeText(getApplicationContext(),
                        "-Login Failed!", Toast.LENGTH_LONG)
                        .show();
            } else {
                // Launch main activity and send session_ctom to products activity
                Log.e(TAG, "Launching main activity.");
                Intent intent = new Intent(LoginActivity.this,
                        ProductsActivity.class);
                Bundle extras = new Bundle();
                extras.putString("session_ctom", session_ctom);
                intent.putExtras(extras);
                startActivity(intent);
                finish();
            }
        }
    }

    //function to encrypt details to be sent to merchant
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

    //function reads private key of client from certificate signed by CA --used to decrypt messages from merchant
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



