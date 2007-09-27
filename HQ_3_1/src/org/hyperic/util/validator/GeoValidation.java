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

package org.hyperic.util.validator;

public class GeoValidation {

    public static final String[] USA = {
        "United States", "USA", "United States of America" };
    public static final String CANADA = "Canada";

    public static final String[] COUNTRIES = {
        "United States",
        "Afghanistan",
        "Albania",
        "Algeria",
        "American Samoa",
        "Andorra",
        "Anguilla",
        "Antarctica",
        "Antigua And Barbuda",
        "Argentina",
        "Armenia",
        "Aruba",
        "Australia",
        "Austria",
        "Azerbaijan",
        "Bahamas",
        "Bahrain",
        "Bangladesh",
        "Barbados",
        "Belarus",
        "Belgium",
        "Belize",
        "Benin",
        "Bermuda",
        "Bhutan",
        "Bolivia",
        "Bosnia and Herzegovina",
        "Botswana",
        "Bouvet Island",
        "Brazil",
        "British Indian Ocean Territory",
        "Brunei Darussalam",
        "Bulgaria",
        "Burkina Faso",
        "Burundi",
        "Cambodia",
        "Cameroon",
        "Canada",
        "Cape Verde",
        "Cayman Islands",
        "Central African Republic",
        "Chad",
        "Chile",
        "China",
        "Christmas Island",
        "Cocos (Keeling) Islands",
        "Colombia",
        "Comoros",
        "Congo",
        "Congo, the Democratic Republic of the",
        "Cook Islands",
        "Costa Rica",
        "Cote d'Ivoire",
        "Croatia",
        "Cyprus",
        "Czech Republic",
        "Denmark",
        "Djibouti",
        "Dominica",
        "Dominican Republic",
        "East Timor",
        "Ecuador",
        "Egypt",
        "El Salvador",
        "England",
        "Equatorial Guinea",
        "Eritrea",
        "Espana",
        "Estonia",
        "Ethiopia",
        "Falkland Islands",
        "Faroe Islands",
        "Fiji",
        "Finland",
        "France",
        "French Guiana",
        "French Polynesia",
        "French Southern Territories",
        "Gabon",
        "Gambia",
        "Georgia",
        "Germany",
        "Ghana",
        "Gibraltar",
        "Great Britain",
        "Greece",
        "Greenland",
        "Grenada",
        "Guadeloupe",
        "Guam",
        "Guatemala",
        "Guinea",
        "Guinea-Bissau",
        "Guyana",
        "Haiti",
        "Heard and Mc Donald Islands",
        "Honduras",
        "Hong Kong",
        "Hungary",
        "Iceland",
        "India",
        "Indonesia",
        "Ireland",
        "Israel",
        "Italy",
        "Jamaica",
        "Japan",
        "Jordan",
        "Kazakhstan",
        "Kenya",
        "Kiribati",
        "Korea, Republic of",
        "Korea (South)",
        "Kuwait",
        "Kyrgyzstan",
        "Lao People's Democratic Republic",
        "Latvia",
        "Lebanon",
        "Lesotho",
        "Liberia",
        "Liechtenstein",
        "Lithuania",
        "Luxembourg",
        "Macau",
        "Macedonia",
        "Madagascar",
        "Malawi",
        "Malaysia",
        "Maldives",
        "Mali",
        "Malta",
        "Marshall Islands",
        "Martinique",
        "Mauritania",
        "Mauritius",
        "Mayotte",
        "Mexico",
        "Micronesia, Federated States of",
        "Moldova, Republic of",
        "Monaco",
        "Mongolia",
        "Montserrat",
        "Morocco",
        "Mozambique",
        "Namibia",
        "Nauru",
        "Nepal",
        "Netherlands",
        "Netherlands Antilles",
        "New Caledonia",
        "New Zealand",
        "Nicaragua",
        "Niger",
        "Nigeria",
        "Niue",
        "Norfolk Island",
        "Northern Ireland",
        "Northern Mariana Islands",
        "Norway",
        "Oman",
        "Pakistan",
        "Palau",
        "Panama",
        "Papua New Guinea",
        "Paraguay",
        "Peru",
        "Philippines",
        "Pitcairn",
        "Poland",
        "Portugal",
        "Puerto Rico",
        "Qatar",
        "Reunion",
        "Romania",
        "Russia",
        "Russian Federation",
        "Rwanda",
        "Saint Kitts and Nevis",
        "Saint Lucia",
        "Saint Vincent and the Grenadines",
        "Samoa (Independent)",
        "San Marino",
        "Sao Tome and Principe",
        "Saudi Arabia",
        "Scotland",
        "Senegal",
        "Serbia and Montenegro",
        "Seychelles",
        "Sierra Leone",
        "Singapore",
        "Slovakia",
        "Slovenia",
        "Solomon Islands",
        "Somalia",
        "South Africa",
        "South Georgia and the South Sandwich Islands",
        "South Korea",
        "Spain",
        "Sri Lanka",
        "St. Helena",
        "St. Pierre and Miquelon",
        "Suriname",
        "Svalbard and Jan Mayen Islands",
        "Swaziland",
        "Sweden",
        "Switzerland",
        "Taiwan",
        "Tajikistan",
        "Tanzania",
        "Thailand",
        "Togo",
        "Tokelau",
        "Tonga",
        "Trinidad",
        "Trinidad and Tobago",
        "Tunisia",
        "Turkey",
        "Turkmenistan",
        "Turks and Caicos Islands",
        "Tuvalu",
        "Uganda",
        "Ukraine",
        "United Arab Emirates",
        "United Kingdom",
        "United States",
        "United States Minor Outlying Islands",
        "Uruguay",
        "USA",
        "Uzbekistan",
        "Vanuatu",
        "Vatican City State (Holy See)",
        "Venezuela",
        "Viet Nam",
        "Virgin Islands (British)",
        "Virgin Islands (U.S.)",
        "Wales",
        "Wallis and Futuna Islands",
        "Western Sahara",
        "Yemen",
        "Zambia",
        "Zimbabwe" };

