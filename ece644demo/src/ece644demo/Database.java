package ece644demo;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.commons.codec.binary.Hex;

public class Database {

	final static String secret = "secret";

	public Database() {
		// TODO Auto-generated constructor stub
	}
	
	//function creates the tables
	private static void onCreate(Connection c) throws SQLException{
		//Connect to database(create if it doesn't exist)
		delete_tables(c);
		Statement stmt_create_users_table = null;
		Statement stmt_create_products_table = null;
		Statement stmt_create_session_table = null;
		Statement stmt_create_OTP_table = null;
		stmt_create_users_table = c.createStatement();
		String sql_users = "CREATE TABLE USERS " +
				"(ID INTEGER PRIMARY KEY     AUTOINCREMENT," +
				" NAME           TEXT    NOT NULL, " + 
				" EMAIL            TEXT     NOT NULL, " + 
				" PASSWORD        TEXT   NOT NULL, " +
				" SALT        TEXT   NOT NULL, " +
				" PHONE        TEXT   NOT NULL)"; 
		stmt_create_users_table.executeUpdate(sql_users);
		stmt_create_users_table.close();

		stmt_create_products_table = c.createStatement();
		String sql_products = "CREATE TABLE PRODUCTS " +
				"(ID INTEGER PRIMARY KEY    AUTOINCREMENT," +
				" NAME           TEXT    NOT NULL, " + 
				" PRICE            INT     NOT NULL)"; 
		stmt_create_products_table.executeUpdate(sql_products);
		stmt_create_products_table.close();

		stmt_create_session_table = c.createStatement();
		String sql_session = "CREATE TABLE SESSION " +
				"(ID INTEGER PRIMARY KEY    AUTOINCREMENT," +
				" EMAIL           TEXT    NOT NULL, " +
				" SESSION_CTOM            TEXT, " +
				" SESSION_MTOB             TEXT)";
		stmt_create_session_table.executeUpdate(sql_session);
		stmt_create_session_table.close();

		stmt_create_OTP_table = c.createStatement();
		String sql_OTP = "CREATE TABLE OTP " +
				"(ID INTEGER PRIMARY KEY    AUTOINCREMENT," +
				"SESSION_MTOB     TEXT    NOT NULL,"  +
				" PHONE           TEXT    NOT NULL, " +
				" OTP            TEXT     NOT NULL)";
		stmt_create_OTP_table.executeUpdate(sql_OTP);
		stmt_create_OTP_table.close();
		//System.out.println("Opened database successfully");
	}

	//USER FUNCTIONS
	//inserts username, email, salt, password and phone number in users table
	public static int insert_users(Connection c, String username, String email_id, String password, String phone_number) throws SQLException, NoSuchAlgorithmException, InvalidKeyException{
		int flag = check_user(username, email_id, phone_number, c);
		System.out.println("Check user(0-good, 1-bad): " + flag);
		if (flag == 1){
			return flag;
		}
		else
		{
			String salt = getSaltString();
			Mac sha1_HMAC = Mac.getInstance("HmacSHA1");
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
			sha1_HMAC.init(secretKey);
			String concat = username+password+salt;
			byte[] hash = sha1_HMAC.doFinal(concat.getBytes());
			String newPassword = Hex.encodeHexString(hash);
			PreparedStatement statement = c.prepareStatement("INSERT INTO USERS (NAME,EMAIL,SALT,PASSWORD,PHONE) " +
					"VALUES (?, ?, ?, ?, ?)"); 
			statement.setString(1, username);
			statement.setString(2, email_id);
			statement.setString(3, salt);
			statement.setString(4, newPassword);
			statement.setString(5, phone_number);
			statement.executeUpdate();
			statement.close();
			c.commit();
			System.out.println("phone:"+phone_number);
			System.out.println("email_id:"+email_id);
			//System.out.println("newPassword:"+newPassword);
			System.out.println("Records created in Users table successfully");
		}
		return flag;
	}
	
