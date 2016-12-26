package com.phoenix.android.phoenix;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ProductsActivity extends AppCompatActivity {

    private String TAG = ProductsActivity.class.getSimpleName();

    private ProgressDialog pDialog;
    private ListView lv;
    private Button btnLogout;

    //IP and port of the merchant server respectively
    //private static final String hostname = "10.0.0.89";
    private static final String hostname = "192.168.137.1";
    private static final int merchant_portNumber = 9995;

    private SQLiteHandler db;
    private SessionManager session;
    private String session_ctom;

    String merchant_name="CA_merchant";
    String merchant_file="ca_merchant_certificate.p12";
    KeyStore keyStore = null;
    Certificate merc_c =null;

    private int flag_products = 0;

    ArrayList<HashMap<String, String>> productList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        productList = new ArrayList<>();
        lv = (ListView) findViewById(R.id.list);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        //get session_ctom from login activity
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null){
            session_ctom = extras.getString("session_ctom");
        }
        else
            try {
                session_ctom = generateSession_ctom();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        try {
            keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        //read merchant's certificate signed by CA
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

        new GetProducts().execute();
    }

    //logout user
    private void logoutUser() {
        db.deleteUsers();
        db.deleteProducts();

        // Launching the login activity
        Intent intent = new Intent(ProductsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private class GetProducts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(ProductsActivity.this);
            pDialog.setMessage("Getting the products...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket merchantSocket = null;
            try {
                PrivateKey clientPrivateKey = readPrivateKey();

                merchantSocket = new Socket(hostname, merchant_portNumber);
                DataOutputStream dos_products = new DataOutputStream(merchantSocket.getOutputStream());

                //send encrypted identifier separately and then send timestamp and session_ctom
                Log.e(TAG, "-Session_ctom sent: " + session_ctom);

                java.util.Date today = new java.util.Date();
                java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
                long tsTime_Products = ts1.getTime();

                String identifier = "Products";
                byte[] cipherTextIdentifier = encrypt(identifier);
                byte[] cipherTextTimeStamp = encrypt(Long.toString(tsTime_Products));
                byte[] cipherTextSession_ctom = encrypt(session_ctom);
                int byteArrayLength = cipherTextTimeStamp.length + cipherTextSession_ctom.length;
                byte[] bigByteArray = new byte[byteArrayLength];
                System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 0, cipherTextTimeStamp.length);
                System.arraycopy(cipherTextSession_ctom, 0, bigByteArray, cipherTextTimeStamp.length, cipherTextSession_ctom.length);

                dos_products.write(cipherTextIdentifier);
                dos_products.write(bigByteArray);
                dos_products.flush();

                //read products and hash of the session_ctom from client
                ObjectInputStream b_ois = new ObjectInputStream(merchantSocket.getInputStream());
                LinkedHashMap<String, String> products;
                products = (LinkedHashMap) b_ois.readObject();
                byte [] mdMerchantCombinedMerchant;
                mdMerchantCombinedMerchant = (byte[])b_ois.readObject();

                b_ois.close();

                byte[] byteSession_ctom = session_ctom.getBytes();
                byte[] byteProducts = products.toString().getBytes();

                int byteArrayCombinedLength = byteSession_ctom.length + byteProducts.length;
                byte[] bigByteCombinedArray = new byte[byteArrayCombinedLength];
                System.arraycopy(byteSession_ctom, 0, bigByteCombinedArray, 0, byteSession_ctom.length);
                System.arraycopy(byteProducts, 0, bigByteCombinedArray, byteSession_ctom.length, byteProducts.length);

                MessageDigest mdClientCombined = MessageDigest.getInstance("MD5");
                mdClientCombined.update(bigByteCombinedArray);
                byte[] messageDigestCombinedMD5 = mdClientCombined.digest();

                //calculate hash
                Log.e(TAG, "-Hash received: "+mdMerchantCombinedMerchant);
                Log.e(TAG, "-Hash generated: "+messageDigestCombinedMD5);
                boolean a = Arrays.equals(mdMerchantCombinedMerchant, messageDigestCombinedMD5);
                Log.e(TAG, "-Are Digests equal?(1-good/0-bad): " + a);

                //check for integrity
                if (a){
                    flag_products = 1;
                    Log.e(TAG, "-Integrity check successful");
                    for (Map.Entry<String, String> entry : products.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        LinkedHashMap<String, String> product = new LinkedHashMap<>();
                        // adding each child node to HashMap key => value
                        product.put("name", key);
                        product.put("price", value);
                        db.addProduct(key, value);
                        productList.add(product);
                        Log.e(TAG, product.toString());
                        Log.e(TAG, productList.toString());
                    }
                }
                else {
                    flag_products = 0;
                    Log.e(TAG, "-Integrity check unsuccessful");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
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
            /**
             * Updating parsed JSON data into ListView
             * */

            if(flag_products == 1) {
                ListAdapter adapter = new SimpleAdapter(
                        ProductsActivity.this, productList,
                        R.layout.list_item, new String[]{"name", "price"
                }, new int[]{R.id.name,
                        R.id.price});

                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        HashMap<String, String> obj = (HashMap<String, String>) lv.getItemAtPosition(i);
                        String name = obj.get("name");
                        String price = obj.get("price");

                        //pass product name, product price and session_ctom to payment activity
                        Intent intent = new Intent(ProductsActivity.this, PaymentActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("selected_product_name", name);
                        extras.putString("selected_product_price", price);
                        extras.putString("session_ctom", session_ctom);
                        intent.putExtras(extras);
                        startActivity(intent);
                        finish();

                    }
                });
            }
            else {
                Toast.makeText(getApplicationContext(),
                        "Product Listing Failed!", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    //generates session key
    public static String generateSession_ctom() throws NoSuchAlgorithmException {

        // Create a secure random number generator
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        // Create two secure number generators with the same seed
        int seedByteCount = 10;
        byte[] seed = sr.generateSeed(seedByteCount);
        sr.setSeed(seed);
        return String.valueOf(sr.nextInt());
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
