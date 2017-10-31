package com.appiancorp.ps.plugins.boxFileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.security.external.SecureCredentialsStore;
import com.appiancorp.suiteapi.servlet.AppianServlet;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;

/**
 * The URL for the service is:
 * <appian>/suite/plugins/servlet/boxFileDownload
 */
@SuppressWarnings("unused")
public class BoxFileDownloadServlet extends AppianServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger
			.getLogger(BoxFileDownloadServlet.class);

	@Inject
	private SecureCredentialsStore scs;

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			String document = req.getParameter("document");
			String token = req.getParameter("token");

			FileOutputStream stream = null;
			OutputStream respStream = null;
			FileInputStream fin = null;
			try {
				BoxAPIConnection api = new BoxAPIConnection(token);
				BoxFile file = new BoxFile(api, document);
				BoxFile.Info info = file.getInfo();
				File tmpFile = new File(info.getName());
				stream = new FileOutputStream(tmpFile);
				file.download(stream);
				respStream = resp.getOutputStream();
				fin = new FileInputStream(tmpFile);
				byte fileContent[] = new byte[(int) tmpFile.length()];
				fin.read(fileContent);
				respStream.write(fileContent);
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						// swallow the exception
					}
				}
				if (respStream != null) {
					try {
						respStream.close();
					} catch (IOException e) {
						// swallow the exception
					}
				}
				if (fin != null) {
					try {
						fin.close();
					} catch (IOException e) {
						// swallow the exception
					}
				}
			}
		} catch (Exception e) {
			LOG.error("There was an error getting the document from Box: "
					+ e.getMessage(), e);
			resp.setContentType("text/html");
			resp.setStatus(500);
			resp.getWriter()
					.write("There was an error retrieving the document from Box."
							+ "  Please contact your administrator for further details.");
		}
	}

	@Override
	protected void doPost(HttpServletRequest q, HttpServletResponse r)
			throws IOException {
		doGet(q, r);
	}
}