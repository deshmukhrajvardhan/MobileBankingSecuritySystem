package com.phoenix.android.phoenix.helper;

/**
 * Created by Akash on 22-10-2016.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

public class SQLiteHandler extends SQLiteOpenHelper {

    private static final String TAG = SQLiteHandler.class.getSimpleName();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "phoenix_database";

    // Login table name
    private static final String TABLE_USER = "user";
    private static final String TABLE_PRODUCTS = "products";
    private static final String TABLE_CARD = "card";

    // Login Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_SESSION_CTOM = "SESSION_CTOM";
    private static final String KEY_ID_PRODUCT = "id";
    private static final String KEY_NAME_PRODUCT = "name";
    private static final String key_price = "price";
//    private static final String KEY_EMAIL = "email";
//    private static final String KEY_UID = "uid";
//    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_CARD_TYPE = "card_type";
    private static final String KEY_CARD_NUMBER = "card_number";
    private static final String KEY_CARD_EXP_DATE = "card_exp_date";
    private static final String KEY_CARD_CVV = "card_CVV";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
//        String CREATE_LOGIN_TABLE = "CREATE TABLE " + TABLE_USER + "("
//                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
//                + KEY_EMAIL + " TEXT UNIQUE," + KEY_UID + " TEXT,"
//                + KEY_CREATED_AT + " TEXT" + ")";
//        db.execSQL(CREATE_LOGIN_TABLE);

        String CREATE_LOGIN_TABLE = "CREATE TABLE " + TABLE_USER + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_EMAIL + " TEXT," + KEY_PHONE + " TEXT," + KEY_SESSION_CTOM + " TEXT" + ")";


        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME_PRODUCT + " TEXT,"
                + key_price + " TEXT" + ")";

        String CREATE_CARD_TABLE = "CREATE TABLE " + TABLE_CARD + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_CARD_TYPE + " TEXT,"
                + KEY_CARD_NUMBER + " TEXT,"+KEY_CARD_EXP_DATE +" TEXT,"+ KEY_CARD_CVV+" TEXT" + ")";

        db.execSQL(CREATE_LOGIN_TABLE);
        db.execSQL(CREATE_PRODUCTS_TABLE);
        db.execSQL(CREATE_CARD_TABLE);

        Log.d(TAG, "Database tables created");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARD);

        // Create tables again
        onCreate(db);
    }

    /**
     * Storing user details in database
     * */
    public void addUser(String name, String email, String phone, String session_ctom) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name); // Name
        values.put(KEY_EMAIL, email); // Email
        values.put(KEY_PHONE, phone); // Phone
        values.put(KEY_SESSION_CTOM, session_ctom);

        // Inserting Row
        long id = db.insert(TABLE_USER, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New user inserted into sqlite: " + id);
    }

    public void addProduct(String name, String price) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME_PRODUCT, name); // Name
        values.put(key_price, price); // Price

        // Inserting Row
        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New product inserted into sqlite: " + id);
    }

    public void addCard(String card_type, String card_number, String card_exp_date, String card_CVV) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CARD_TYPE, card_type);
        values.put(KEY_CARD_NUMBER, card_number);
        values.put(KEY_CARD_EXP_DATE, card_exp_date);
        values.put(KEY_CARD_CVV, card_CVV);


        // Inserting Row
        long id = db.insert(TABLE_CARD, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New product inserted into sqlite: " + id);
    }

    /**
     * Getting user data from database
     * */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT  * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            user.put("name", cursor.getString(1));
//            user.put("email", cursor.getString(2));
//            user.put("uid", cursor.getString(3));
//            user.put("created_at", cursor.getString(4));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());

        return user;
    }
    public String getSessionCtoM(){
 /*       String selectQuery = "SELECT " + KEY_SESSION_CTOM + " FROM " + TABLE_USER;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        String session_ctom = null;
        if (cursor.getCount() > 0) {
            session_ctom = cursor.getString(3);
        }*/

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c =db.rawQuery("SELECT * FROM USER", null);
        c.moveToFirst();
        String session_ctom = c.getString(c.getColumnIndex("SESSION_CTOM"));
        c.close();
        db.close();
        return session_ctom;
    }

    public String getPhone(){
       /* String selectQuery = "SELECT " + KEY_PHONE + " FROM " + TABLE_USER;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        String phone = null;
        if (cursor.getCount() > 0) {
            phone = cursor.getString(3);
        }*/
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c =db.rawQuery("SELECT * FROM USER", null);
        c.moveToFirst();
        String phone = c.getString(c.getColumnIndex("phone"));
        c.close();
        db.close();
        return phone;
    }

    public HashMap<String, String> getProductDetails() {
        HashMap<String, String> product = new HashMap<String, String>();
        String selectQuery = "SELECT  * FROM " + TABLE_PRODUCTS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            product.put("name", cursor.getString(1));
            product.put("price", cursor.getString(2));
//            product.put("uid", cursor.getString(3));
//            product.put("created_at", cursor.getString(4));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching product from Sqlite: " + product.toString());

        return product;
    }

    /**
     * Re crate database Delete all tables and create them again
     * */
    public void deleteUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_USER, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }
    public void deleteProducts() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_PRODUCTS, null, null);
        db.close();

        Log.d(TAG, "Deleted all products info from sqlite");
    }
    public void deleteCard() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_CARD, null, null);
        db.close();

        Log.d(TAG, "Deleted all products info from sqlite");
    }

}