    public static final String[] US_STATES = {
        "AL",        "AK",        "AZ",        "AR",        "CA",
        "CO",        "CT",        "DE",        "DC",        "FL",
        "GA",        "HI",        "ID",        "IL",        "IN",
        "IA",        "KS",        "KY",        "LA",        "ME",
        "MD",        "MA",        "MI",        "MN",        "MS",        "MO",
        "MT",        "NE",        "NV",        "NH",        "NJ",        "NM",
        "NY",        "NC",        "ND",        "OH",        "OK",
        "OR",        "PA",        "RI",        "SC",
        "SD",        "TN",        "TX",        "UT",        "VT",
        "VA",        "WA",        "WV",        "WI",        "WY" };
    public static final String[] US_STATES_NAMES = {
        "Alabama",     "Alaska",         "Arizona",  "Arkansas",  "California",
        "Colorado",    "Connecticut",    "Delaware", "the District of Columbia",
        "Florida",     "Georgia",        "Hawaii",   "Idaho",      "Illinois",
        "Indiana",     "Iowa",           "Kansas",   "Kentucky",   "Louisiana",
        "Maine",       "Maryland",       "Massachusetts",          "Michigan",
        "Minnesota",   "Mississippi",    "Missouri", "Montana",    "Nebraska",
        "Nevada",      "New Hampshire",  "New Jersey",             "New Mexico",
        "New York",    "North Carolina", "North Dakota",           "Ohio",
        "Oklahoma",    "Oregon",         "Pennsylvania",           "Rhode Island",
        "South Carolina","South Dakota", "Tennesee", "Texas",      "Utah",
        "Vermont",     "Virginia",       "Washington",             "West Virginia",
        "Wisconsin",   "Wyoming" };
    public static final String[] US_TERRITORIES = {
        "AS", "FM", "GU", "MH", "MP", "PW", "PR", "VI"
    };
    public static final String[] US_MILITARY = {
        "AA", "AE", "AP"
    };

    public static final String[] CANADA_PROVINCES = {
        "AB",      "BC",      "MB",      "NB",      "NL",      "NT",
        "NS",      "NU",      "ON",      "PE",      "QC",      "SK",     "YT" };
    public static final String[] CANADA_PROVINCE_NAMES = {
        "Alberta", "British Columbia", "Manitoba", "New Brunswick", 
        "Newfoundland and Labrador", "Northwest Territories", "Nova Scotia",
        "Nunavut", "Ontario", "Prince Edward Island", "Quebec", "Saskatchewan",
        "Yukon"
    };

    private GeoValidation () {}

    public static String validateCountry (String country) 
        throws InvalidCountryException {
        String realCountry = inArray(country, COUNTRIES);
        if (realCountry != null) return realCountry;
        throw new InvalidCountryException(country);
    }

