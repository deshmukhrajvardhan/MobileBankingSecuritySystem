package ece644demo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MerchantServerThread extends Thread{
	protected Socket socket;
	PrivateKey merchant_private_key;
	PublicKey merchant_public_key;
	PublicKey bank_public_key;
	PublicKey client_public_key;
	String input = null;
	DataInputStream dis = null;
	private int flag_integrity = 0;
	private int flag_combined_hash = 0;
	int flag_login = 0;
	String flag_login_message = null;
	String flag_registration_message = null;
	String flag_products_message = null;
	String flag_payment_message = null;
	int flag_pay = 0;
	Connection c = null;

	//receive keys
	//merchant - private and public
	//bank - public
	//client - public
	MerchantServerThread(Socket msocket, PrivateKey serverMerchantPrivateKey, PublicKey serverMerchantPublicKey, PublicKey serverBankPublicKey, PublicKey clientPublicKey) {
		this.socket = msocket;
		this.merchant_private_key = serverMerchantPrivateKey;
		this.merchant_public_key = serverMerchantPublicKey;
		this.bank_public_key = serverBankPublicKey;
		this.client_public_key = clientPublicKey;
	}

	//thread run
	public void run(){	
		try{
			//receive identifier and decrypt it
			dis = new DataInputStream(socket.getInputStream());
			byte[] cipherText = null;
			cipherText = new byte[64];
			dis.readFully(cipherText, 0, cipherText.length);

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
			input = new String(cipher.doFinal(cipherText));
		}catch(IOException e){
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//client login
		if(input.equals("Login")){
			System.out.println("Inside login function");
			try{
				int length = 320;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length);
				}

				//decrypt username, password, email, time stamp and session_ctom
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
				byte[] slice_username = Arrays.copyOfRange(cipherText, 0, 64);
				String name = new String(cipher.doFinal(slice_username));
				byte[] slice_password = Arrays.copyOfRange(cipherText, 64, 128);
				String password = new String(cipher.doFinal(slice_password));
				byte[] slice_email = Arrays.copyOfRange(cipherText, 128, 192);
				String email = new String(cipher.doFinal(slice_email));
				byte[] slice_TimeStampClient = Arrays.copyOfRange(cipherText, 192, 256);
				String timeStamp_client_string = new String(cipher.doFinal(slice_TimeStampClient));
				long timeStamp_client = Long.parseLong(timeStamp_client_string);
				byte[] slice_session_ctom = Arrays.copyOfRange(cipherText, 256, 320);
				String session_ctom = new String(cipher.doFinal(slice_session_ctom));

				System.out.println("-Username: "+name);
				System.out.println("-Password: "+password);
				System.out.println("-Email: "+email);
				System.out.println("Session_ctom: "+session_ctom);

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long tsTime_login = ts1.getTime();

				String phone_users = "0";

				System.out.println("-Time from client: "+timeStamp_client);
				System.out.println("-Time of server: "+tsTime_login);
				Long difference = tsTime_login - timeStamp_client; 
				//System.out.println("-difference: "+difference);
				//System.out.println(difference.compareTo(minimum));
				//check time
				if(tsTime_login - timeStamp_client< 30000){
					System.out.println("-Time condition satisfied.");

					new Database();
					Class.forName("org.sqlite.JDBC");
					c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
					c.setAutoCommit(false);

					flag_login = Database.login_activity(name, password, email, c);


					if(flag_login == 1){
						flag_login_message = "Successful";
						Database.insert_session(c, email, session_ctom);
						phone_users = Database.get_phone_users(c, email);	
					}
					else
						flag_login_message = "Unsuccessful";
				}
				else{
					flag_login_message = "Unsuccessful";
					System.out.println("-Time condition not satisfied.");
					return;
				}


				java.util.Date today_bank = new java.util.Date();
				java.sql.Timestamp ts2 = new java.sql.Timestamp(today_bank.getTime());
				long tsTime_client = ts2.getTime();

				//send reply to client - message, time stamp, session_ctom and phone number
				byte[] cipherTextMessage = encrypt_client(flag_login_message);
				byte[] cipherTextTimeStamp = encrypt_client(Long.toString(tsTime_client));
				byte[] cipherTextSession_ctom = encrypt_client(session_ctom);
				byte[] cipherTextPhone = encrypt_client(phone_users);

				int byteArrayLength = 256;
				byte[] bigByteArray = new byte[byteArrayLength];
				System.arraycopy(cipherTextMessage, 0, bigByteArray, 0, cipherTextMessage.length);
				System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 64, cipherTextTimeStamp.length);
				System.arraycopy(cipherTextSession_ctom, 0, bigByteArray, 128, cipherTextSession_ctom.length);
				System.arraycopy(cipherTextPhone, 0, bigByteArray, 192, cipherTextPhone.length);

				DataOutputStream dos_login = new DataOutputStream(socket.getOutputStream());
				System.out.println("-Sending reply to client");
				dos_login.write(bigByteArray);
				dos_login.flush();			
				System.out.println("-Flag_login_message: "+flag_login_message);

			}catch(IOException | ClassNotFoundException | SQLException | NoSuchAlgorithmException | InvalidKeyException e){
				e.printStackTrace();

			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//sending list of products to client
		if(input.equals("Products")){
			System.out.println("Inside Products function");
			try{
				int length = 128;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length); // read the message
				}

				//received time stamp and session_ctom from client
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
				byte[] slice_TimeStampClient = Arrays.copyOfRange(cipherText, 0, 64);
				String timeStamp_client_string = new String(cipher.doFinal(slice_TimeStampClient));
				long timeStamp_client = Long.parseLong(timeStamp_client_string);
				byte[] slice_session_ctom = Arrays.copyOfRange(cipherText, 64, 128);
				String session_ctom = new String(cipher.doFinal(slice_session_ctom));

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long tsTime_login = ts1.getTime();

				System.out.println("-Session_ctom: "+session_ctom);
				System.out.println("-Time from client: "+timeStamp_client);
				System.out.println("-Time of server: "+tsTime_login);

				//check time
				if((tsTime_login - timeStamp_client) < 30000){
					System.out.println("-Time condition satisfied");
					flag_products_message = "Successful";
				}
				else{
					flag_products_message = "Unsuccessful";
				}
				
				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
				c.setAutoCommit(false);
				new Database();
				LinkedHashMap<String, String> products;
				products = Database.get_products(c);
				
				byte[] byteSession_ctom = session_ctom.getBytes();
				byte[] byteProducts = products.toString().getBytes();
				
				int byteArrayLength = byteSession_ctom.length + byteProducts.length;
				byte[] bigByteArray = new byte[byteArrayLength];
				System.arraycopy(byteSession_ctom, 0, bigByteArray, 0, byteSession_ctom.length);
				System.arraycopy(byteProducts, 0, bigByteArray, byteSession_ctom.length, byteProducts.length);
				
				MessageDigest mdClientCombined = MessageDigest.getInstance("MD5");
				mdClientCombined.update(bigByteArray);
				byte[] messageDigestCombinedMD5 = mdClientCombined.digest();
				
				//send product list and combined hash of session_ctom and products
				ObjectOutputStream dos_products = new ObjectOutputStream(socket.getOutputStream());
				dos_products.writeObject(products);
				dos_products.writeObject(messageDigestCombinedMD5);

				dos_products.flush();
				dos_products.close();
			}
			catch (IOException | ClassNotFoundException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//client is sending the message
		if(input.equals("Message")){
			System.out.println("Inside Merchant's Message function");
			try{
				int length = 352;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length); // read the message
				}

				//receive time stamp, session_ctom, product name and card name
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
				byte[] slice_TimeStampClient = Arrays.copyOfRange(cipherText, 224, 288);
				String timeStamp_client_string = new String(cipher.doFinal(slice_TimeStampClient));
				long timeStamp_client = Long.parseLong(timeStamp_client_string);
				byte[] slice_session_ctom = Arrays.copyOfRange(cipherText, 288, 352);
				String session_ctom = new String(cipher.doFinal(slice_session_ctom));

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long tsTime_login = ts1.getTime();

				System.out.println("-Session_ctom: "+session_ctom);
				System.out.println("-Time from client: "+timeStamp_client);
				System.out.println("-Time of server: "+tsTime_login);

				//check time
				if((tsTime_login - timeStamp_client) < 30000){
					System.out.println("-Time condition satisfied");
					new Database();
					Class.forName("org.sqlite.JDBC");
					c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
					c.setAutoCommit(false);

					//generate session_mtob
					String session_mtob = generateSession_mtob();
					Database.update_session_pay(c, session_ctom, session_mtob);

					//Decrypting the message from client
					byte[] slice = Arrays.copyOfRange(cipherText, 64, 128);
					String plainText = new String(cipher.doFinal(slice));

					//Computing the hash
					MessageDigest mdMerchant = MessageDigest.getInstance("MD5");
					mdMerchant.update(plainText.getBytes());	
					byte[] messageDigestMerchantMD5 = mdMerchant.digest();

					//Retrieving the hashes
					byte[] slice_merchant_hash = Arrays.copyOfRange(cipherText, 144, 160);
					byte[] slice_bank_hash = Arrays.copyOfRange(cipherText, 128, 144);
					byte[] slice_merchant_concat = Arrays.copyOfRange(cipherText, 160, 224);

					byte[] messageDigestConcatenate = new byte[slice_bank_hash.length + messageDigestMerchantMD5.length];
					System.arraycopy(slice_bank_hash, 0, messageDigestConcatenate, 0, slice_bank_hash.length);
					System.arraycopy(messageDigestMerchantMD5, 0, messageDigestConcatenate, slice_bank_hash.length, messageDigestMerchantMD5.length);

					//Computing the dual hash
					MessageDigest mdConcatenate = MessageDigest.getInstance("MD5");
					mdConcatenate.update(messageDigestConcatenate);
					byte[] combinedHash = mdConcatenate.digest();
					
					byte[] concatenatedHash = decrypt_client(slice_merchant_concat);

					//Matching the dual hash
					if(Arrays.equals(combinedHash, concatenatedHash)) {
						System.out.println("-Dual hash checked");
						flag_combined_hash = 1;
					}

					//Checking for integrity
					if(Arrays.equals(messageDigestMerchantMD5, slice_merchant_hash)) {
						System.out.println("-Integrity checked");
						flag_integrity = 1;
					}

					//If everything is correct send notification to bank
					if(flag_integrity == 1 && flag_combined_hash == 1 && plainText.length() > 0){
						System.out.println("-The message received is authentic.");
						System.out.println("-Message received: "+plainText);
						//send message to bank
						try{
							flag_payment_message = "Successful";

							int bank_port = 9996;
							Socket bsocket = new Socket(InetAddress.getLocalHost(), bank_port);
							DataOutputStream dos_mtob = new DataOutputStream(bsocket.getOutputStream());

							java.util.Date today_bank = new java.util.Date();
							java.sql.Timestamp ts2 = new java.sql.Timestamp(today_bank.getTime());
							long tsTime_bank = ts2.getTime();

							String identifier = "Message";
							byte[] cipherTextIdentifier = encrypt_bank(identifier);
							byte[] cipherTextTimeStamp = encrypt_bank(Long.toString(tsTime_bank));
							byte[] cipherTextSession_mtob = encrypt_bank(session_mtob);

							int byteArrayLength = 352;
							byte[] bigByteArray = new byte[byteArrayLength];
							byte[] slice_originalmessage = Arrays.copyOfRange(cipherText, 0, 224);
							System.arraycopy(slice_originalmessage, 0, bigByteArray, 0, 224);
							System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 224, cipherTextTimeStamp.length);
							System.arraycopy(cipherTextSession_mtob, 0, bigByteArray, 288, cipherTextSession_mtob.length);

							System.out.println("-Sending message to bank server!");
							dos_mtob.write(cipherTextIdentifier);
							dos_mtob.write(bigByteArray);
							dos_mtob.flush();	    	
							bsocket.close();   	
						}catch(IOException e){
							e.printStackTrace();
							return;
						}
					}
					else{
						flag_payment_message = "Unsuccessful";
					}
				}
				else{
					flag_payment_message = "Unsuccessful";
				}

				//Send reply to client
				try{
					DataOutputStream dos_message_reply_client = new DataOutputStream(socket.getOutputStream());
					//java.util.Date today = new java.util.Date();
					java.sql.Timestamp ts2 = new java.sql.Timestamp(today.getTime());
					long tsTime_merchant = ts2.getTime();

					byte[] cipherTextMessage = encrypt_client(flag_payment_message);
					byte[] cipherTextTimeStamp = encrypt_client(Long.toString(tsTime_merchant));
					byte[] cipherSessionctom = encrypt_client(session_ctom);


					int byteArrayLength = 192;
					byte[] bigByteArray = new byte[byteArrayLength];
					System.arraycopy(cipherTextMessage, 0, bigByteArray, 0, cipherTextMessage.length);
					System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 64, cipherTextTimeStamp.length);
					System.arraycopy(cipherSessionctom, 0, bigByteArray, 128, cipherSessionctom.length);

					System.out.println("-Sending reply to client");
					dos_message_reply_client.write(bigByteArray);
					dos_message_reply_client.flush();			
					System.out.println("-Flag_Payment_message: "+flag_payment_message);
					dos_message_reply_client.close();

				}catch(IOException e){
					e.printStackTrace();
					return;
				}

			}catch(IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e){
				e.printStackTrace();
				return;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//reply from bank
		if(input.equals("Reply")){
			System.out.println("Inside Merchant's Reply(from bank) function");
			try {
				int length = 192;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length);
				}

				//received time stamp, session_mtob and message from bank 
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
				byte[] slice_TimeStampBank = Arrays.copyOfRange(cipherText, 0, 64);
				String timeStamp_bank_string = new String(cipher.doFinal(slice_TimeStampBank));
				long timeStamp_bank = Long.parseLong(timeStamp_bank_string);
				byte[] slice_session_mtob = Arrays.copyOfRange(cipherText, 64, 128);
				String session_mtob = new String(cipher.doFinal(slice_session_mtob));
				byte[] slice_verification = Arrays.copyOfRange(cipherText, 128, 192);
				String verification_message = new String(cipher.doFinal(slice_verification));

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long timestamp_merchant = ts1.getTime();

				System.out.println("-Session_mtob: "+session_mtob);
				System.out.println("-Time from bank: "+timeStamp_bank);
				System.out.println("-Time of server: "+timestamp_merchant);
				System.out.println("-Message from bank: "+verification_message);

				new Database();
				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
				c.setAutoCommit(false);
				String email = Database.get_mail(c, session_mtob);

				//check time
				if((timestamp_merchant - timeStamp_bank) < 30000){
					System.out.println("-Time condition satisfied");


					if(verification_message.equals("Verified"))	{					
						new SendEmail().sendMailVerified(email);
						System.out.println("-Client's trasaction was successful");
					}
					else{
						new SendEmail().sendMailNotVerified(email);
						System.out.println("-Client's trasaction was unsuccessful");
					}
				}
				else{
					new SendEmail().sendMailVerified(email);
					System.out.println("-Client's trasaction was unsuccessful due to time check failure");
				}
			} catch (IOException | ClassNotFoundException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}


		//input from registration screen
		if(input.equals("Registration")){
			System.out.println("Inside Merchant's Registartion function");
			int flag_registration = 0;
			try{
				int length = 256;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length); // read the message
				}

				//received username, password, email, phone number from client
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, merchant_private_key);
				byte[] slice_username = Arrays.copyOfRange(cipherText, 0, 64);
				String name = new String(cipher.doFinal(slice_username));
				byte[] slice_password = Arrays.copyOfRange(cipherText, 64, 128);
				String password = new String(cipher.doFinal(slice_password));
				byte[] slice_email = Arrays.copyOfRange(cipherText, 128, 192);
				String email = new String(cipher.doFinal(slice_email));
				byte[] slice_phoneNumber = Arrays.copyOfRange(cipherText, 192, 256);
				String phoneNumber = new String(cipher.doFinal(slice_phoneNumber));

				System.out.println("-Username: "+name);
				System.out.println("-Password: "+password);
				System.out.println("-Email: "+email);
				System.out.println("-PhoneNumber: "+phoneNumber);

				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
				c.setAutoCommit(false);
				new Database();
				flag_registration = Database.insert_users(c, name, email, password, phoneNumber);

				if(flag_registration == 0){
					flag_registration_message = "Successful";
				}
				else
					flag_registration_message = "Unsuccessful";

				//send message and  timestamp to client
				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts2 = new java.sql.Timestamp(today.getTime());
				long tsTime_merchant = ts2.getTime();

				byte[] cipherTextMessage = encrypt_client(flag_registration_message);
				byte[] cipherTextTimeStamp = encrypt_client(Long.toString(tsTime_merchant));


				int byteArrayLength = 128;
				byte[] bigByteArray = new byte[byteArrayLength];
				System.arraycopy(cipherTextMessage, 0, bigByteArray, 0, cipherTextMessage.length);
				System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 64, cipherTextTimeStamp.length);

				DataOutputStream dos_register = new DataOutputStream(socket.getOutputStream());
				System.out.println("-Sending reply to client");
				dos_register.write(bigByteArray);
				dos_register.flush();			
				System.out.println("-Flag_Registration_message: "+flag_registration_message);

			}catch(IOException | ClassNotFoundException | SQLException | NoSuchAlgorithmException | InvalidKeyException e){
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			socket.close();
			c.close();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}


	//function generates session key for merchant and bank servers - session_mtob
	public static String generateSession_mtob() throws NoSuchAlgorithmException {

		// Create a secure random number generator
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

		// Create two secure number generators with the same seed
		int seedByteCount = 10;
		byte[] seed = sr.generateSeed(seedByteCount);
		sr.setSeed(seed);
		return String.valueOf(sr.nextInt());
	}

	//function encrypts using bank's public key
	public byte[] encrypt_bank(String plainText){
		byte[] cipherTextBank = null;
		try {
			Cipher cipherBank = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipherBank.init(Cipher.ENCRYPT_MODE, bank_public_key);
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

	//function encrypts using client's public key
	public byte[] encrypt_client(String plainText){
		byte[] cipherText = null;
		try {
			Cipher cipherClient = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipherClient.init(Cipher.ENCRYPT_MODE, client_public_key);
			cipherText = cipherClient.doFinal(plainText.getBytes());

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

		return cipherText;
	}
	
	//function decrypts using client's public key
		public byte[] decrypt_client(byte[] plainText){
			byte[] cipherText = null;
			try {
				Cipher cipherClient = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipherClient.init(Cipher.DECRYPT_MODE, client_public_key);
				cipherText = cipherClient.doFinal(plainText);

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

			return cipherText;
		}
}
