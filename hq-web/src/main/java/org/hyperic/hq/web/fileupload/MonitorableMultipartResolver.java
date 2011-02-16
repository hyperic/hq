package org.hyperic.hq.web.fileupload;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

public class MonitorableMultipartResolver extends CommonsMultipartResolver {
	public final static String UPLOAD_STATUS_SESSION_ATTRIBUTE = "org.hyperic.hq.web.fileupload.UploadStatus";
	
	@Override
	public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
		String encoding = determineEncoding(request);
		FileUpload fileUpload = prepareFileUpload(encoding);
		UpdateStatusProgressListener progressListener = new UpdateStatusProgressListener() {
			private final Integer BYTES_10K = 1024 * 10;
			
			private UploadStatus uploadStatus = new UploadStatus();
			
			public void update(long bytesRead, long contentLength, int items) {
				// Update if bytesRead == contentLength (finished!) or every 10k (in progress)...
				// (This will get called very often so this makes it a bit more 'performant')
				if (bytesRead == contentLength) return;
				
				uploadStatus.setItem(items);
				uploadStatus.setBytesRead(bytesRead);
				uploadStatus.setContentLength(contentLength);
			}

			public UploadStatus getUploadStatus() {
				return uploadStatus;
			}
		};
		
		fileUpload.setProgressListener(progressListener);

		// Stash the uploadStatus in session for retrieval...
		request.getSession().setAttribute(UPLOAD_STATUS_SESSION_ATTRIBUTE, progressListener.getUploadStatus());
		
		try {
			@SuppressWarnings("unchecked")
			List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
			MultipartParsingResult parsingResult = parseFileItems(fileItems, encoding);
			
			return new DefaultMultipartHttpServletRequest(request, 
					parsingResult.getMultipartFiles(), parsingResult.getMultipartParameters());
		} catch (FileUploadBase.SizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		} catch (FileUploadException ex) {
			throw new MultipartException("Could not parse multipart servlet request", ex);
		} 
	}
	
	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (request != null) {
			// Clean up session...
			request.getSession().removeAttribute(UPLOAD_STATUS_SESSION_ATTRIBUTE);
			
			super.cleanupMultipart(request);
		}
	}
}