    public static String validateUSState (String state) 
        throws InvalidUSStateException {
        // Try to convert to abbreviation
        if (state == null) throw new InvalidUSStateException("<empty>");
        state = state.trim();
        if (state.length() > 2) {
            try {
                state = getUSStateAbbreviation(state);
            } catch (Exception e) {
                // OK, just roll with it.  It will probably fail later...
            }
        }
        String realState = inArray(state, US_STATES);
        if (realState != null) return realState;

        realState = inArray(state, US_TERRITORIES);
        if (realState != null) return realState;

        realState = inArray(state, US_MILITARY);
        if (realState != null) return realState;

        throw new InvalidUSStateException(state);
    }

    public static String getFullUSStateName(String abbr) 
        throws InvalidUSStateException {
        for (int i=0; i<US_STATES.length; i++) {
            if (US_STATES[i].equalsIgnoreCase(abbr)) {
                return US_STATES_NAMES[i];
            }
        }
        throw new InvalidUSStateException(abbr);
    }
    public static String getUSStateAbbreviation(String state) 
        throws InvalidUSStateException {
        for (int i=0; i<US_STATES_NAMES.length; i++) {
            if (US_STATES_NAMES[i].equalsIgnoreCase(state)) {
                return US_STATES[i];
            }
        }
        throw new InvalidUSStateException(state);
    }
    public static String validateCanadianProvince (String province) 
        throws InvalidCanadianProvinceException {
        // Try to convert to abbreviation
        if (province == null) throw new InvalidCanadianProvinceException("<empty>");
        province = province.trim();
        if (province.length() > 2) {
            try {
                province = getCanadianProvinceAbbreviation(province);
            } catch (Exception e) {
                // OK, just roll with it.  It will probably fail later...
            }
        }
        String realProvince = inArray(province, CANADA_PROVINCES);
        if (realProvince != null) return realProvince;
        throw new InvalidCanadianProvinceException(province);
    }
    public static String getCanadianProvinceAbbreviation(String province) 
        throws InvalidCanadianProvinceException {
        for (int i=0; i<CANADA_PROVINCE_NAMES.length; i++) {
            if (CANADA_PROVINCE_NAMES[i].equalsIgnoreCase(province)) {
                return CANADA_PROVINCES[i];
            }
        }
        throw new InvalidCanadianProvinceException(province);
    }

    public static boolean isUSA (String country) {
        return (inArray(country, USA) != null);
    }
    public static boolean isUSTerritoryOrMilitary (String state) {
        return ((inArray(state, US_TERRITORIES) != null) ||
                (inArray(state, US_MILITARY) != null));
    }
    public static boolean isCanada (String country) {
        return CANADA.equalsIgnoreCase(country);
    }

    public static String validateStateOrProvince (String country, String state) 
        throws InvalidStateOrProvinceException {
        if (isUSA(country)) {
            return validateUSState(state);

        } else if (isCanada(country)) {
            return validateCanadianProvince(state);

        } else {
            // Just assume other places are OK, as long as something is there
            if ( (state != null) && (state.length() > 0) ) return state;
            throw new InvalidStateOrProvinceException("no state/province was specified");
        }
    }

    public static String normalizeZIP(String country, String zip) {
        if (isUSA(country)) {
            try {
                int zint = Integer.parseInt(zip);
                if (zint < 10000) return "0" + zip;
            } catch (Exception e) {
                // Oh well, we tried
            }
        }
        return zip;
    }

