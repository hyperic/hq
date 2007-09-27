/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.util;

import org.apache.oro.text.regex.*;
import org.hyperic.util.validator.DomainValidation;

/**
 * User's may have email addresses
 * This class provides and validates email addresses
 *
 * The Jakarta ORO libraries full featured (i.e. Perl5) regexp support
 */

public class EmailValidator {
    /**
     * Basic syntax check to validate whether something looks like an email
     * address
     */
    public static boolean isValidEmailAddress(String email) {
        /**
         * take a deep breath
         *
         * text%x+y_z@foo.bar.com is a valid email address but
         * text..x@foo.bar.com is not
         *
         * This is derived (the escapes were tweaked to deal with Java's
         * string parser) from Jeffrey Friedl's email pattern described
         * in his O'Reilly book "Mastering Regular Expressions"
         *
         * It is believed to be the pattern that really checks for RFC 822
         * syntax compliance.  Now, there are probably a lot of MTA's that
         * choke on some of the things that RFC 822 allows but for now, let's
         * go with it.
         *
         */
        String rfc822string = 
        "[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\" +
        "x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\" +
        "xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\" +
        "015()]*)*\\)[\\040\\t]*)*(?:(?:[^(\\040)<>@,;:\".\\\\\\[\\]" +
        "\\000-\\037\\x80-\\xff]+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000" +
        "-\\037\\x80-\\xff])|\"[^\\\\\\x80-\\xff\\n\\015\"]*(?:\\\\[^" +
        "\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015\"]*)*\")[\\040\\t]*(?:" +
        "\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\(" +
        "[^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x8" +
        "0-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[" +
        "\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015" +
        "()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]" +
        "*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\" +
        "\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:[^(\\040)<>@," +
        ";:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\040)<>@,;:\"." +
        "\\\\\\[\\]\\000-\\037\\x80-\\xff])|\"[^\\\\\\x80-\\xff\\n\\0" +
        "15\"]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015\"]*)*\"" +
        ")[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\" +
        "x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\" +
        "xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n" +
        "\\015()]*)*\\)[\\040\\t]*)*)*@[\\040\\t]*(?:\\([^\\\\\\x80-\\" +
        "xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xf" +
        "f\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015(" +
        ")]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:[" +
        "^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\0" +
        "40)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[(?:[^\\\\\\" +
        "x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])[\\040\\t]" +
        "*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]" +
        "|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\" +
        "\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*" +
        "\\)[\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\" +
        "015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\01" +
        "5()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\)" +
        ")[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:[^(\\040)" +
        "<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\040)<>@,;" +
        ":\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[(?:[^\\\\\\x80-\\x" +
        "ff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])[\\040\\t]*(?:\\([" +
        "^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\" +
        "\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\" +
        "xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\04" +
        "0\\t]*)*)*|(?:[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\" +
        "xff]+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]" +
        ")|\"[^\\\\\\x80-\\xff\\n\\015\"]*(?:\\\\[^\\x80-\\xff][^\\\\" +
        "\\x80-\\xff\\n\\015\"]*)*\")[^()<>@,;:\".\\\\\\[\\]\\x80-\\x" +
        "ff\\000-\\010\\012-\\037]*(?:(?:\\([^\\\\\\x80-\\xff\\n\\015" +
        "()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]" +
        "*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\" +
        "\\\\x80-\\xff\\n\\015()]*)*\\)|\"[^\\\\\\x80-\\xff\\n\\015\"" +
        "]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015\"]*)*\")[^" +
        "()<>@,;:\".\\\\\\[\\]\\x80-\\xff\\000-\\010\\012-\\037]*)*<[" +
        "\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x" +
        "80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\x" +
        "ff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\" +
        "015()]*)*\\)[\\040\\t]*)*(?:@[\\040\\t]*(?:\\([^\\\\\\x80-\\" +
        "xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff" +
        "\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()" +
        "]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:[^" +
        "(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\04" +
        "0)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[(?:[^\\\\\\" +
        "x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])[\\040\\t]*" +
        "(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|" +
        "\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\" +
        "x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\" +
        ")[\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\" +
        "015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015" +
        "()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))" +
        "[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:[^(\\040)<" +
        ">@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\040)<>@,;:" +
        "\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[(?:[^\\\\\\x80-\\xf" +
        "f\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])[\\040\\t]*(?:\\([^" +
        "\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\" +
        "\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\x" +
        "ff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040" +
        "\\t]*)*)*(?:,[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(" +
        "?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\" +
        "\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\" +
        "x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*@[\\040\\t]*(?:\\([^\\" +
        "\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\" +
        "\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xf" +
        "f\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\" +
        "t]*)*(?:[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]" +
        "+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[" +
        "(?:[^\\\\\\x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])" +
        "[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\" +
        "x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\" +
        "xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\" +
        "015()]*)*\\)[\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\([^\\\\\\x80" +
        "-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\" +
        "xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\01" +
        "5()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?" +
        ":[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\" +
        "040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\\[(?:[^\\\\" +
        "\\x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*\\])[\\040\\" +
        "t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xf" +
        "f]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\" +
        "\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*" +
        ")*\\)[\\040\\t]*)*)*)*:[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n" +
        "\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\0" +
        "15()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\" +
        "))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*)?(?:[^(\\0" +
        "40)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]+(?![^(\\040)<>" +
        "@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\"[^\\\\\\x80-\\xf" +
        "f\\n\\015\"]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015\"" +
        "]*)*\")[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:" +
        "\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^" +
        "\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\" +
        "xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\([^\\" +
        "\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\" +
        "\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xf" +
        "f\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\" +
        "t]*)*(?:[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff]" +
        "+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])|\"[" +
        "^\\\\\\x80-\\xff\\n\\015\"]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80" +
        "-\\xff\\n\\015\"]*)*\")[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n" +
        "\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\0" +
        "15()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\" +
        "))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*)*@[\\040\\" +
        "t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xf" +
        "f]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\" +
        "\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*" +
        ")*\\)[\\040\\t]*)*(?:[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037" +
        "\\x80-\\xff]+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80" +
        "-\\xff])|\\[(?:[^\\\\\\x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-" +
        "\\xff])*\\])[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?" +
        ":(?:\\\\[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\" +
        "\\[^\\x80-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x" +
        "80-\\xff\\n\\015()]*)*\\)[\\040\\t]*)*(?:\\.[\\040\\t]*(?:\\" +
        "([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\[^\\x80-\\xff]|\\([^" +
        "\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x80-\\xff][^\\\\\\x80-" +
        "\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff\\n\\015()]*)*\\)[\\" +
        "040\\t]*)*(?:[^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\" +
        "xff]+(?![^(\\040)<>@,;:\".\\\\\\[\\]\\000-\\037\\x80-\\xff])" +
        "|\\[(?:[^\\\\\\x80-\\xff\\n\\015\\[\\]]|\\\\[^\\x80-\\xff])*" +
        "\\])[\\040\\t]*(?:\\([^\\\\\\x80-\\xff\\n\\015()]*(?:(?:\\\\" +
        "[^\\x80-\\xff]|\\([^\\\\\\x80-\\xff\\n\\015()]*(?:\\\\[^\\x8" +
        "0-\\xff][^\\\\\\x80-\\xff\\n\\015()]*)*\\))[^\\\\\\x80-\\xff" +
        "\\n\\015()]*)*\\)[\\040\\t]*)*)*>)";
        // resume respiration
        PatternCompiler compiler = new Perl5Compiler();
        Pattern rfc822pattern = null;
        boolean match = false;
        try {
            rfc822pattern = compiler.compile(rfc822string);
        } catch(MalformedPatternException e) {
            System.err.println("Failed to compile pattern");
            System.err.println(e.getMessage());
            return false;
        }
        PatternMatcher matcher = new Perl5Matcher();
        if (matcher.matches(email, rfc822pattern)) {
            match = true;
        }
        return match;
    }

    /**
     * Simple check.  Just make sure there is
     * -- only 1 '@' sign
     * -- no whitespace
     * -- some text before the '@' sign
     * -- some text after the '@' sign
     * -- a dot after the '@' sign
     * -- a TLD after that.
     */
    public static boolean isValidSimpleEmail(String email) {
        if (email == null) return false;
        // Just one '@' sign
        int atSignIndex = email.indexOf('@');
        if (atSignIndex == -1) return false;
        if (email.lastIndexOf('@') != atSignIndex) return false;

        // '@' cannot be first char
        if (atSignIndex == 0) return false;

        // '@' cannot be last char
        if (atSignIndex == email.length() - 1) return false;

        // No whitespace before '@'
        for (int i=0; i<atSignIndex; i++) {
            if (Character.isWhitespace(email.charAt(i))) return false;
        }

        // All chars after the '@' must be legal domain name chars
        String domain = email.substring(atSignIndex+1);

        return DomainValidation.isValidDomainName(domain);
    }
}
