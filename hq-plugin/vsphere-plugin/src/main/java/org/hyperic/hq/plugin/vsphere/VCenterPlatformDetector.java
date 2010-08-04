package org.hyperic.hq.plugin.vsphere;

import java.io.IOException;
import java.util.Properties;

import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.product.PluginException;

public interface VCenterPlatformDetector {
    void discoverPlatforms(Properties props, HQApi hqApi, VSphereUtil vim) throws IOException, PluginException;
}
