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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by Akash on 23-11-2016.
 */
public class OTPActivity extends Activity {
    private static final String TAG = OTPActivity.class.getSimpleName();
    private EditText inputOTP;
    private Button btnSend;
    private Button btnGoBack;
    private ProgressDialog pDialog;
    //private static final String hostname = "10.0.0.89";
    private static final String hostname = "192.168.137.1";
    private static final int paymentGateway_portNumber = 9996;
    private SQLiteHandler db;
    String userOTP;

    String bank_name="CA_Bank";
    String bank_file="ca_bank_certificate.p12";
    KeyStore keyStore = null;
    Certificate bank_c= null;

    private String session_ctom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        inputOTP = (EditText) findViewById(R.id.otp);
        btnSend = (Button) findViewById(R.id.btnSendOTP);
        btnGoBack = (Button) findViewById(R.id.btnGoBack);
        db = new SQLiteHandler(getApplicationContext());
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        Bundle extras = getIntent().getExtras();
        session_ctom = extras.getString("session_ctom");

        try {
            keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        InputStream bank_stream=(getResources().openRawResource(R.raw.ca_bank_certificate));
        try {
            keyStore.load(bank_stream, "password".toCharArray());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }


        try {
            bank_c = keyStore.getCertificate(bank_name);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        //sends the OTP to bank server
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userOTP = inputOTP.getText().toString().trim();
                if (!userOTP.isEmpty()) {
                    new runOTP().execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter the OTP", Toast.LENGTH_LONG).show();
                }
            }
        });

        //goes back to main screen
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch Products activity
                Intent intent = new Intent(OTPActivity.this, ProductsActivity.class);
                Bundle extras = new Bundle();
                extras.putString("session_ctom",session_ctom);
                intent.putExtras(extras);
                startActivity(intent);
                finish();

            }
        });
    }

    public static String generateSession_ctom() throws NoSuchAlgorithmException {

        // Create a secure random number generator
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        // Create two secure number generators with the same seed
        int seedByteCount = 10;
        byte[] seed = sr.generateSeed(seedByteCount);

        //sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        return String.valueOf(sr.nextInt());
    }

    private class runOTP extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(OTPActivity.this);
            pDialog.setMessage("Registering...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket paymentGatewaySocket = null;
            try {
                paymentGatewaySocket = new Socket(hostname, paymentGateway_portNumber);

                Log.e(TAG, "Connection established!");

                //retrieves phone number from app's database
                //required because bank uses the phone number to find the OTP sent from
                //its database and it also serves as identification
                String phone = db.getPhone();
                DataOutputStream dos = new DataOutputStream(paymentGatewaySocket.getOutputStream());

                java.util.Date today = new java.util.Date();
                java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
                long tsTime_Pay = ts1.getTime();

                String identifier = "OTP";
                byte[] cipherTextIdentifier = encrypt_bank(identifier);
                byte[] cipherTextTimeStamp = encrypt_bank(Long.toString(tsTime_Pay));
                byte[] cipherTextOTP = encrypt_bank(Long.toString(Long.parseLong(userOTP)));
                byte[] cipherTextPhone = encrypt_bank(phone);

                int byteArrayLength = cipherTextTimeStamp.length + cipherTextOTP.length + cipherTextPhone.length;
                byte[] bigByteArray = new byte[byteArrayLength];
                System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 0, cipherTextTimeStamp.length);
                System.arraycopy(cipherTextOTP, 0, bigByteArray, cipherTextTimeStamp.length, cipherTextOTP.length);
                System.arraycopy(cipherTextPhone, 0, bigByteArray, cipherTextTimeStamp.length + cipherTextOTP.length, cipherTextPhone.length);

                dos.write(cipherTextIdentifier);
                dos.write(bigByteArray);
                dos.flush();

                Log.e(TAG, "Phone:" + phone);
                Log.e(TAG, "OTP sent:" + userOTP);

                paymentGatewaySocket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
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

            Intent intent = new Intent(OTPActivity.this, ProductsActivity.class);
            Bundle extras = new Bundle();
            extras.putString("session_ctom",session_ctom);
            intent.putExtras(extras);
            startActivity(intent);
            finish();
        }
    }

    public byte[] encrypt_bank(String plainText){
        byte[] cipherTextBank = null;
        try {
            PublicKey serverBankPublicKey = bank_c.getPublicKey();
            Cipher cipherBank = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipherBank.init(Cipher.ENCRYPT_MODE, serverBankPublicKey);
            cipherTextBank = cipherBank.doFinal(plainText.getBytes());

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

        return cipherTextBank;
    }

}