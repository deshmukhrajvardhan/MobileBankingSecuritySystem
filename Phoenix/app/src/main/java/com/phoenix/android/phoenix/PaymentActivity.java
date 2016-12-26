package com.phoenix.android.phoenix;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.phoenix.android.phoenix.helper.DateDialog;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
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
 * Created by Akash on 26-10-2016.
 */
public class PaymentActivity extends Activity {
    private static final String TAG = PaymentActivity.class.getSimpleName();
    private Button btnPay;
    private Button btnGoBack;
    private EditText inputCardType;
    private EditText inputCardNumber;
    private EditText inputCardExpDate;
    private EditText inputCardCVV;
    private ProgressDialog pDialog;

    private static final String hostname = "192.168.137.1";
    //private static final String hostname = "10.0.0.89";
    private static final int merchant_portNumber = 9995;
    private SessionManager session;
    private SQLiteHandler db;
    int flag_output = -1;

    String product_name;
    String product_price;
    private String session_ctom;
    String cardType;
    String cardNumber;
    String cardExpDate;
    String cardCVV;

    String merchant_name="CA_merchant";
    String merchant_file="ca_merchant_certificate.p12";
    String bank_name="CA_Bank";
    String bank_file="ca_bank_certificate.p12";
    KeyStore keyStore = null;

    Certificate bank_c= null;
    Certificate merc_c =null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        inputCardType = (EditText) findViewById(R.id.card_type);
        inputCardNumber = (EditText) findViewById(R.id.card_number);
        inputCardExpDate = (EditText) findViewById(R.id.card_expiration_date);
        inputCardCVV = (EditText) findViewById(R.id.card_CVV);
        btnPay = (Button) findViewById(R.id.btnPay);
        btnGoBack = (Button) findViewById(R.id.btnGoBack);

        inputCardExpDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    DateDialog dialog = new DateDialog(v);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    dialog.show(ft, "DatePicker");
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        product_name = extras.getString("selected_product_name");
        session_ctom = extras.getString("session_ctom");
        product_price = extras.getString("selected_product_price");

        db = new SQLiteHandler(getApplicationContext());

        // Progress dialog
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


