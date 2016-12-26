package ece644demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class GetClientPublicKeyFromCert {

	public GetClientPublicKeyFromCert() {
		// TODO Auto-generated constructor stub
	}
	
	public static PublicKey getClientPublicKey() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
		String client_name="CA_Phoenix";
		String client_file="CA_client_certi.p12";
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);
		keyStore.load(new FileInputStream(client_file), "password".toCharArray());
		Certificate cload=keyStore.getCertificate(client_name);
		//System.out.println(cload.getPublicKey());
		PublicKey clientPublicKey = cload.getPublicKey();
		return clientPublicKey;
	}
	
	public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
		getClientPublicKey();
	}

}
