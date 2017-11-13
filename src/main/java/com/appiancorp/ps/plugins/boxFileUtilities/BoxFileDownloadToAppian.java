package com.appiancorp.ps.plugins.boxFileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.ContentExpiredException;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.content.exceptions.NotLockOwnerException;
import com.appiancorp.suiteapi.content.exceptions.PendingApprovalException;
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
@Order({ "boxIds", "createNewDocument", "existingDocument", "addToZip", "newDocumentName", "newDocumentDescription",
		"saveInFolder", "token", "document", "exceptionMessage" })

@Category("boxCategory")
public class BoxFileDownloadToAppian {
	private static final Logger LOG = Logger.getLogger(BoxFileDownloadToAppian.class);
	private File tmpFile;

	/* Outputs */
	private Long outputDocumentId;
	private String exceptionMessage;

	public BoxFileDownloadToAppian() {
	}

	@Function
	public String downloadDocumentToAppian(ContentService cs, @Parameter @Name("boxIds") String[] boxIds,
			@Parameter @Name("createNewDocument") Boolean createNewDocument,
			@DocumentDataType @Parameter @Name("existingDocument") Long existingDocument,
			@Parameter @Name("addToZip") Boolean addToZip, @Parameter @Name("newDocumentName") String newDocumentName,
			@Parameter @Name("newDocumentDescription") String newDocumentDescription,
			@FolderDataType @Parameter @Name("saveInFolder") Long saveInFolder, @Parameter @Name("token") String token,
			@DocumentDataType @Parameter @Name("document") Long document,
			@Parameter @Name("exceptionMessage") String exceptionMessage) throws Exception {
		String result = "Failed to Download File(s)";
		try {
			// Check for multiple Files
			if (boxIds.length > 0) {
				// If more than 1 file, make sure zip is selected
				if (boxIds.length > 1 && !addToZip) {
					exceptionMessage = "Please set Add To Zip to true when providing multiple documents";
					this.exceptionMessage = exceptionMessage;
					LOG.error(exceptionMessage);
					throw new Exception(exceptionMessage);
				}
				// Loop through each Box File ID and download
				ZipOutputStream zout = null;
				for (String id : boxIds) {
					// Download the file
					BoxFile file = downloadBoxFile(id, token);

					// Get information about the file
					BoxFile.Info info = file.getInfo();
					String boxFileName = info.getName();
					String boxFileDesc = info.getDescription();
					String boxFileExt = info.getExtension();
					if (boxFileExt == null && boxFileName.lastIndexOf(".") > 0) {
						// Pull the extension from the file name
						int last = boxFileName.lastIndexOf(".");
						boxFileExt = boxFileName.substring(last + 1);
						boxFileName = boxFileName.substring(0, last);
					}

					if (outputDocumentId == null) {
						// Create the Appian Document
						String name;
						String ext;
						String desc;
						if (newDocumentName == null || newDocumentName.isEmpty()) {
							name = boxFileName;
							ext = boxFileExt;
							desc = boxFileDesc;
						} else if (newDocumentName.contains(".")) {
							int dotIndex = newDocumentName.indexOf('.');
							name = newDocumentName.substring(0, dotIndex);
							ext = newDocumentName.substring(dotIndex + 1);
							desc = newDocumentDescription;
						} else {
							name = newDocumentName;
							ext = boxFileExt;
							desc = newDocumentDescription;
						}
						outputDocumentId = createDocument(cs, createNewDocument, name, ext, desc, saveInFolder,
								existingDocument);

						// Add to Zip?
						if (addToZip) {
							zout = new ZipOutputStream(new FileOutputStream(cs.getInternalFilename(outputDocumentId)));
						}
					}

					if (addToZip && zout != null) {
						addFileToZip(zout, tmpFile, boxFileName+"."+boxFileExt);
						try {
							tmpFile.delete();
						} catch (Exception ex) {
							// ignore
						}
					}
				}

				// Close the zip output stream
				if (zout != null) {
					zout.close();
				}
				cs.setSizeOfDocumentVersion(outputDocumentId);

				if (outputDocumentId != null) {
					if (!addToZip) {
						// Copy downloaded file to Appian Document
						File outputFile = new File(cs.getInternalFilename(outputDocumentId));
						FileUtils.copyFile(tmpFile, outputFile);
						try {
							tmpFile.delete();
						} catch (Exception ex) {
							// ignore
						}
					}

					// Provide feedback
					result = "Downloaded File ID:" + outputDocumentId;
					LOG.info(result);
				}
			} else {
				exceptionMessage = "Please provide a Box File ID to download";
				this.exceptionMessage = exceptionMessage;
				throw new Exception(exceptionMessage);
			}
		} catch (Exception e) {
			exceptionMessage = e.getMessage();
			this.exceptionMessage = exceptionMessage;
			LOG.error(exceptionMessage);
		}
		return result;
	}

	// Download one file from Box
	private BoxFile downloadBoxFile(String id, String token) throws IOException {
		// Setup Box Connection
		BoxAPIConnection api = new BoxAPIConnection(token);
		BoxFile file = new BoxFile(api, id);

		// Download the file
		tmpFile = File.createTempFile(id, id);
		FileOutputStream stream = new FileOutputStream(tmpFile);
		file.download(stream);
		stream.close();
		return file;
	}

	private Long createDocument(ContentService cs, Boolean createNewDocument, String name, String ext, String desc,
			Long saveInFolder, Long existingDocument) throws InvalidContentException, StorageLimitException,
			PrivilegeException, InsufficientNameUniquenessException, DuplicateUuidException, InvalidVersionException,
			NotLockOwnerException, PendingApprovalException, ContentExpiredException {
		if (createNewDocument) {
			Document doc = new Document();
			doc.setDescription(desc);
			doc.setParent(saveInFolder);
			doc.setName(name);
			doc.setExtension(ext);

			return cs.create(doc, ContentConstants.UNIQUE_NONE);
		} else {
			Document oldVersionDoc = (Document) cs.download(existingDocument, ContentConstants.VERSION_CURRENT,
					false)[0];
			Document newVersionDoc = new Document(oldVersionDoc.getParent(), oldVersionDoc.getName(),
					oldVersionDoc.getExtension());
			newVersionDoc.setId(oldVersionDoc.getId());

			return cs.createVersion(newVersionDoc, ContentConstants.UNIQUE_NONE).getId()[0];
		}
	}

	private void addFileToZip(ZipOutputStream zout, File file, String name)
			throws InvalidContentException, IOException, InvalidVersionException, PrivilegeException {
		LOG.debug("Adding a file to the zip");
		InputStream in = new FileInputStream(file);
		byte[] buf = new byte[1024];
		zout.putNextEntry(new ZipEntry(name));
		int len;
		while ((len = in.read(buf)) > 0) {
			zout.write(buf, 0, len);
		}
		zout.closeEntry();
		in.close();
	}

	@Name("document")
	@DocumentDataType
	public Long getDocument() {
		return outputDocumentId;
	}

	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}