	//function retrieves salt from users table
	protected static String getSaltString() { 
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 10) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}
	
	//function checks if username or email or phone number is already present in database
	public static int check_user(String username, String email_id, String phone_number, Connection c){
		int flag = 0;
		try{
			PreparedStatement statement_username = c.prepareStatement("select * from USERS where name = ?");    
			statement_username.setString(1, username);
			PreparedStatement statement_email = c.prepareStatement("select * from USERS where email = ?");
			statement_email.setString(1, email_id);
			PreparedStatement statement_phone = c.prepareStatement("select * from USERS where phone = ?");
			statement_phone.setString(1, phone_number);
			ResultSet resultSet_username = statement_username.executeQuery();
			ResultSet resultSet_email = statement_email.executeQuery();
			ResultSet resultSet_phone = statement_phone.executeQuery();

			if(resultSet_username.next()){
				String user_database = resultSet_username.getString("name");
				if(user_database.equals(username))
					flag = 1;
				else
					flag = 0;
			}
			if(resultSet_email.next()){
				String email_database = resultSet_email.getString("email");
				if(email_database.equals(email_id))
					flag = 1;
				else
					flag = 0;
			}
			if(resultSet_phone.next()){
				String phone_database = resultSet_phone.getString("phone");
				if(phone_database.equals(phone_number))
					flag = 1;
				else
					flag = 0;
			}
			resultSet_username.close();
			resultSet_email.close();
			resultSet_phone.close();
			statement_username.close();
			statement_email.close();
			statement_phone.close();
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		return flag;
	}
	
	//function is called while user login
	public static int login_activity(String username, String password, String email_id, Connection c) throws SQLException, NoSuchAlgorithmException, InvalidKeyException{
		int flag = 1;
		try{
			String salt_db = "SALT";
			PreparedStatement statement = c.prepareStatement("select * from USERS where name = ? and email = ?");    
			statement.setString(1, username);
			statement.setString(2, email_id);
			ResultSet resultSet = statement.executeQuery();
			if(!resultSet.next()){
				flag = 0;
				return flag;
			}
			String salt = resultSet.getString(salt_db);
			String password_table = resultSet.getString("password");

			Mac sha1_HMAC = Mac.getInstance("HmacSHA1");
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
			sha1_HMAC.init(secretKey);
			String concat = username+password+salt;
			byte[] hash = sha1_HMAC.doFinal(concat.getBytes());
			String newPassword = Hex.encodeHexString(hash);

			if(newPassword .equals(password_table))
				flag = 1;
			else
				flag = 0;
			//resultSet.close();
			statement.close();
			System.out.println("salt:"+salt);
			System.out.println("concat:"+concat);
			System.out.println("newPassword:"+newPassword);
			System.out.println("password_table:"+password_table);
		}catch(SQLException e){
			e.printStackTrace();
			flag = 0;
		}
		return flag;
	}
	
	//function is to print the users table
	public static String print_users(Connection c) throws SQLException{
		Statement stmt_print_users = null;
		String email = null;
		stmt_print_users = c.createStatement();
		ResultSet rs = stmt_print_users.executeQuery( "SELECT * FROM USERS" );
		while ( rs.next() ) {
			int id = rs.getInt("ID");
			String  name = rs.getString("NAME");
			email  = rs.getString("EMAIL");
			String  password = rs.getString("PASSWORD");
			String  salt = rs.getString("SALT");
			String  phoneNumber = rs.getString("PHONE");
			System.out.println( "ID = " + id );
			System.out.println( "NAME = " + name );
			System.out.println( "EMAIL = " + email );
			System.out.println( "PASSWORD = " + password );
			System.out.println( "SALT = " + salt );
			System.out.println( "PHONE = " + phoneNumber );
		}
		rs.close();
		stmt_print_users.close();
		return email;
	}
	
	//function called to get the phone number from users table using email
	public static String get_phone_users(Connection c , String email) throws SQLException{
		PreparedStatement statement_phone = c.prepareStatement("SELECT * FROM USERS WHERE EMAIL = ?");
		statement_phone.setString(1, email);
		ResultSet resultSet_phone = statement_phone.executeQuery();
		String phone = null;
		while(resultSet_phone.next()){
			phone = resultSet_phone.getString("PHONE");
		}
		System.out.println("phone from query: "+ phone);
		statement_phone.close();

		return phone;
	}


	//SESSION FUNCTIONS

	//function called to insert sessin_ctom when user logs in
	public static void insert_session(Connection c, String email_id, String session_ctom) throws SQLException, NoSuchAlgorithmException{
		PreparedStatement statement = c.prepareStatement("INSERT INTO SESSION (EMAIL, SESSION_CTOM) " +
				"VALUES (?, ?)"); 
		statement.setString(1, email_id);
		statement.setString(2, session_ctom);
		statement .executeUpdate();
		c.commit();
		statement.close();

		System.out.println("Records created in Session table successfully");
	}

	//function called to insert session_mtob
	public static void update_session_pay(Connection c, String session_ctom, String session_mtob) throws SQLException{
		String SESSION_MTOB = "SESSION_MTOB";
		PreparedStatement statement_pay = c.prepareStatement("UPDATE SESSION SET " + SESSION_MTOB + " = ? " + "WHERE SESSION_CTOM = ?");
		statement_pay.setString(1, session_mtob);
		statement_pay.setString(2, session_ctom);
		statement_pay .executeUpdate();
		c.commit();
		statement_pay.close();
	}
	
	//function to get phone number from sessions table
	public static String get_phone(Connection c, String session_mtob) throws SQLException{
		PreparedStatement statement_email = c.prepareStatement("SELECT * FROM SESSION WHERE SESSION_MTOB = ?");
		statement_email.setString(1, session_mtob);
		ResultSet resultSet_email = statement_email.executeQuery();
		String session_database_email = null;
		while(resultSet_email.next()){
			session_database_email = resultSet_email.getString("EMAIL");
		}
		System.out.println("Email from query: "+ session_database_email);
		statement_email.close();

		PreparedStatement statement_phone = c.prepareStatement("SELECT * FROM USERS WHERE EMAIL = ?");
		statement_phone.setString(1, session_database_email);
		ResultSet resultSet_phone = statement_phone.executeQuery();
		String phone = null;
		while(resultSet_phone.next()){
			phone = resultSet_phone.getString("PHONE");
		}
		System.out.println("phone from query: "+ phone);
		statement_phone.close();

		return phone;
	}
	
	//function to get session_mtob from OTP table using phone number
	public static String get_session_mtob(Connection c, String phone_number) throws SQLException{
		PreparedStatement statement = c.prepareStatement("SELECT * FROM OTP WHERE PHONE = ?");
		statement.setString(1, phone_number);
		ResultSet resultSet = statement.executeQuery();
		String session_mtob = null;
		while(resultSet.next()){		
			session_mtob = resultSet.getString("SESSION_MTOB");
		}
		statement.close();
		return session_mtob;
	}
	
	//function to get email from session tables using session_mtob
	public static String get_mail(Connection c, String session_mtob) throws SQLException{
		PreparedStatement statement = c.prepareStatement("SELECT * FROM SESSION WHERE SESSION_MTOB = ?");
		statement.setString(1, session_mtob);
		ResultSet resultSet = statement.executeQuery();
		String email = null;
		while(resultSet.next()){		
			email = resultSet.getString("EMAIL");
		}
		statement.close();
		return email;
	}
	
	

	//OTP functions
	//function to insert session_mtob, phone number and OTP in OTP table
	public static void insert_OTP(Connection c, String session_mtob, String phone_number, String OTP) throws SQLException, NoSuchAlgorithmException{
		PreparedStatement statement = c.prepareStatement("INSERT INTO OTP (SESSION_MTOB, PHONE, OTP) " +
				"VALUES (?, ?, ?)"); 
		statement.setString(1, session_mtob);
		statement.setString(2, phone_number);
		statement.setString(3, OTP);
		statement .executeUpdate();
		statement.close();
		print_OTP(c);
		c.commit();
		//c.close();

		System.out.println("Records created in OTP table successfully");
	}
	
	
	//function to get OTP from OTP table using phone_number
	public static String get_OTP(Connection c, String phone) throws SQLException{
		PreparedStatement statement = c.prepareStatement("SELECT * FROM OTP WHERE PHONE = ?");
		statement.setString(1, phone);
		ResultSet resultSet = statement.executeQuery();
		String database_OTP = null;
		while(resultSet.next()){
			database_OTP = resultSet.getString("OTP");
		}
		statement.close();
		return database_OTP;
	}
	
	//function to print the details of OTP table
	public static void print_OTP(Connection c) throws SQLException{
		Statement stmt_print_users = null;
		stmt_print_users = c.createStatement();
		ResultSet rs = stmt_print_users.executeQuery( "SELECT * FROM OTP" );
		while ( rs.next() ) {
			int id = rs.getInt("ID");
			String  OTP = rs.getString("OTP");
			String  SESSION_MTOB = rs.getString("SESSION_MTOB");
			String  phone = rs.getString("PHONE");
			System.out.println( "ID = " + id );
			System.out.println( "OTP = " + OTP );
			System.out.println( "SESSION_MTOB = " + SESSION_MTOB );
			System.out.println( "phone = " + phone );
		}
		rs.close();
		stmt_print_users.close();
	}
	
	//function to get phone number from OTP table
	public static String get_phone_number(Connection c) throws SQLException{
		PreparedStatement statement = c.prepareStatement("SELECT * FROM OTP ");
		ResultSet resultSet = statement.executeQuery();
		String database_phone_number = null;
		while(resultSet.next()){
			database_phone_number = resultSet.getString("PHONE");
		}
		statement.close();
		return database_phone_number;
	}

	//function to delete OTP from OTP table
	public static void delete_OTP(Connection c, String OTP) throws SQLException{
		PreparedStatement statement = c.prepareStatement("DELETE * FROM OTP WHERE OTP = ?");
		statement.setString(1, OTP);
		statement.executeUpdate();
		statement.close();
	}

	//PRODUCT FUNCTIONS
	//function to insert products in products table
	private static void insert_products(Connection c) throws SQLException{
		Statement stmt_insert_products = null;
		stmt_insert_products = c.createStatement();
		String sql_product1 = "INSERT INTO PRODUCTS (NAME,PRICE) " +
				"VALUES ('Phone1', '500');"; 
		stmt_insert_products.executeUpdate(sql_product1);

		String sql_product2 = "INSERT INTO PRODUCTS (NAME,PRICE) " +
				"VALUES ('Phone2', '700');"; 
		stmt_insert_products.executeUpdate(sql_product2);

		String sql_product3 = "INSERT INTO PRODUCTS (NAME,PRICE) " +
				"VALUES ('Phone3', '900');"; 
		stmt_insert_products.executeUpdate(sql_product3);

		stmt_insert_products.close();
		System.out.println("Records created in Products table successfully");
	}

	//function to create a hashmap and return it to merchant
	public static LinkedHashMap<String, String> get_products(Connection c) throws SQLException{
		Statement stmt_print_products = null;
		stmt_print_products = c.createStatement();
		LinkedHashMap<String, String> products = new LinkedHashMap<>();
		ResultSet rs = stmt_print_products.executeQuery( "SELECT * FROM PRODUCTS;" );
		while ( rs.next() ) {
			String  name = rs.getString("NAME");
			String price  = rs.getString("PRICE");
			products.put(name, price);
			//products.put("price", price);
			//System.out.println( "ID = " + id );
			//System.out.println( "NAME = " + name );
			//System.out.println( "PRICE = " + price );
		}
		rs.close();
		stmt_print_products.close();
		return products;
	}

	//function to delete all the tables
	private static void delete_tables(Connection c) throws SQLException{
		Statement stmt_delete = null;
		stmt_delete = c.createStatement();
		String sql_users = "DROP TABLE USERS;";
		String sql_products = "DROP TABLE PRODUCTS;";
		String sql_session = "DROP TABLE SESSION;";
		String sql_OTP = "DROP TABLE OTP;";
		stmt_delete.executeUpdate(sql_users);
		stmt_delete.executeUpdate(sql_products);
		stmt_delete.executeUpdate(sql_session);
		stmt_delete.executeUpdate(sql_OTP);
		stmt_delete.close();
	}


	public static void main( String args[] )
	{
		Connection c = null;

		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
			c.setAutoCommit(false);
			onCreate(c);
			//delete_tables(c);
			//insert_users(c);
			insert_products(c);
			//print_users(c);
			//get_products(c);
			/*HashMap<String, String> products;
			products = get_products(c);
			System.out.println(products);
			int flag = login_activity("akash","mantry","akashmantry@gmail.com", c);
			if (flag == 1)
				System.out.println("Value present in database");
			else
				System.out.println("Value not in database");*/
			c.commit();
			c.close();
		}catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}


	}

}
