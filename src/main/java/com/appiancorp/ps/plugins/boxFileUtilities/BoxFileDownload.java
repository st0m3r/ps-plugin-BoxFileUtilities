package com.appiancorp.ps.plugins.boxFileUtilities;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;

@ConnectivityServices
@Order({ "boxIds", "createNewDocument", "existingDocument", "addToZip", "newDocumentName", "newDocumentDescription",
		"saveInFolder", "token", "document", "exceptionMessage" })

@Category("boxCategory")
public class BoxFileDownload extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(BoxFileDownload.class);
	private final ContentService cs;

	/* Inputs */
	private String[] boxIds;
	private Boolean addToZip;
	private Boolean createNewDocument;
	private Long existingDocument;
	private String newDocumentName;
	private String newDocumentDescription;
	private Long saveInFolder;
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
			String result = down.downloadDocumentToAppian(cs, boxIds, createNewDocument, existingDocument, addToZip,
					newDocumentName, newDocumentDescription, saveInFolder, token, document, exceptionMessage);
			String exc = down.getExceptionMessage();
			if (exc != null) {
				exceptionMessage = exc;
			}

			// Check the result for a document ID
			int loc = result.lastIndexOf(":");
			if (loc > 0) {
				String str = result.substring(loc + 1);
				try {
					document = Long.valueOf(str);
				} catch (Exception ex) {
					// ignore
				}
			}
		} catch (Exception e) {
			exceptionMessage = e.getMessage();
			LOG.error(exceptionMessage);
		}
	}

	@Name("boxIds")
	public void setBoxIds(String[] ids) {
		this.boxIds = ids;
	}

	@Name("addToZip")
	public void setAddToZip(Boolean b) {
		this.addToZip = b;
	}

	@Name("createNewDocument")
	public void setCreateNewDocument(Boolean b) {
		this.createNewDocument = b;
	}

	@Input(required = Required.OPTIONAL)
	@Name("existingDocument")
	@DocumentDataType
	public void setExistingDocument(Long l) {
		this.existingDocument = l;
	}

	@Input(required = Required.OPTIONAL)
	@Name("newDocumentName")
	public void setNewDocumentName(String s) {
		this.newDocumentName = s;
	}

	@Input(required = Required.OPTIONAL)
	@Name("newDocumentDescription")
	public void setNewDocumentDescription(String s) {
		this.newDocumentDescription = s;
	}

	@Name("saveInFolder")
	@FolderDataType
	public void setSaveInFolder(Long id) {
		this.saveInFolder = id;
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