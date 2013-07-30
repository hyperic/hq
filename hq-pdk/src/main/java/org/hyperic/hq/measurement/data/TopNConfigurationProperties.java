package org.hyperic.hq.measurement.data;

import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;

public enum TopNConfigurationProperties {

    ENABLE_TOPN_COLLECTION("top_processes.enable"), TOPN_COLLECTION_INTERVAL_IN_MINUTES("top_processes.interval"), TOPN_NUMBER_OF_PROCESSES(
            "top_processes.number");

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
            return new BooleanConfigOption(this.name, "Enable top processes collection", true);

        case TOPN_COLLECTION_INTERVAL_IN_MINUTES:
            ConfigOption intervalOption = new ConfigOption(this.name, "Top processes collection interval in minutes",
                    "1") {

                @Override
                public void checkOptionIsValid(String value) throws InvalidOptionValueException {
                    try {
                        int val = Integer.valueOf(value);
                        if (val <= 0) {
                            throw new InvalidOptionValueException("top_processes.interval must be larger than 0");
                        }
                    } catch (NumberFormatException e) {
                        throw new InvalidOptionValueException("top_processes.interval must be a number");
                    }
                }
            };
            intervalOption.setOptional(false);
            return intervalOption;

        case TOPN_NUMBER_OF_PROCESSES:
            ConfigOption numberOfProcessesOption = new ConfigOption(this.name,
                    "Number of processes to collect", "10") {

                @Override
                public void checkOptionIsValid(String value) throws InvalidOptionValueException {
                    try {
                        int val = Integer.valueOf(value);
                        if (val <= 0) {
                            throw new InvalidOptionValueException("top_processes.number must be larger than 0");
                        }
                    } catch (NumberFormatException e) {
                        throw new InvalidOptionValueException("top_processes.number must be a number");
                    }
                }
            };
            numberOfProcessesOption.setOptional(false);
            return numberOfProcessesOption;

        default:
            return null;
        }
    }
}
