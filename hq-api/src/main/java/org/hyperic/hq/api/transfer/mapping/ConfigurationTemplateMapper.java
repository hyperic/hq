package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.api.model.ConfigurationOption;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationTemplateMapper {
    
    public ConfigurationTemplate toConfigurationTemplate(ConfigSchema configSchema, String configType, ConfigurationTemplate configTemplate){
        if (null == configSchema)
            return configTemplate;
        
        final List<ConfigOption> options = configSchema.getOptions();
        
        if (null == configTemplate) {
            configTemplate = new ConfigurationTemplate();
            configTemplate.setConfigurationOptions(new ArrayList<ConfigurationOption>(options.size()));
        }
        
        final List<ConfigurationOption> apiConfigurationOptions = configTemplate.getConfigurationOptions();
        
        
        for(ConfigOption configOption:options) { 
            apiConfigurationOptions.add(toConfigurationOption(configOption, configType));
        }
        
        
        return configTemplate;
    }
    
    
    public ConfigurationTemplate toConfigurationTemplate(Map<String,ConfigSchema> configSchemas, String configType, ConfigurationTemplate configTemplate){
        if ((null == configSchemas) || configSchemas.isEmpty()) {
            return configTemplate;
        }
        
        int optionsSizeEstimation = configSchemas.values().iterator().next().getOptions().size();
        if (null == configTemplate) {
            configTemplate = new ConfigurationTemplate();
            configTemplate.setConfigurationOptions(new ArrayList<ConfigurationOption>(optionsSizeEstimation));
        }
        
        final List<ConfigurationOption> apiConfigurationOptions = configTemplate.getConfigurationOptions();

        Set<String> optionNames = new HashSet<String>(optionsSizeEstimation);
        for (Map.Entry<String, ConfigSchema> configSchemaPerPlatform:configSchemas.entrySet()) {
            final List<ConfigOption> configOptions = configSchemaPerPlatform.getValue().getOptions();
            for (ConfigOption configOption:configOptions) {
                final String optionName = configOption.getName();
                if (!optionNames.contains(optionName)) {
                    optionNames.add(optionName);
                    apiConfigurationOptions.add(toConfigurationOption(configOption, configType));
                }
            }
        }
        
        return configTemplate;        
    }
    
    @SuppressWarnings("unchecked")
    private ConfigurationOption toConfigurationOption(ConfigOption configOption, String configType) {
         
        List<String> enumValues = null;
        
        if (configOption instanceof EnumerationConfigOption) {
            EnumerationConfigOption enumConfigOption = (EnumerationConfigOption)configOption;
            enumValues = enumConfigOption.getValues();
        }        
        // TODO check about configType, whether different from category
        ConfigurationOption apiConfigurationOption = new ConfigurationOption(configOption.getName(), configOption.getDescription(), configType,// configOption.getCategory(), 
                configOption.getDefault(), configOption.getConfirm(), configOption.isOptional(), toType(configOption), enumValues);
        return apiConfigurationOption;

    }

    
    private String toType(ConfigOption configOption) {
        final String className = configOption.getClass().getSimpleName();
        final String typeName = StringUtils.removeEnd(className, ConfigOption.class.getSimpleName()).toLowerCase();
        // { int, double, boolean, long, string, ip, enum, secret, hidden, port, macaddress, stringarray};
        // integer - int, ipaddress - ip, enumeration - enum

        
        return typeName;                
    }   
    

    
}
