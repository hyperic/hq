package org.hyperic.hq.web.fileupload;

import org.apache.commons.fileupload.ProgressListener;

public interface UpdateStatusProgressListener extends ProgressListener {
	public UploadStatus getUploadStatus();
}

