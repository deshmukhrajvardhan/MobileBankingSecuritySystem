package ece644demo;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.net.URISyntaxException;

public class SendSMS {

    // Find your Account Sid and Auth Token at twilio.com/console
    public static final String ACCOUNT_SID = "ACba667d4717de2c4eb000631124d4bce5";
    public static final String AUTH_TOKEN = "ec311b0e81d44d18bda641d4d3eaaa35";
    
    public static String generateOTP(String phone_number){
    	Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        int random = (int )(Math.random() * 999999 + 1);
        System.out.println("Number for sending message: "+ phone_number);
        String OTP = Integer.toString(random);
        Message message = Message
                .creator(new PhoneNumber("+14138013878"),  // to
                         new PhoneNumber("+14043416965"),  // from
                         OTP)
                .create();
        return OTP;
    }
    
    public static Void sendFailureMessage(String phone_number){
    	Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        System.out.println("Number for sending message: "+ phone_number);
        String failureMessage = "Your transaction was declined due to incorrect card details.";
        Message message = Message
                .creator(new PhoneNumber("+14138013878"),  // to
                         new PhoneNumber("+14043416965"),  // from
                         failureMessage)
                .create();
        return null;
    }

   /* public static void main(String[] args) throws URISyntaxException {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        int random = (int )(Math.random() * 999999 + 1);
        String a = Integer.toString(random);
        Message message = Message
                .creator(new PhoneNumber("+14138013878"),  // to
                         new PhoneNumber("+14043416965"),  // from
                         a)
                .create();
    }*/
}





