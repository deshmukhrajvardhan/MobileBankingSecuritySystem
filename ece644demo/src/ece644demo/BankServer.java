package ece644demo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class BankServer {
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
	
	//function to get private key
	private static PrivateKey readPrivateKey(String fileName) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		InputStream in = new FileInputStream(fileName);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
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
	
	public static void main(String[] args) throws ClassNotFoundException, UnknownHostException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, KeyStoreException, CertificateException
	{
		int port_client = 9996;
		ServerSocket bsocket = new ServerSocket(port_client);
		Socket ms = null;
		
		String file_bank_public_key="serverBankPublicKey";
		String file_bank_private_key="serverBankPrivateKey";
		String file_merchant_public_key="serverMerchantPublicKey";
		
		PublicKey bank_public_key=readPublicKey(file_bank_public_key);
		PrivateKey bank_private_key=readPrivateKey(file_bank_private_key);
		PublicKey merchant_public_key=readPublicKey(file_merchant_public_key);
		PublicKey client_public_key = new GetClientPublicKeyFromCert().getClientPublicKey();
		
		while(true) {
			try{
				ms = bsocket.accept();
				}catch(IOException e){
					e.printStackTrace();
			}
			new BankServerThread(ms, bank_private_key, bank_public_key, merchant_public_key, client_public_key).start();
		}
	}

}
