package org.hyperic.hq.measurement.data;

import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;

public enum TopNConfigurationProperties {

    ENABLE_TOPN_COLLECTION("EnableTopN"), TOPN_COLLECTION_INTERVAL_IN_MINUTES("TopNInterval"), TOPN_NUMBER_OF_PROCESSES(
            "TopNProcessNumber");

    private String name;

    private TopNConfigurationProperties(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @SuppressWarnings("serial")
    public ConfigOption getConfigOption() {
        switch (this) {
        case ENABLE_TOPN_COLLECTION:
            return new BooleanConfigOption(this.name, "Enable TopN collection", true);

        case TOPN_COLLECTION_INTERVAL_IN_MINUTES:
            ConfigOption intervalOption = new ConfigOption(this.name, "TopN collection interval in minutes", "1") {

                @Override
                public void checkOptionIsValid(String value) throws InvalidOptionValueException {
                    try {
                        int val = Integer.valueOf(value);
                        if ((val <= 0) || (val > 30)) {
                            throw new InvalidOptionValueException(
                                    "TopN collection interval must be between 1 and 30 minutes");
                        }
                    } catch (Exception e) {
                        throw new InvalidOptionValueException("TopN collection interval must be a number");
                    }
                }
            };
            intervalOption.setOptional(true);
            return intervalOption;

        case TOPN_NUMBER_OF_PROCESSES:
            ConfigOption numberOfProcessesOption = new ConfigOption(this.name,
                    "Number of processes to collect", "10") {

                @Override
                public void checkOptionIsValid(String value) throws InvalidOptionValueException {
                    try {
                        int val = Integer.valueOf(value);
                        if ((val <= 0) || (val > 30)) {
                            throw new InvalidOptionValueException(
                                    "TopN number of processes must be between 1 and 30 minutes");
                        }
                    } catch (Exception e) {
                        throw new InvalidOptionValueException("TopN number of processes must be a number");
                    }
                }
            };
            numberOfProcessesOption.setOptional(true);
            return numberOfProcessesOption;

        default:
            return null;
        }
    }
}
