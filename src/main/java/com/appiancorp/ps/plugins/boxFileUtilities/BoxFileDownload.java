package com.appiancorp.ps.plugins.boxFileUtilities;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;

@ConnectivityServices
@Order({
	"boxId", "folder", "token", "document", "exceptionMessage"
})

@Category("boxCategory")
public class BoxFileDownload extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(BoxFileDownload.class);
	private final ContentService cs ;

	/* Inputs */
	private String boxId;
	private Long folderId;
	private String token;

	/* Outputs */
	private Long document;
	private String exceptionMessage;

	public BoxFileDownload(ContentService cs) {
		this.cs = cs;
	}

	@Override
	public void run() throws SmartServiceException {
		BoxFileDownloadToAppian down = new BoxFileDownloadToAppian();
		try {
			String result = down.downloadDocumentToAppian(cs, boxId, folderId, token);
			//Check the result for a document ID
			int loc = result.lastIndexOf(":");
			if (loc > 0) {
				String str = result.substring(loc+1);
				try {
					document = Long.valueOf(str);
				} catch (Exception ex) {
					//ignore
				}
			}
		} catch (Exception e) {
			exceptionMessage = e.getMessage();
			LOG.error(exceptionMessage);
		}
	}

	@Name("boxId")
	public void setBoxId(String id) {
		this.boxId = id;
	}

	@Name("folder")
	@FolderDataType
	public void setFolderId(Long id) {
		this.folderId = id;
	}

	@Name("token")
	public void setToken(String token) {
		this.token = token;
	}

	@Name("document")
	@DocumentDataType
	public Long getDocument() {
		return document;
	}

	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}