    public static void validatePostalCode(String country, String state, String zip)
        throws InvalidPostalCodeException {

        // these are definitely invalid
        if (zip == null || zip.length() == 0) {
            throw new InvalidPostalCodeException("no postal code was specified");
        }

        // don't know much about non-us zipcodes
        if (!isUSA(country)) return;

        // jav's peeps get special treatment
        if (isUSTerritoryOrMilitary(state)) return;
        
        // If the state is invalid, don't do anything, the caller should
        // have validated the state separately
        try {
            state = validateUSState(state);
        } catch (Exception e) { return; }

        zip = zip.trim();
        if (zip.length() > 5) {
            // Strip trailing -XXXX if present
            int dashPos = zip.indexOf("-");
            if (dashPos == -1 || dashPos == 0 || dashPos == zip.length()-1) {
                throw new InvalidPostalCodeException(zip);
            }
            String trailingDigits = zip.substring(dashPos+1).trim();
            if (trailingDigits.length() != 4) {
                throw new InvalidPostalCodeException(zip);
            }
            int zextra;
            try {
                zextra = Integer.parseInt(trailingDigits);
            } catch (NumberFormatException nfe) {
                throw new InvalidPostalCodeException(zip);
            }
            zip = zip.substring(0, dashPos).trim();
        }
        int zint;
        try {
            zint = Integer.parseInt(zip);
        } catch (NumberFormatException nfe) {
            throw new InvalidPostalCodeException(zip);
        }
        if (state.equalsIgnoreCase("AK") && (zint >= 99501 && zint <= 99950)) return;
        if (state.equalsIgnoreCase("AL") && (zint >= 35004 && zint <= 36925)) return;
        if (state.equalsIgnoreCase("AR") && (zint >= 71601 && zint <= 72959)) return;
        if (state.equalsIgnoreCase("AR") && (zint >= 75502 && zint <= 75502)) return;
        if (state.equalsIgnoreCase("AZ") && (zint >= 85001 && zint <= 86556)) return;
        if (state.equalsIgnoreCase("CA") && (zint >= 90001 && zint <= 96162)) return;
        if (state.equalsIgnoreCase("CO") && (zint >= 80001 && zint <= 81658)) return;
        if (state.equalsIgnoreCase("CT") && (zint >= 6001 && zint <= 6389)) return;
        if (state.equalsIgnoreCase("CT") && (zint >= 6401 && zint <= 6928)) return;
        if (state.equalsIgnoreCase("DC") && (zint >= 20001 && zint <= 20039)) return;
        if (state.equalsIgnoreCase("DC") && (zint >= 20042 && zint <= 20599)) return;
        if (state.equalsIgnoreCase("DC") && (zint >= 20799 && zint <= 20799)) return;
        if (state.equalsIgnoreCase("DE") && (zint >= 19701 && zint <= 19980)) return;
        if (state.equalsIgnoreCase("FL") && (zint >= 32004 && zint <= 34997)) return;
        if (state.equalsIgnoreCase("GA") && (zint >= 30001 && zint <= 31999)) return;
        if (state.equalsIgnoreCase("GA") && (zint >= 9901 && zint <= 39901)) return;
        if (state.equalsIgnoreCase("HI") && (zint >= 96701 && zint <= 96898)) return;
        if (state.equalsIgnoreCase("IA") && (zint >= 50001 && zint <= 52809)) return;
        if (state.equalsIgnoreCase("IA") && (zint >= 68119 && zint <= 68120)) return;
        if (state.equalsIgnoreCase("ID") && (zint >= 83201 && zint <= 83876)) return;
        if (state.equalsIgnoreCase("IL") && (zint >= 60001 && zint <= 62999)) return;
        if (state.equalsIgnoreCase("IN") && (zint >= 46001 && zint <= 47997)) return;
        if (state.equalsIgnoreCase("KS") && (zint >= 66002 && zint <= 67954)) return;
        if (state.equalsIgnoreCase("KY") && (zint >= 40003 && zint <= 42788)) return;
        if (state.equalsIgnoreCase("LA") && (zint >= 0001 && zint <= 71232)) return;
        if (state.equalsIgnoreCase("LA") && (zint >= 71234 && zint <=71497)) return;
        if (state.equalsIgnoreCase("MA") && (zint >= 1001 && zint <= 2791)) return;
        if (state.equalsIgnoreCase("MA") && (zint >= 5501 && zint <= 5544)) return;
        if (state.equalsIgnoreCase("MD") && (zint >= 20331 && zint <= 20331)) return;
        if (state.equalsIgnoreCase("MD") && (zint >= 20335 && zint <= 20797)) return;
        if (state.equalsIgnoreCase("MD") && (zint >= 20812 && zint <= 21930)) return;
        if (state.equalsIgnoreCase("ME") && (zint >= 3901 && zint <= 4992)) return;
        if (state.equalsIgnoreCase("MI") && (zint >= 48001 && zint <=49971)) return;
        if (state.equalsIgnoreCase("MN") && (zint >= 55001 && zint <= 56763)) return;
        if (state.equalsIgnoreCase("MO") && (zint >= 63001 && zint <= 65899)) return;
        if (state.equalsIgnoreCase("MS") && (zint >= 38601 && zint <= 39776)) return;
        if (state.equalsIgnoreCase("MS") && (zint >= 71233 && zint <= 71233)) return;
        if (state.equalsIgnoreCase("MT") && (zint >= 59001 && zint <= 59937)) return;
        if (state.equalsIgnoreCase("NC") && (zint >= 27006 && zint <= 28909)) return;
        if (state.equalsIgnoreCase("ND") && (zint >= 58001 && zint <= 58856)) return;
        if (state.equalsIgnoreCase("NE") && (zint >= 68001 && zint <= 68118)) return;
        if (state.equalsIgnoreCase("NE") && (zint >= 68122 && zint <= 69367)) return;
        if (state.equalsIgnoreCase("NH") && (zint >= 3031 && zint <= 3897)) return;
        if (state.equalsIgnoreCase("NJ") && (zint >= 7001 && zint <= 8989)) return;
        if (state.equalsIgnoreCase("NM") && (zint >= 87001 && zint <= 88441)) return;
        if (state.equalsIgnoreCase("NV") && (zint >= 88901 && zint <= 89883)) return;
        if (state.equalsIgnoreCase("NY") && (zint >= 6390 && zint <= 6390)) return;
        if (state.equalsIgnoreCase("NY") && (zint >= 10001 && zint <= 14975)) return;
        if (state.equalsIgnoreCase("OH") && (zint >= 43001 && zint <= 45999)) return;
        if (state.equalsIgnoreCase("OK") && (zint >= 73001 && zint <= 73199)) return;
        if (state.equalsIgnoreCase("OK") && (zint >= 73401 && zint <= 74966)) return;
        if (state.equalsIgnoreCase("OR") && (zint >= 97001 && zint <= 97920)) return;
        if (state.equalsIgnoreCase("PA") && (zint >= 15001 && zint <= 19640)) return;
        if (state.equalsIgnoreCase("RI") && (zint >= 2801 && zint <= 2940)) return;
        if (state.equalsIgnoreCase("SC") && (zint >= 29001 && zint <= 29948)) return;
        if (state.equalsIgnoreCase("SD") && (zint >= 57001 && zint <= 57799)) return;
        if (state.equalsIgnoreCase("TN") && (zint >= 37010 && zint <= 38589)) return;
        if (state.equalsIgnoreCase("TX") && (zint >= 73301 && zint <= 73301)) return;
        if (state.equalsIgnoreCase("TX") && (zint >= 75001 && zint <= 75501)) return;
        if (state.equalsIgnoreCase("TX") && (zint >= 75503 && zint <= 79999)) return;
        if (state.equalsIgnoreCase("TX") && (zint >= 88510 && zint <= 88589)) return;
        if (state.equalsIgnoreCase("UT") && (zint >= 84001 && zint <= 84784)) return;
        if (state.equalsIgnoreCase("VA") && (zint >= 20040 && zint <= 20041)) return;
        if (state.equalsIgnoreCase("VA") && (zint >= 20040 && zint <= 20167)) return;
        if (state.equalsIgnoreCase("VA") && (zint >= 20042 && zint <= 20042)) return;
        if (state.equalsIgnoreCase("VA") && (zint >= 22001 && zint <= 24658)) return;
        if (state.equalsIgnoreCase("VT") && (zint >= 5001 && zint <= 5495)) return;
        if (state.equalsIgnoreCase("VT") && (zint >= 5601 && zint <= 5907)) return;
        if (state.equalsIgnoreCase("WA") && (zint >= 98001 && zint <= 99403)) return;
        if (state.equalsIgnoreCase("WI") && (zint >= 53001 && zint <= 54990)) return;
        if (state.equalsIgnoreCase("WV") && (zint >= 24701 && zint <= 26886)) return;
        if (state.equalsIgnoreCase("WY") && (zint >= 82001 && zint <= 83128)) return;
        
        try {
            state = getFullUSStateName(state);
        } catch (Exception e) {
            // oh well, just use the abbreviation
        }
        throw new InvalidPostalCodeForStateException(state, zip);
    }

    public static void main (String[] args) {
        for (int i=0; i<US_STATES.length; i++) {
            System.err.println(US_STATES[i] + "\t---> " + US_STATES_NAMES[i]);
        }
    }

    private static String inArray(String srch, String[] array){
        for(int i=0; i<array.length; i++){
            if(srch.equalsIgnoreCase(array[i])){
                return array[i];
            }
        }
        return null;
    }
}