        try {
            merc_c = keyStore.getCertificate(merchant_name);
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

        //PayButtonClickEvent
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardType = inputCardType.getText().toString().trim();
                cardNumber = inputCardNumber.getText().toString().trim();
                cardExpDate = inputCardExpDate.getText().toString().trim();
                cardCVV = inputCardCVV.getText().toString().trim();

                //Check the fields
                if (!cardType.isEmpty() && !cardNumber.isEmpty() && !cardExpDate.isEmpty() && !cardCVV.isEmpty()) {
                    new runPay().execute();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please enter all the fields", Toast.LENGTH_LONG).show();
                }
            }
        });


        //executes when back button is pressed
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch Main activity
                Intent intent = new Intent(PaymentActivity.this, ProductsActivity.class);
                Bundle extras = new Bundle();
                extras.putString("session_ctom",session_ctom);
                intent.putExtras(extras);
                startActivity(intent);
                finish();

            }
        });
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private class runPay extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(PaymentActivity.this);
            pDialog.setMessage("Processing...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket merchantSocket = null;
            try {
                PrivateKey clientPrivateKey = readPrivateKey();
                Log.e(TAG, "Connection established!");

                java.util.Date today = new java.util.Date();
                java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
                long tsTime_Pay = ts1.getTime();

                merchantSocket = new Socket(hostname, merchant_portNumber);
                DataOutputStream dos = new DataOutputStream(merchantSocket.getOutputStream());

                String identifier = "Message";
                byte[] cipherTextIdentifier = encrypt_merchant(identifier);
                byte[] cipherTextTimeStamp = encrypt_merchant(Long.toString(tsTime_Pay));
                byte[] cipherTextSession_ctom = encrypt_merchant(session_ctom);

                //Computing encrypted info(length of each = 64)
                byte[] cipherTextBank = encrypt_bank(cardNumber);
                byte[] cipherTextMerchant = encrypt_merchant(product_name);

                //Computing individual hashes
                MessageDigest mdBank = MessageDigest.getInstance("MD5");
                MessageDigest mdMerchant = MessageDigest.getInstance("MD5");
                mdBank.update(cardNumber.getBytes());
                mdMerchant.update(product_name.getBytes());
                byte[] messageDigestBankMD5 = mdBank.digest();
                byte[] messageDigestMerchantMD5 = mdMerchant.digest();

                Log.e(TAG, "messageDigestMerchantMD5" + messageDigestMerchantMD5.length);

                //Computing dual hash(hash length = 16)
                byte[] messageDigestConcatenate = new byte[messageDigestBankMD5.length + messageDigestMerchantMD5.length];
                System.arraycopy(messageDigestBankMD5, 0, messageDigestConcatenate, 0, messageDigestBankMD5.length);
                System.arraycopy(messageDigestMerchantMD5, 0, messageDigestConcatenate, messageDigestBankMD5.length, messageDigestMerchantMD5.length);
                MessageDigest mdConcatenate = MessageDigest.getInstance("MD5");
                mdConcatenate.update(messageDigestConcatenate);
                byte[] combinedHash = mdConcatenate.digest();

                Cipher cipherSign = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherSign.init(Cipher.ENCRYPT_MODE, clientPrivateKey);
                byte[] cipherDualSignature = cipherSign.doFinal(combinedHash);



                //Creating the message to be sent to merchant and bank
                int byteArrayLength = cipherTextBank.length + cipherTextMerchant.length +
                        messageDigestBankMD5.length + messageDigestMerchantMD5.length +
                        cipherTextTimeStamp.length + cipherTextTimeStamp.length + cipherTextSession_ctom.length;
                byte[] bigByteArray = new byte[byteArrayLength];
                System.arraycopy(cipherTextBank, 0, bigByteArray, 0, cipherTextBank.length);
                System.arraycopy(cipherTextMerchant, 0, bigByteArray, cipherTextBank.length, cipherTextMerchant.length);
                System.arraycopy(messageDigestBankMD5, 0, bigByteArray, cipherTextBank.length + cipherTextMerchant.length, messageDigestBankMD5.length);
                System.arraycopy(messageDigestMerchantMD5, 0, bigByteArray, cipherTextBank.length + cipherTextMerchant.length + messageDigestBankMD5.length, messageDigestMerchantMD5.length);
                System.arraycopy(cipherDualSignature, 0, bigByteArray, cipherTextBank.length + cipherTextMerchant.length + messageDigestBankMD5.length + messageDigestMerchantMD5.length, cipherDualSignature.length);
                System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, cipherTextBank.length + cipherTextMerchant.length + messageDigestBankMD5.length + messageDigestMerchantMD5.length + cipherDualSignature.length, cipherTextTimeStamp.length);
                System.arraycopy(cipherTextSession_ctom, 0, bigByteArray, cipherTextBank.length + cipherTextMerchant.length + messageDigestBankMD5.length + messageDigestMerchantMD5.length + cipherDualSignature.length + cipherTextTimeStamp.length, cipherTextSession_ctom.length);

                Log.e(TAG, "bigByteArray.length" + bigByteArray.length);
                Log.e(TAG, "bigByteArray" + bigByteArray);

                dos.write(cipherTextIdentifier);
                dos.write(bigByteArray);
                dos.flush();

                //read reply from merchant
                DataInputStream dis_pay = new DataInputStream(merchantSocket.getInputStream());
                int length = 192;
                byte[] cipherText = null;
                if(length>0) {
                    cipherText = new byte[length];
                    dis_pay.readFully(cipherText, 0, cipherText.length); // read the message
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

                Log.e(TAG,"-Message: "+textMessage);
                Log.e(TAG,"-TimeStampMerchant: "+textMerchantTimeStamp);
                Log.e(TAG,"-Session_ctom received: "+textSession_ctom);

                java.util.Date today_reply = new java.util.Date();
                java.sql.Timestamp ts1_reply = new java.sql.Timestamp(today_reply.getTime());
                long tsTime_reply = ts1_reply.getTime();

                //checking time condition
                if(tsTime_reply - timeStamp_merchant < 30000){
                    if(textMessage.equals("Successful")) {
                        Log.e(TAG, "-Time condition satisfied and message received is successful");
                        flag_output = 1;
                    }
                    else{
                        Log.e(TAG, "-Message received is unsuccessful");
                        flag_output = 0;
                        Log.e(TAG, "-");
                    }
                }
                else{
                    flag_output = 0;
                    Log.e(TAG, "-Time condition not satisfied");
                }

                //Closing sockets
                merchantSocket.close();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            if (flag_output == 0) {
                Toast.makeText(getApplicationContext(),
                        "Payment Failed!", Toast.LENGTH_LONG)
                        .show();
            } else {
                // Launch OTP activity
                Intent intent = new Intent(PaymentActivity.this, OTPActivity.class);
                Bundle extras = new Bundle();
                extras.putString("session_ctom",session_ctom);
                intent.putExtras(extras);
                startActivity(intent);
                finish();
            }
        }
    }

    public byte[] encrypt_merchant(String plainText){
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
