package com.appiancorp.ps.plugins.boxFileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;

@ConnectivityServices
@Order({ "documents", "folder", "token", "boxIds", "exceptionMessage" })

@Category("boxCategory")
public class BoxFileUpload extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(BoxFileUpload.class);
	private final ContentService cs;

	/* Inputs */
	private Long[] documents;
	private String folder;
	private String token;

	/* Outputs */
	private String[] boxIds;
	private String exceptionMessage;

	public BoxFileUpload(ContentService cs) {
		this.cs = cs;
	}

	@Override
	public void run() throws SmartServiceException {
		try {
			uploadDocument(cs);
		} catch (Exception e) {
			exceptionMessage = e.getMessage();
		}
	}

	public String uploadDocument(ContentService cs) throws Exception {
		String result = "Failed to Upload File";
		int cnt = 0;
		for (Long id : documents) {
			Document doc = (Document) cs.getVersion(id, ContentConstants.VERSION_CURRENT);
			String tmp = cs.getInternalFilename(cs.getVersionId(id, ContentConstants.VERSION_CURRENT));
			File file = new File(tmp);
			FileInputStream fis = new FileInputStream(file);
			BoxAPIConnection api = new BoxAPIConnection(token);
			BoxFolder rootFolder = new BoxFolder(api, folder);
			// BoxFolder rootFolder = BoxFolder.getRootFolder(api);
			try {
				String name = doc.getDisplayName();
				if (name == null) {
					// Build the name
					name = doc.getName() + "." + doc.getExtension();
				}
				BoxFile.Info newFileInfo = rootFolder.uploadFile(fis, name);
				if (cnt > 0) {
					result += ", " + newFileInfo.getID();
				} else {
					result = newFileInfo.getID();
				}
				boxIds[cnt++] = newFileInfo.getID();
				fis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				LOG.info(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				LOG.info(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				LOG.info(e.getMessage());
			}
		}
		return result;
	}

	@Name("document")
	@DocumentDataType
	public void setDocument(Long[] document) {
		this.documents = document;
	}

	@Name("folder")
	public void setFolder(String folder) {
		this.folder = folder;
	}

	@Name("token")
	public void setToken(String token) {
		this.token = token;
	}

	@Name("boxIds")
	public String[] getBoxIds() {
		return boxIds;
	}

	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}