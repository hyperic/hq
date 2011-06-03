package org.hyperic.hq.plugin.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileInfo;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Test;


public class ApacheErrorLogPluginTest {

	private ApacheErrorLogPlugin plugin;
	private FileInfo mockFileInfo;

	@Before
	public void setUp() throws PluginException {
		plugin = new ApacheErrorLogPlugin();
		plugin.setName(AppdefEntityConstants.APPDEF_TYPE_PLATFORM + ":4668");
		
		TypeInfo mockTypeInfo = mock(TypeInfo.class);
		plugin.setTypeInfo(mockTypeInfo);
		
		LogTrackPluginManager mockPluginManager = mock(LogTrackPluginManager.class);
		when(mockPluginManager.getName()).thenReturn("Server:E101");
		plugin.init(mockPluginManager);
		
		ConfigResponse mockConfigResponse = mock(ConfigResponse.class);
		plugin.configure(mockConfigResponse);
		
		mockFileInfo = mock(FileInfo.class);
		when(mockFileInfo.getName()).thenReturn("Server");
	}
	
	@Test
	public void parseAllLowerCaseLevel() {
		String line = "[Wed Jan 21 20:47:35 2004] [error] This is a test message";
		TrackEvent event = plugin.processLine(mockFileInfo, line);
		
		assertNotNull(event);
		assertEquals(3, event.getLevel());
		assertEquals("This is a test message", event.getMessage());
	}
	
	@Test
	public void parseAllUpperCaseLevel() {
		String line = "[Wed Jan 21 20:47:35 2004] [ERROR] This is a test message";
		TrackEvent event = plugin.processLine(mockFileInfo, line);
		
		assertNotNull(event);
		assertEquals(3, event.getLevel());
		assertEquals("This is a test message", event.getMessage());
	}
	
	@Test
	public void parseMixedCaseLevel() {
		String line = "[Wed Jan 21 20:47:35 2004] [Error] This is a test message";
		TrackEvent event = plugin.processLine(mockFileInfo, line);
		
		assertNotNull(event);
		assertEquals(3, event.getLevel());
		assertEquals("This is a test message", event.getMessage());
	}

}
