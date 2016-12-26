package ece644demo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateAuthorityServer {

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
	public static Certificate CertiGen(PublicKey pub_key,PrivateKey ca_pr_key) throws OperatorCreationException, CertificateException{
		//Certificate creation
		// subjects name - the same as we are self signed.
		String  issuer = "C=CA_AUTH, O=The Legion of CA, OU=CA Bouncy Primary Certificate";
		String  subject = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate";
		// create the certificate - version 1
		X509v1CertificateBuilder v1Bldr = new JcaX509v1CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(1),
				new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
				new X500Name(subject), pub_key);//merc.getPublic());//CA_key.getPublic());
		X509CertificateHolder certHldr = v1Bldr.build(new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(ca_pr_key));//CA_key.getPrivate()));
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr);
		return cert;
	}
	public static void CertiStore(Certificate c1,String n_cert,String f_cert) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException	{
		//#Store Certificate
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);
		keyStore.setCertificateEntry(n_cert, c1);
		keyStore.store(new FileOutputStream(f_cert), "password".toCharArray());//
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, IOException, ClassNotFoundException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, OperatorCreationException, CertificateException, KeyStoreException, NoSuchProviderException, SignatureException
	{
		//#read public keys of client, merchant and PG		
		String file_CA_public_key="serverCAPublicKey";
		String file_CA_private_key="serverCAPrivateKey";
		String file_merchant_public_key="serverMerchantPublicKey";
		String file_merchant_private_key="serverMerchantPrivateKey";
		String file_bank_public_key="serverBankPublicKey";
		String file_client_public_key="serverClientPublicKey";
		String file_client_private_key="serverClientPrivateKey";
		//PublicKey ca_pu=readPublicKey(f_ca);
		
		PrivateKey CA_private_key=readPrivateKey(file_CA_private_key);
		PublicKey CA_public_key=readPublicKey(file_CA_public_key);
		PublicKey merchant_public_key=readPublicKey(file_merchant_public_key);
		PrivateKey merchant_private_key=readPrivateKey(file_merchant_private_key);
		PublicKey bank_public_key=readPublicKey(file_bank_public_key);
		PublicKey client_public_key=readPublicKey(file_client_public_key);
		PrivateKey client_private_key=readPrivateKey(file_client_private_key);

		Security.addProvider(new BouncyCastleProvider());
		//#Generate Certificate	
		//Certificate cl_certi=CertiGen(ca_pu_cl,CA_key.getPrivate());
		Certificate merchantCertificate=CertiGen(merchant_public_key,CA_private_key);
		Certificate bankCertificate=CertiGen(bank_public_key,CA_private_key);
		Certificate clientCertificate=CertiGen(client_public_key,CA_private_key);
		
		//Self-signed certificates
		Certificate merchantCertificate_selfsigned=CertiGen(merchant_public_key,merchant_private_key);
		Certificate clientCertificate_selfsigned=CertiGen(client_public_key,client_private_key);
		
		//#Store Certificate	
		//String cl_n="CA_cl";
		//String cl_f="CA_cl_certi.p12";
		String merchant_name="CA_merchant";
		String merchant_file="CA_merchant_certi.p12";
		String bank_name="CA_Bank";
		String bank_file="CA_Bank_certi.p12";
		String client_name="CA_Phoenix";
		String client_file="CA_client_certi.p12";
		
		//self-signed
		String merchant_name_selfsigned="CA_merchant_selfsigned";
		String merchant_file_selfsigned="CA_merchant_selfsigned_certi.p12";
		String client_name_selfsigned="CA_Phoenix_selfsigned";
		String client_file_selfsigned="CA_client_selfsigned_certi.p12";

		//CertiStore(cl_certi,cl_n,cl_f);
		CertiStore(merchantCertificate,merchant_name,merchant_file);
		CertiStore(bankCertificate,bank_name,bank_file);
		CertiStore(clientCertificate,client_name,client_file);
		
		//self-signed
		CertiStore(merchantCertificate_selfsigned,merchant_name_selfsigned,merchant_file_selfsigned);
		CertiStore(clientCertificate_selfsigned,client_name_selfsigned,client_file_selfsigned);
		
		System.out.println("Merchant Certificate:");
		//#Read Certificate
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);
		keyStore.load(new FileInputStream(merchant_file), "password".toCharArray());
		System.out.println(keyStore.getCertificate(merchant_name));//
		Certificate cload=keyStore.getCertificate(merchant_name);
		System.out.println(cload.getPublicKey());
		try{
			cload.verify(CA_public_key);
		}catch(SignatureException e){
			e.printStackTrace();
			System.out.println("Inside catch");
		}
	}

}
