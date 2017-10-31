package com.appiancorp.ps.plugins.boxFileUtilities;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;

@ConnectivityServices
@Order({
	"document", "folder", "token", "exceptionMessage"
})

@Category("boxCategory")
public class BoxFileDownloadToAppian {
	private static final Logger LOG = Logger.getLogger(BoxFileDownloadToAppian.class);

	/* Outputs */
	private String exceptionMessage;

	public BoxFileDownloadToAppian() {
	}

	@Function
	public String downloadDocumentToAppian(
			ContentService cs,
			@DocumentDataType @Parameter @Name("document") String document,
			@FolderDataType @Parameter @Name("folder") Long folderId,
			@Parameter @Name("token") String token) throws Exception {
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
		} catch(Exception e) {
			exceptionMessage = e.getMessage();
			LOG.error(exceptionMessage);
		}
		return result;
	}

	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}