package ece644demo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class test_hash_encrypt_send {

		public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnknownHostException, IOException, ClassNotFoundException{
			try{
				//Communicate with payment gateway
				//v is sent to payment gateway to tell that we want the public key
				//public key is then received
				int bank_port = 9996;
				Socket bsocket = new Socket(InetAddress.getLocalHost(), bank_port);				
				DataOutputStream dos2 = new DataOutputStream(bsocket.getOutputStream());
				int v = 1;
				dos2.writeInt(v);
				dos2.flush();
				ObjectInputStream b_ois = new ObjectInputStream(bsocket.getInputStream());
				PublicKey serverBankPublicKey = (PublicKey) b_ois.readObject();
				
				//Communicate with merchant
				//v is sent to merchant to tell that we want the public key
				//public key is then received
				int merchant_port = 9995;
				Socket msocket = new Socket(InetAddress.getLocalHost(), merchant_port);				
				DataOutputStream dos = new DataOutputStream(msocket.getOutputStream());
				dos.writeInt(v);
				dos.flush();
				ObjectInputStream m_ois = new ObjectInputStream(msocket.getInputStream());
				PublicKey serverMerchantPublicKey = (PublicKey) m_ois.readObject();
				msocket.close();
				
				Cipher cipherMerchant = Cipher.getInstance("RSA");
				cipherMerchant.init(Cipher.ENCRYPT_MODE, serverMerchantPublicKey);
		    	Cipher cipherBank = Cipher.getInstance("RSA");
		    	cipherBank.init(Cipher.ENCRYPT_MODE, serverBankPublicKey);
		    
		    	String plainTextBank = "I am bank";
		    	//String plainTextBank = "123";
		    	String plainTextMerchant = "I am merchant";
		    	
		    	System.out.println("Message sent to bank: "+plainTextBank);
		    	System.out.println("Message sent to merchant: "+plainTextMerchant);
		    	
		    	//Computing encrypted info(length of each = 64)
		    	byte[] cipherTextBank = cipherBank.doFinal(plainTextBank.getBytes());
		    	byte[] cipherTextMerchant = cipherMerchant.doFinal(plainTextMerchant.getBytes());
		    	
		    	//Computing individual hashes
		    	MessageDigest mdBank = MessageDigest.getInstance("MD5");
		    	MessageDigest mdMerchant = MessageDigest.getInstance("MD5");
		    	mdBank.update(plainTextBank.getBytes());
		    	mdMerchant.update(plainTextMerchant.getBytes());	
		    	byte[] messageDigestBankMD5 = mdBank.digest();
		    	byte[] messageDigestMerchantMD5 = mdMerchant.digest();
		    	
		    	//Computing dual hash(hash length = 16)
		    	byte[] messageDigestConcatenate = new byte[messageDigestBankMD5.length + messageDigestMerchantMD5.length];
		    	System.arraycopy(messageDigestBankMD5, 0, messageDigestConcatenate, 0, messageDigestBankMD5.length);
		    	System.arraycopy(messageDigestMerchantMD5, 0, messageDigestConcatenate, messageDigestBankMD5.length, messageDigestMerchantMD5.length);
		    	MessageDigest mdConcatenate = MessageDigest.getInstance("MD5");
		    	mdConcatenate.update(messageDigestConcatenate);
		    	byte[] combinedHash = mdConcatenate.digest();
		    	
		    	//Creating the message to be sent to merchant and bank
		    	int byteArrayLength = cipherTextBank.length + cipherTextMerchant.length +
					messageDigestBankMD5.length + messageDigestMerchantMD5.length +
					combinedHash.length;
		    	byte[] bigByteArray = new byte[byteArrayLength];
		    	System.arraycopy(cipherTextBank, 0, bigByteArray, 0, cipherTextBank.length);
		    	System.arraycopy(cipherTextMerchant, 0, bigByteArray, cipherTextBank.length, cipherTextMerchant.length);
		    	System.arraycopy(messageDigestBankMD5, 0, bigByteArray, cipherTextBank.length+cipherTextMerchant.length, messageDigestBankMD5.length);
		    	System.arraycopy(messageDigestMerchantMD5, 0, bigByteArray, cipherTextBank.length+cipherTextMerchant.length+messageDigestBankMD5.length, messageDigestMerchantMD5.length);
		    	System.arraycopy(combinedHash, 0, bigByteArray, cipherTextBank.length+cipherTextMerchant.length+messageDigestBankMD5.length+messageDigestMerchantMD5.length, combinedHash.length);
		    	
		    	System.out.println("bigByteArray.length"+bigByteArray.length);
		    	System.out.println("bigByteArray"+bigByteArray);
		    	//Sending message to merchant
		    	//s tell the merchant that we are sending the message
				msocket = new Socket(InetAddress.getLocalHost(), merchant_port);
		    	dos = new DataOutputStream(msocket.getOutputStream());
		    	int s=2;
		    	dos.writeInt(s);
		    	dos.writeInt(bigByteArray.length);
		    	dos.write(bigByteArray);
		    	dos.flush();
				
				
		    	//Closing sockets
		    	msocket.close();
		    	bsocket.close();
			}catch(IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
			{
				e.getStackTrace();
			}
		}
	}
