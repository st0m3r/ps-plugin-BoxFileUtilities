package com.appiancorp.ps.plugins.boxFileUtilities;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;

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
		String result = "Failed to Download File";
		try {
			BoxAPIConnection api = new BoxAPIConnection(token);
			BoxFile file = new BoxFile(api, document);
			BoxFile.Info info = file.getInfo();
			File tmpFile = new File(info.getName());
			FileOutputStream stream = new FileOutputStream(tmpFile);
			file.download(stream);
			Document doc = new Document();
			doc.setName(info.getName());
			doc.setDescription(info.getDescription());
			doc.setExtension(info.getExtension());
			doc.setParent(folderId);
			doc.setSize((int)info.getSize());
			long outputDocumentId = cs.create(doc, ContentConstants.UNIQUE_NONE);
			File outputFile = new File(cs.getInternalFilename(outputDocumentId));
			FileUtils.moveFile(tmpFile, outputFile);
			result = "Uploaded File ID: " + outputDocumentId;
			LOG.info(result);
			stream.close();
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