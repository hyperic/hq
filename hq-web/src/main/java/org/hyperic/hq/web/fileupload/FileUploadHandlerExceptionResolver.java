package org.hyperic.hq.web.fileupload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public class FileUploadHandlerExceptionResolver implements HandlerExceptionResolver {
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		ModelAndView modelAndView = new ModelAndView("admin/managers/plugin/upload/status");

		modelAndView.addObject("success", false);
		modelAndView.addObject("filename", "");
		
		if (ex instanceof FileUploadBase.FileSizeLimitExceededException || 
			ex instanceof FileUploadBase.SizeLimitExceededException || 
			ex instanceof FileUploadBase.FileSizeLimitExceededException) {
			modelAndView.addObject("messageKey", "admin.managers.plugin.message.file.too.big");
		} else if (ex instanceof FileUploadBase.FileUploadIOException || 
				   ex instanceof FileUploadBase.IOFileUploadException) {
			modelAndView.addObject("messageKey", "admin.managers.plugin.message.io.failure");
		} else if (ex instanceof FileUploadBase.InvalidContentTypeException) {
			modelAndView.addObject("messageKey", "admin.managers.plugin.message.invalid.content.type");
		}
		
		return modelAndView;
	}
}

