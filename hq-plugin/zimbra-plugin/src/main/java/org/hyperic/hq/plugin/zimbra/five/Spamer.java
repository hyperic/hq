package org.hyperic.hq.plugin.zimbra.five;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Spamer {
	public static void main(String[]  aArguments) {
		long start = System.currentTimeMillis();
		int c = 0;
		while (true) {
			c++;
			Spamer.sendEmail("admin@redhat.localdomain", "a@redhat.localdomain", "Testing 1-2-3", "blah blah blah");
			Spamer.sendEmail("admin@redhat.localdomain", "b@redhat.localdomain", "Testing 1-2-3", "blah blah blah");
			Spamer.sendEmail("admin@redhat.localdomain", "c@redhat.localdomain", "Testing 1-2-3", "blah blah blah");
			if(c%10==0){
				long m=((System.currentTimeMillis()-start)/1000)/60;
				System.out.println("-->"+c+ "("+(c*3)/m+"mm)("+m+"m)");
			}
			try {
				Thread.sleep(1000*10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void sendEmail(String aFromEmailAddr, String aToEmailAddr, String aSubject, String aBody) {
		Properties props = new Properties();
		props.setProperty("mail.host", "redhat");
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(aToEmailAddr));
			message.setSubject(aSubject);
			message.setText(aBody);
			Transport.send(message);
		} catch (MessagingException ex) {
			System.err.println("Cannot send email. " + ex);
		}
	}

}
