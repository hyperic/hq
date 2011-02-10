package org.hyperic.hq.web.fileupload;

public class UploadStatus {
	private Integer item = 0;
	private Long bytesRead = 0l;
	private Long contentLength = -1l;
	
	public UploadStatus() {}
	
	public UploadStatus(Integer item, Long bytesRead, Long contentLength) {
		this.item = item;
		this.bytesRead = bytesRead;
		this.contentLength = contentLength;
	}
	
	public Integer getItem() {
		return item;
	}

	public void setItem(Integer item) {
		this.item = item;
	}

	public Long getBytesRead() {
		return bytesRead;
	}
	
	public void setBytesRead(Long bytesRead) {
		this.bytesRead = bytesRead;
	}
	
	public Long getContentLength() {
		return contentLength;
	}
	
	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}
	
	public boolean isContentLengthKnown() {
		return contentLength == -1;
	}
	
	public boolean isComplete() {
		return getPercentageComplete() == 100;
	}
	
	public Long getPercentageComplete() {
		if (contentLength < 0) {
			return -1l;
		}
		
		return ((100 * bytesRead) / contentLength);
	}
}

