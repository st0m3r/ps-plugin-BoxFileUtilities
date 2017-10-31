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
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.ProgressListener;

@ConnectivityServices
@Order({
	"document", "fileName", "token", "exceptionMessage"
})

@Category("boxCategory")
public class BoxFileUpload extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(BoxFileUpload.class);
	private final ContentService cs;

	/* Inputs */
	private long document;
	private String fileName;
	private String token;

	/* Outputs */
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
		String tmpName = cs.getInternalFilename(cs.getVersionId(document, ContentConstants.VERSION_CURRENT));
		File file = new File(tmpName);
		FileInputStream fis = new FileInputStream(file);
		BoxAPIConnection api = new BoxAPIConnection(token);
		BoxFolder rootFolder = BoxFolder.getRootFolder(api);
		try {
			BoxFile.Info newFileInfo = rootFolder.uploadFile(fis, fileName, 2046, new ProgressListener() {
			    public void onProgressChanged(long numBytes, long totalBytes) {
			    }
			});
			result = newFileInfo.getID();
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
		return result;
	}

	@Name("document")
	public void setDocument(long document) {
		this.document = document;
	}

	@Name("fileName")
	public void setFileName(String fileName) {
		this.fileName = fileName;
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