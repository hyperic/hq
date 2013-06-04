package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.api.model.ConfigurationOption;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.StringConfigOption;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationTemplateMapper {
    
    private static final String CONFIG_OPTION_TYPE_IP = "ip";
    private static final String CONFIG_OPTION_TYPE_INT = "int";
    private static final String UI_CONFIG_TYPE_CONTROL = "Control";
    private static final String UI_CONFIG_TYPE_SHARED = "Shared";
    private static final String UI_CONFIG_TYPE_MONITORING = "Monitoring";

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
    
    /**
     * 
     * @param configSchemas schemas per platform
     * @param configType
     * @param configTemplate the configTemplate to append config options to. If null, a new config
     * template will be returned 
     * @return configurationTemplate with mapped config options
     */
    public ConfigurationTemplate toConfigurationTemplate(Map<String,ConfigSchema> configSchemas, String configType, ConfigurationTemplate configTemplate){
        if ((null == configSchemas) || configSchemas.isEmpty() || (null == configType)) {
            return configTemplate;
        }
        if (null == configTemplate) {
            configTemplate = new ConfigurationTemplate();            
        }        
        
        // a very rough estimation according to the first option list
        final ConfigSchema firstConfigSchema = configSchemas.values().iterator().next();
        final int firstOptionsListSize = 
                (((null == firstConfigSchema) || (null == firstConfigSchema.getOptions())) ? 1 : 
                    firstConfigSchema.getOptions().size());
        int optionsSizeEstimation = ProductPlugin.CONFIGURABLE_TYPES.length * (firstOptionsListSize + 1);
        
        if (null == configTemplate.getConfigurationOptions()) {
            configTemplate.setConfigurationOptions(new ArrayList<ConfigurationOption>(optionsSizeEstimation));
        }
        final List<ConfigurationOption> apiConfigurationOptions = configTemplate.getConfigurationOptions();        

        // Here we combine configuration options from all platforms,
        // and each option name will appear only once in configTemplate
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
        // configType is used instead of category
        ConfigurationOption apiConfigurationOption = new ConfigurationOption(configOption.getName(), configOption.getDescription(), 
                toUIConfigType(configType),// configOption.getCategory(), 
                configOption.getDefault(), configOption.getConfirm(), configOption.isOptional(), toType(configOption), enumValues);
        return apiConfigurationOption;

    }

    
    /**
     * 
     * @param configOption One of { int, double, boolean, long, string, ip, enum, secret, hidden, port, macaddress, stringarray}
     * @return
     */
    private String toType(ConfigOption configOption) {
       
        final String className = configOption.getClass().getSimpleName();
        
        // IntegerConfigOption -> integer, BooleanConfigOption -> boolean, ...
        String typeName = StringUtils.removeEnd(className, ConfigOption.class.getSimpleName()).toLowerCase();        
        
        // integer->int, ipaddress->ip, enumeration->enum, 
        // string.isSecret->secret, string.isHidden->hidden
        if ("integer".equals(typeName))
            typeName = CONFIG_OPTION_TYPE_INT;
        else if ("ipaddress".equals(typeName))
            typeName = CONFIG_OPTION_TYPE_IP;
        else if ("enumeration".equals(typeName))
            typeName = "enum";        
        else if (configOption instanceof StringConfigOption) {
            StringConfigOption strConfigOption = (StringConfigOption) configOption;
            if (strConfigOption.isSecret())
                typeName = "secret";
            else if (strConfigOption.isHidden())
                typeName = "hidden";
        }        
                    
        return typeName;                
    }   
    
    private String toUIConfigType(String configType) {
        // final static Map<String,String>
        String uiConfigType = "";
        if(ProductPlugin.TYPE_MEASUREMENT.equals(configType)) {
            uiConfigType = UI_CONFIG_TYPE_MONITORING;
        }else if(ProductPlugin.TYPE_PRODUCT.equals(configType)) {
            uiConfigType = UI_CONFIG_TYPE_SHARED;
        }else if(ProductPlugin.TYPE_CONTROL.equals(configType)) {
            uiConfigType = UI_CONFIG_TYPE_CONTROL;
        }
        return uiConfigType;
    }

    
}
