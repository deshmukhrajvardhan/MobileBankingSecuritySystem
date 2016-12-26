package ece644demo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class BankServerThread extends Thread{
	protected Socket socket;
	PrivateKey bank_private_key;
	PublicKey bank_public_key;
	PublicKey merchant_public_key;
	PublicKey client_public_key;
	//int input = 0;
	String input= null;
	DataInputStream dis = null;
	DataOutputStream dos = null;
	private int flag_integrity = 0;
	private int flag_combined_hash = 0;
	private String flag_OTP_correct = null;
	String OTP_sent;
	String plainText;
	int flag_mtob = 0;
	String phone_database = null;
	Connection c = null;
	
	//receive keys
	//merchant - public
	//bank - public and private
	BankServerThread(Socket bsocket, PrivateKey serverBankPrivateKey, PublicKey serverBankPublicKey, PublicKey serverMerchantPublicKey, PublicKey clientPublicKey) {
		this.socket = bsocket;
		this.bank_private_key = serverBankPrivateKey;
		this.bank_public_key = serverBankPublicKey;
		this.merchant_public_key = serverMerchantPublicKey;
		this.client_public_key = clientPublicKey;
	}

	public void run(){		
		try{
			//receive identifier and decrypt it
			dis = new DataInputStream(socket.getInputStream());
			byte[] cipherText = null;
			cipherText = new byte[64];
			dis.readFully(cipherText, 0, cipherText.length); // read the message
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, bank_private_key);
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

		//merchant is sending the message
		if(input.equals("Message")){
			System.out.println("Inside Bank's Message function");
			try{
				int length = 352;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length); // read the message
				}
				
				//received time stamp, session_mtob and card details
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, bank_private_key);
				byte[] slice_TimeStampClient = Arrays.copyOfRange(cipherText, 224, 288);
				String timeStamp_client_string = new String(cipher.doFinal(slice_TimeStampClient));
				long timeStamp_merchant = Long.parseLong(timeStamp_client_string);
				byte[] slice_session_mtob = Arrays.copyOfRange(cipherText, 288, 352);
				String session_mtob = new String(cipher.doFinal(slice_session_mtob));

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long tsTime_bank = ts1.getTime();

				System.out.println("-Session_mtob: "+session_mtob);
				System.out.println("-Time from merchant: "+timeStamp_merchant);
				System.out.println("-Time of server: "+tsTime_bank);
				
				//check time
				if((tsTime_bank - timeStamp_merchant) < 30000){
					System.out.println("-Time condition satisfied.");
					new Database();
					Class.forName("org.sqlite.JDBC");
					c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
					c.setAutoCommit(false);
					flag_mtob = 1;

					phone_database = Database.get_phone(c, session_mtob);
					System.out.println("-Phone number from database: "+ phone_database);
				}else
					flag_mtob = 0;

				if(flag_mtob == 0){
					System.out.println("-Time condition not satisfied");
					new  SendSMS();
					SendSMS.sendFailureMessage(phone_database);
					System.out.println("-Failure message sent");
				}
				else{
					cipher.init(Cipher.DECRYPT_MODE, bank_private_key);
					byte[] slice = Arrays.copyOfRange(cipherText, 0, 64);
					plainText = new String(cipher.doFinal(slice));

					//Computing the hash
					MessageDigest mdBank = MessageDigest.getInstance("MD5");
					mdBank.update(plainText.getBytes());	
					byte[] messageDigestBankMD5 = mdBank.digest();

					//Retrieving the hashes
					byte[] slice_merchant_hash = Arrays.copyOfRange(cipherText, 144, 160);
					byte[] slice_bank_hash = Arrays.copyOfRange(cipherText, 128, 144);
					byte[] slice_merchant_concat = Arrays.copyOfRange(cipherText, 160, 224);

					byte[] messageDigestConcatenate = new byte[messageDigestBankMD5.length + slice_merchant_hash.length];
					System.arraycopy(messageDigestBankMD5, 0, messageDigestConcatenate, 0, messageDigestBankMD5.length);
					System.arraycopy(slice_merchant_hash, 0, messageDigestConcatenate, messageDigestBankMD5.length, slice_merchant_hash.length);

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
					if(Arrays.equals(messageDigestBankMD5, slice_bank_hash)) {
						System.out.println("-Integrity checked");
						flag_integrity = 1;
					}

					//Send OTP to client
					if(flag_integrity == 1 && flag_combined_hash == 1 && plainText.length() > 0){
						new  SendSMS();
						String OTP = SendSMS.generateOTP(phone_database);
						Database.insert_OTP(c, session_mtob, phone_database, OTP);
						System.out.println("-OTP message sent");
					}
					else{
						//send failure message to client
						new  SendSMS();
						SendSMS.sendFailureMessage(phone_database);
						System.out.println("-Failure message sent");
					}				
				}
			}catch(IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | ClassNotFoundException | SQLException e){
				e.printStackTrace();
				return;
			}
		}

		//called when user sends OTP
		if(input.equals("OTP")){
			System.out.println("Inside Bank's OTP function");
			try {
				new Database();
				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
				c.setAutoCommit(false);

				//Database.delete_OTP(c, OTP_sent);
				dis = new DataInputStream(socket.getInputStream());

				int length = 192;
				byte[] cipherText = null;
				if(length>0) {
					cipherText = new byte[length];
					dis.readFully(cipherText, 0, cipherText.length);
				}
				
				//receive time stamp, OTP and phone number
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, bank_private_key);
				byte[] slice_TimeStampClient = Arrays.copyOfRange(cipherText, 0, 64);
				String timeStamp_client_string = new String(cipher.doFinal(slice_TimeStampClient));
				long timeStamp_client = Long.parseLong(timeStamp_client_string);
				byte[] slice_OTP = Arrays.copyOfRange(cipherText, 64, 128);
				String userOTP = new String(cipher.doFinal(slice_OTP));
				byte[] slice_phone = Arrays.copyOfRange(cipherText, 128, 192);
				String phone = new String(cipher.doFinal(slice_phone));

				java.util.Date today = new java.util.Date();
				java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
				long tsTime_bank = ts1.getTime();

				System.out.println("-OTP received: "+ userOTP);
				System.out.println("-Phone: "+ phone);
				System.out.println("-Time from client: "+timeStamp_client);
				System.out.println("-Time of server: "+tsTime_bank);

				String OTP_sent = Database.get_OTP(c, phone);
				System.out.println("-OTP sent:"+OTP_sent);
				
				//check time
				if((tsTime_bank - timeStamp_client) < 30000){
					if(userOTP.equals(OTP_sent)){
						System.out.println("-The OTP sent was correct.");
						flag_OTP_correct = "Verified";
					}
					else{
						flag_OTP_correct = "Not verified";
						System.out.println("-The OTP sent was incorrect.");
					}
				}
				else{
					flag_OTP_correct = "Not verified";
					System.out.println("-Time condition was not satisfied");
				}

				try{
					String session_mtob = Database.get_session_mtob(c, phone);;
					int merchant_port = 9995;
					Socket msocket = new Socket(InetAddress.getLocalHost(), merchant_port);
					DataOutputStream dos = new DataOutputStream(msocket.getOutputStream());

					java.util.Date today_bank = new java.util.Date();
					java.sql.Timestamp ts2 = new java.sql.Timestamp(today_bank.getTime());
					long timeStamp_bank = ts2.getTime();
					
					//send reply to merchant - time stamp, session_mtob, message
					String identifier = "Reply";
					byte[] cipherTextIdentifier = encrypt_merchant(identifier);
					byte[] cipherTextTimeStamp = encrypt_merchant(Long.toString(timeStamp_bank));
					byte[] cipherTextSession_mtob = encrypt_merchant(session_mtob);
					byte[] cipherTextMessage = encrypt_merchant(flag_OTP_correct);

					int byteArrayLength = 192;
					byte[] bigByteArray = new byte[byteArrayLength];
					System.arraycopy(cipherTextTimeStamp, 0, bigByteArray, 0, 64);
					System.arraycopy(cipherTextSession_mtob, 0, bigByteArray, 64, cipherTextTimeStamp.length);
					System.arraycopy(cipherTextMessage, 0, bigByteArray, 128, cipherTextMessage.length);

					dos.write(cipherTextIdentifier);
					dos.write(bigByteArray);
					dos.flush();
					msocket.close();
					System.out.println("-Sent message to merchant server!");
				}catch(IOException e){
					e.printStackTrace();
					return;
				}	    		

				//If everything is correct send notification to merchant
			} catch (IOException | SQLException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchPaddingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalBlockSizeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (BadPaddingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		try {
			socket.close();
			c.close();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	//function encrypts using merchant's public key
	public byte[] encrypt_merchant(String plainText){
		byte[] cipherTextMerchant = null;
		try {
			Cipher cipherMerchant = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipherMerchant.init(Cipher.ENCRYPT_MODE, merchant_public_key);
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
	
	//function encrypts using client's public key
			public byte[] decrypt_client(byte[] plainText){
				byte[] cipherTextBank = null;
				try {
					Cipher cipherClient = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					cipherClient.init(Cipher.DECRYPT_MODE, client_public_key);
					cipherTextBank = cipherClient.doFinal(plainText);

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

