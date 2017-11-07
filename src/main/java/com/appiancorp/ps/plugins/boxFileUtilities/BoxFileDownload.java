package com.appiancorp.ps.plugins.boxFileUtilities;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;

@ConnectivityServices
@Order({
	"document", "folder", "token", "exceptionMessage"
})

@Category("boxCategory")
public class BoxFileDownload extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(BoxFileDownload.class);
	private final ContentService cs ;

	/* Inputs */
	private String document;
	private Long folderId;
	private String token;

	/* Outputs */
	private String exceptionMessage;

	public BoxFileDownload(ContentService cs) {
		this.cs = cs;
	}

	@Override
	public void run() throws SmartServiceException {
		BoxFileDownloadToAppian down = new BoxFileDownloadToAppian();
		try {
			down.downloadDocumentToAppian(cs, document, folderId, token);
		} catch (Exception e) {
			exceptionMessage = e.getMessage();
			LOG.error(exceptionMessage);
		}
	}

	@Name("document")
	public void setDocument(String document) {
		this.document = document;
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
	
	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}