package org.hyperic.hq.ui.action.resource.hub;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.hyperic.hq.ui.Constants;
import org.hyperic.util.pager.PageControl;

public class BreadcrumbUtil {
    public static String createRootBrowseURL(String baseUrl,
                                             ResourceHubForm hubForm,
                                             PageControl pageControl)
    {
        StringBuilder url = new StringBuilder(baseUrl);
        
        url.append("?");
        
        // Set the view param
        String view = hubForm.getView();
        
        if (view != null && !view.equals("")) {
            url.append(ResourceHubForm.VIEW_PARAM).append("=").append(encode(view)).append("&");
        }
        
        // Set the entity type id param
        int entityTypeId = hubForm.getFf();
        
        url.append(ResourceHubForm.ENTITY_TYPE_ID_PARAM).append("=").append(entityTypeId).append("&");
        
        // Set the keywords param
        String keywords = hubForm.getKeywords();
        
        if (keywords != null && !keywords.equals("")) {
            url.append(ResourceHubForm.KEYWORDS_PARAM).append("=").append(encode(keywords)).append("&");
        }
        
        // Set the resource type id param
        String resourceTypeId = hubForm.getFt();
        
        if (resourceTypeId != null && !resourceTypeId.equals("")) {
            url.append(ResourceHubForm.RESOURCE_TYPE_ID_PARAM).append("=").append(encode(resourceTypeId)).append("&");
        }
        
        // Set group type id param
        Integer groupTypeId = hubForm.getG();
        
        if (groupTypeId != null) {
            url.append(ResourceHubForm.GROUP_TYPE_ID_PARAM).append("=").append(groupTypeId).append("&");
        }
        
        // Set group id param
        String groupId = hubForm.getFg();
        
        if (groupId != null && groupId.equals("")) {
            url.append(ResourceHubForm.GROUP_ID_PARAM).append("=").append(encode(groupId)).append("&");
        }
        
        url.append(ResourceHubForm.ANY_FLAG_PARAM).append("=").append(hubForm.isAny()).append("&");
        url.append(ResourceHubForm.OWNER_FLAG_PARAM).append("=").append(hubForm.isOwn()).append("&");
        url.append(ResourceHubForm.UNAVAILABLE_FLAG_PARAM).append("=").append(hubForm.isUnavail()).append("&");
        url.append(Constants.PAGENUM_PARAM).append("=").append(pageControl.getPagenum()).append("&");
        url.append(Constants.PAGESIZE_PARAM).append("=").append(pageControl.getPagesize());

        return url.toString();
    }
    
    public static String createRootBrowseURL(String baseUrl, int entityTypeId, Integer groupTypeId) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        url.append("?");
        
        // Set the entity type id param
        url.append(ResourceHubForm.ENTITY_TYPE_ID_PARAM).append("=").append(entityTypeId).append("&");
        
        if (groupTypeId != null) {
            url.append(ResourceHubForm.GROUP_TYPE_ID_PARAM).append("=").append(groupTypeId).append("&");
        }
        
        return url.toString();
    }
    
    public static String createResourceURL(String baseUrl, String entityId, String ctype) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        url.append("?");
        url.append(Constants.ENTITY_ID_PARAM).append("=").append(encode(entityId));
        
        if (ctype != null && !ctype.equals("")) {
            url.append("&").append(Constants.CHILD_RESOURCE_TYPE_ID_PARAM).append("=").append(encode(ctype));
        }
        
        return url.toString();
    }
    
    public static String createReturnToURL(String baseUrl) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        if (!baseUrl.endsWith("&") && !baseUrl.endsWith("?")) {
            url.append("&");
        }
        
        url.append(Constants.RETURN_TO_LINK_PARAM_NAME).append("=").append(Constants.RETURN_TO_LINK_PARAM_VALUE);
        
        return url.toString();
    }
    
    private static String encode(Object value) {
        if (value == null) return "";
        
        try {
            return URLEncoder.encode(value.toString(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            // Encoding error, pass the original value instead
            return value.toString();
        }
    }
}
