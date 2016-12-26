package ece644demo;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class RandomText {
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
	
	public static String generateNonce() throws NoSuchAlgorithmException { 

		// Create a secure random number generator
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

		// Get 1024 random bits
		//byte[] bytes = new byte[1024/8];
		//sr.nextBytes(bytes);

		// Create two secure number generators with the same seed
		int seedByteCount = 10;
		byte[] seed = sr.generateSeed(seedByteCount);

		//sr = SecureRandom.getInstance("SHA1PRNG");
		sr.setSeed(seed);
		// return sr.toString();
		return String.valueOf(sr.nextInt());
	}

	public static void main(String[] args) throws NoSuchAlgorithmException {
		// TODO Auto-generated constructor stub
		//String a= getSaltString();
		//System.out.print("Nonce: "+a);
		
		String a= generateNonce();
		System.out.println("Nonce: "+a);
		
		java.util.Date today = new java.util.Date();
	    java.sql.Timestamp ts1 = new java.sql.Timestamp(today.getTime());
	    long tsTime1 = ts1.getTime();
	    System.out.println("t1:"+ tsTime1);
	    
	}

}
