package org.hyperic.hq.web.fileupload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public class FileUploadHandlerExceptionResolver implements HandlerExceptionResolver {
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		ModelAndView modelAndView = null;
		
		if (ex instanceof MaxUploadSizeExceededException) {
			modelAndView = new ModelAndView("admin/managers/plugin/upload/status");

			modelAndView.addObject("success", false);
			modelAndView.addObject("filename", "");
			modelAndView.addObject("messageKey", "admin.managers.plugin.message.file.too.big");
		} else if (ex instanceof MultipartException) {
			modelAndView = new ModelAndView("admin/managers/plugin/upload/status");

			modelAndView.addObject("success", false);
			modelAndView.addObject("filename", "");
			modelAndView.addObject("messageKey", "admin.managers.plugin.message.io.failure");
		}
		
		return modelAndView;
	}
}

