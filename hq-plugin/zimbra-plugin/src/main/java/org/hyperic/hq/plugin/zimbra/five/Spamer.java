/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
