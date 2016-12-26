package ece644demo;

import java.util.Properties;

import javax.naming.Context;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendEmail {
	private Context context;
	private String email;
    private String subject;
    private String message;
    private Session session;

    private static String myEmail = ""; //put your gmail id
    private static String myPassword = ""; //put your gmail password
    //you might need to give permissions in your gmail for external applications to connect

    
    public void sendMailVerified(String toAddress){
    	//Creating properties
        Properties props = new Properties();

        //Configuring properties for gmail
        //If you are not using gmail you may need to change the values
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        
      //Creating a new session
        Session session = Session.getDefaultInstance(props,  
        	    new javax.mail.Authenticator() {  
        	      protected PasswordAuthentication getPasswordAuthentication() {  
        	    return new PasswordAuthentication(myEmail,myPassword);  
        	      }  
        	    });  

        try {
            //Creating MimeMessage object
            MimeMessage mm = new MimeMessage(session);

            //Setting sender address
            mm.setFrom(new InternetAddress(myEmail));
            //Adding receiver
			mm.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            //Adding subject
            mm.setSubject("Phoenix Transaction Successful");
            //Adding message
            mm.setText("Your transaction was successful");

            //Sending email
            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        
        
    }
    
    public void sendMailNotVerified(String toAddress){
    	//Creating properties
        Properties props = new Properties();

        //Configuring properties for gmail
        //If you are not using gmail you may need to change the values
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        
      //Creating a new session
        Session session = Session.getDefaultInstance(props,  
        	    new javax.mail.Authenticator() {  
        	      protected PasswordAuthentication getPasswordAuthentication() {  
        	    return new PasswordAuthentication(myEmail,myPassword);  
        	      }  
        	    });  

        try {
            //Creating MimeMessage object
            MimeMessage mm = new MimeMessage(session);

            //Setting sender address
            mm.setFrom(new InternetAddress(myEmail));
            //Adding receiver
			mm.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            //Adding subject
            mm.setSubject("Phoenix Transaction Unuccessful");
            //Adding message
            mm.setText("Your transaction was unsuccessful");

            //Sending email
            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        
        
    }
    
    public static void main( String args[] )
	{
    	//sendMail();
	}
}
