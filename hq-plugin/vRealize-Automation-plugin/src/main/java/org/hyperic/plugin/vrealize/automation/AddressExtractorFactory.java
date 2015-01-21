/**
 *
 */
package org.hyperic.plugin.vrealize.automation;

import org.apache.commons.lang.StringUtils;

/**
 * @author imakhlin
 *
 */
public class AddressExtractorFactory {
    /**
     * Returns default FQDN extractor for JDBC URL's
     *
     * @return {@code AddressExtractor}
     */
    public static AddressExtractor getDatabaseServerFqdnExtractor(){
        return new AddressExtractor() {

            public String extractAddress(String containsAddress) {
                if (StringUtils.isBlank(containsAddress)) {
                    return "localhost";
                }
                int beginIndex = containsAddress.indexOf("//") + "//".length();
                containsAddress = containsAddress.substring(beginIndex);
                return containsAddress.split(";")[0];
            }
        };
    }
}
