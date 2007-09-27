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

public class DNS_TLD {

    public static final String[] allTLD = {
        // ICANN gTLDs
        "aero", "biz", "com", "coop", "edu", "gov", "info", "int",
        "mil", "museum", "name", "net", "org", "pro", "kids", "golf",

        // "other" gTLDs
        "love", "church", "school", "arts", "shop", "chat", "club",
        "auction", "agent", "llc", "llp", "scifi", "family", "free", 
        "game", "gmbh", "hola", "inc", "law", "ltd", "med", "soc", 
        "sport", "tech", "travel", "video", 

        // ccTLDs
        "ac", "ad", "ae", "af", "ag", "ai", "al", "am", "an", "ao", "aq",
        "ar", "as", "at", "au", "aw", "az", "ba", "bb", "bd", "be", "bf",
        "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bv",
        "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck",
        "cl", "cm", "cn", "co", "cr", "cu", "cv", "cx", "cy", "cz", "de",
        "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "eh", "er", "es",
        "et", "fi", "fj", "fk", "fm", "fo", "fr", "ga", "gd", "ge", "gf",
        "gg", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt",
        "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie",
        "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo",
        "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky",
        "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt", "lu", "lv",
        "ly", "ma", "mc", "md", "mg", "mh", "mk", "ml", "mm", "mn", "mo",
        "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz",
        "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu",
        "nz", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn",
        "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "ru", "rw", "sa",
        "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk", "sl", "sm",
        "sn", "so", "sr", "st", "sv", "sy", "sz", "tc", "td", "tf", "tg",
        "th", "tj", "tk", "tm", "tn", "to", "tp", "tr", "tt", "tv", "tw",
        "tz", "ua", "ug", "uk", "um", "us", "uy", "uz", "va", "vc", "ve",
        "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "yu", "za", "zm", 
        "zw" };

    public static boolean isTLD (String tld) {
        return stringValInArray(tld, allTLD);
    }

    private static boolean stringValInArray(String srch, String[] array){
        for(int i=0; i<array.length; i++){
            if(srch.equals(array[i])){
                return true;
            }
        }
        return false;
    }
}
