package ece644demo;

import java.io.BufferedInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class KeyGenerator {

	private static void saveToFile(String fileName,
			BigInteger mod, BigInteger exp) throws FileNotFoundException, IOException 
	{
		ObjectOutputStream oout = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(fileName)));
		try {
			oout.writeObject(mod);
			oout.writeObject(exp);
		} finally {
			oout.close();
		}
	}
	private static PublicKey readPublicKey(String fileName) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		InputStream in = new FileInputStream(fileName);
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
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, IOException, ClassNotFoundException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		//Generate RSA public-Private KEYS
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512);
		KeyPair serverMerchantKey = keyGen.genKeyPair();
		KeyPair serverBankKey = keyGen.genKeyPair();
		KeyPair serverClientKey = keyGen.genKeyPair();
		KeyPair serverCertificateAuthority = keyGen.genKeyPair();

		//Starts before we Store keys to store keys
		KeyFactory fact = KeyFactory.getInstance("RSA");

		//#store merchant's public key
		RSAPublicKeySpec merchantPub = fact.getKeySpec(serverMerchantKey.getPublic(),RSAPublicKeySpec.class);
		saveToFile("serverMerchantPublicKey", merchantPub.getModulus(), merchantPub.getPublicExponent());

		//Store merchant's private key
		RSAPrivateKeySpec merchantPriv = fact.getKeySpec(serverMerchantKey.getPrivate(),  RSAPrivateKeySpec.class);
		saveToFile("serverMerchantPrivateKey", merchantPriv.getModulus(), merchantPriv.getPrivateExponent());

		//store bank's public key
		RSAPublicKeySpec bankPub = fact.getKeySpec(serverBankKey.getPublic(),RSAPublicKeySpec.class);
		saveToFile("serverBankPublicKey", bankPub.getModulus(), bankPub.getPublicExponent());

		//Store bank's private key
		RSAPrivateKeySpec bankPriv = fact.getKeySpec(serverBankKey.getPrivate(),  RSAPrivateKeySpec.class);
		saveToFile("serverBankPrivateKey", bankPriv.getModulus(), bankPriv.getPrivateExponent());

		//store CA's public key
		RSAPublicKeySpec CAPub = fact.getKeySpec(serverCertificateAuthority.getPublic(),RSAPublicKeySpec.class);
		saveToFile("serverCAPublicKey", CAPub.getModulus(), CAPub.getPublicExponent());

		//Store CA's private key
		RSAPrivateKeySpec CAPriv = fact.getKeySpec(serverCertificateAuthority.getPrivate(),  RSAPrivateKeySpec.class);
		saveToFile("serverCAPrivateKey", CAPriv.getModulus(), CAPriv.getPrivateExponent());

		//store client's public key
		RSAPublicKeySpec clientPub = fact.getKeySpec(serverClientKey.getPublic(),RSAPublicKeySpec.class);
		saveToFile("serverClientPublicKey", clientPub.getModulus(), clientPub.getPublicExponent());

		//Store client's private key
		RSAPrivateKeySpec clientPriv = fact.getKeySpec(serverClientKey.getPrivate(),  RSAPrivateKeySpec.class);
		saveToFile("serverClientPrivateKey", clientPriv.getModulus(), clientPriv.getPrivateExponent());

		//Testing part

		/*		System.out.println(readPublicKey("serverMerchantPublicKey"));
		PublicKey ca_pu_cl=readPublicKey("serverMerchantPublicKey");
		PrivateKey ca_pr_ca=readPrivateKey("serverMerchantPrivateKey");
		Cipher cipher = Cipher.getInstance("RSA");//
		cipher.init(Cipher.ENCRYPT_MODE, ca_pu_cl);//kp.getPublic());//
		String plainText = "I have a secret";//
		byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));//

		System.out.println(cipherText);
		cipher.init(Cipher.DECRYPT_MODE, ca_pr_ca);
		byte[] decrypted=cipher.doFinal(cipherText);
		System.out.println("decrypted: " + new String(decrypted, "UTF-8"));*/
	}

}
