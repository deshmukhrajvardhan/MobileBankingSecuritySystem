#Mobile Banking Security System
##[Presentation](../master/PAYMENT VIA MOBILE PHONES.pdf)
##1. Introduction: *For Actual Implementation details Checkout* [**(1)**HowItWorks](../master/Documentation/HowItWorks.pdf) in the Documentation file

Throughout history human beings have relied on some sort of payment system to purchase the goods or services we wanted or needed. Mobile devices have changed business and now possibly the way financial transactions of all kinds are made. Consumers are willing to utilize mobile phones for payment purposes as it provides an efficient and easier way to process financial transactions. 
Even though widely used, many security concerns are also involved with payment through mobile phones. One of the biggest threats can be the interception of the traffic when the mobile payment is in process. This can lead to identity theft, identification disclosure and replay attacks. Poor data protection controls also lead to data disclosure and privacy infringement. If mobile payment is not done securely then any intruder can obtain the credit card details of the user and use it to transfer illegal funds.
We propose a model to overcome these security concerns which is based on a method used worldwide called as Secure Electronic Transaction(SET). SET uses the concept of Dual signature for secure electronic transaction. Our model involves four entities namely Certification Authority, Client(User), Merchant Server and Payment Gateway(Bank). CA generates the X.509v1 certificates for client, Merchant Server and the Payment Gateway and signs it using its private key. We have assumed that all the 3 entities have access to all these certificates and the public key of CA. The entities derive necessary public keys from the certificates as and when required. This method provides end to end authentication. Merchant server verifies the order details of the user and Payment Gateway verifies the payment information. In our proposed project, we used the hashed version of the order information and payment information to send to the merchant server and the payment gateway server. The information is encrypted and hashed so that only the entities that have the knowledge of the keys being used in the encryption can decrypt the information and use it.
The Android App acts like the client and the Laptop runs CA, Merchant Server and Payment Gateway. To provide Confidentiality and Integrity, several encryption algorithms like RSA, MAC are used to secure the communication between the different entities. Timestamps are used while sending messages, to prevent replay attacks. We have used the concept of OTP to facilitate authentication during payment process. Twilio API is used to send the OTP message. This approach prevents replay attacks and even if the intruder gets hold of the security details of the user he would need the registered phone number to get access to the OTP and complete the payment. To indicate success or failure of the whole process we have implemented email notification whereby an email would be sent to the user.

##2. Installations:
###2.1 Install Softwares
####2.1.1 Install Android studio from the link given:   https://developer.android.com/studio/index.html
####2.1.2 Install Eclipse: http://www.eclipse.org/neon/

##3. Download the Projects:
###3.1 Open Eclipse: choose `File->Open Projects from File System->` <path> `ece644demo`
####3.1.1 Install Jar Libraries given in Pheonix_jars: See the Documentation folder(properties->add external jars)
###3.2 Open Android studio: choose `File->Open File or Project->` <path> `Phoenix`

##*Checkout* [**(2)**Setup.pdf](../master/Documentation/Setup.pdf] in the Documentation file

##4. Run the Projects:

###4.1 Android App: make these settings on your phone: https://developer.android.com/studio/run/device.html
####4.1.1 Connect your Android Phone and in the Android Studio Run'app' (Shift +F10)

###4.2 Eclipse Java servers: *Checkout* [**(3)**DescriptionOfFiles.pdf](../master/Documentation/DescriptionOfFiles.pdf) in the Documentation file DescriptionOfFiles.docx)
####4.2.1 The certificate and key pairs are stored in folder> `Phoenix_extra` (and will be used)
####4.2.1/1 (Optional)(to create the files in 4.2.1)Run the `CertificateAuthorityServer.java` (generates necessary certificates and key-pairs
Note for 4.2.1/1 (should be run only once and the keys shoud be saved in the Andriod program in the `app->res->raw` folder
list of files to be saved in raw folder:
`serverclientprivatekey`
`client_ss.bks`
`ca_merchant_certificate.p12`
`ca_bank_certificate.p12`)
####4.2.2 Run `Database.java` to store the Registeration, login and other details
####4.2.3 Run `MerchantServer.java` to authenticate the user and send product list  
####4.2.4 Run `BankServer.java` to authenticate the Payment  

###4.3 The App run
####4.3.1 Open the App: Enter your details and keep going through the steps
####4.3.2 To run attacks *Checkout* [**(4)**Limitations&Attacker.pdf] (../master/Documentation/Limitations&Attacker.pdf)
