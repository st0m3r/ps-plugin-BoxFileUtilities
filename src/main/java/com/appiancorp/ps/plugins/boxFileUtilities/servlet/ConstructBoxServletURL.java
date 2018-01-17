package com.appiancorp.ps.plugins.boxFileUtilities.servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.appiancorp.ps.plugins.boxFileUtilities.security.UrlEncryptHelper;
import com.appiancorp.suiteapi.cfg.ConfigurationLoader;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;

@Category("boxCategory")
public class ConstructBoxServletURL {
	private static final int BROWSER_URL_LIMIT = 2083;
	private static final String SERVLET_PATH = "/plugins/servlet/boxfiledownload";
	private static final Logger LOG = Logger.getLogger(BoxFileDownloadServlet.class);

	@Function
	public String constructBoxURL(ContentService cs, @Parameter(required = true) @Name("boxId") String boxId,
//			@Parameter(required = true) @Name("groupConstant") String groupConstant,
			@Parameter(required = true) @Name("ds") String ds, @Parameter(required = true) @Name("table") String table,
			@Parameter(required = true) @Name("ownerColumn") String ownerColumn,
			@Parameter(required = true) @Name("documentColumn") String docColumn,
			@Parameter(required = false) @Name("token") String token,
			@DocumentDataType @Parameter(required = false) @Name("privateKey") Long privKey,
			@DocumentDataType @Parameter(required = false) @Name("publicKey") Long pubKey,
			@Parameter(required = false) @Name("sub") String sub,
			@Parameter(required = false) @Name("customClaims") String customClaims,
			@Parameter(required = false) @Name("clientId") String clientIdConstant,
			@Parameter(required = false) @Name("clientSecret") String clientSecretConstant) throws Exception {
		String result = null;
		LOG.debug("Starting constructURL");
		UrlEncryptHelper ueh = new UrlEncryptHelper();
		try {
			// Build the URL path from target and input parameters
			String urlPath = "/" + ConfigurationLoader.getConfiguration().getContextPath() + SERVLET_PATH + "?";
			if (!StringUtils.isBlank(boxId)) {
				urlPath += "document=" + ueh.encryptParam(boxId);
			}
//			if (!StringUtils.isBlank(groupConstant)) {
//				urlPath += "&groupConstant=" + ueh.encryptParam(groupConstant);
//			}
			if (!StringUtils.isBlank(ds)) {
				urlPath += "&ds=" + ueh.encryptParam(ds);
			}
			if (!StringUtils.isBlank(table)) {
				urlPath += "&table=" + ueh.encryptParam(table);
			}
			if (!StringUtils.isBlank(ownerColumn)) {
				urlPath += "&ownerColumn=" + ueh.encryptParam(ownerColumn);
			}
			if (!StringUtils.isBlank(docColumn)) {
				urlPath += "&documentColumn=" + ueh.encryptParam(docColumn);
			}
			if (!StringUtils.isBlank(token)) {
				urlPath += "&token=" + ueh.encryptParam(token);
			}
			if (privKey != null) {
				urlPath += "&privateKey=" + privKey;
			}
			if (pubKey != null) {
				urlPath += "&publicKey=" + pubKey;
			}
			if (!StringUtils.isBlank(sub)) {
				urlPath += "&sub=" + ueh.encryptParam(sub);
			}
			if (!StringUtils.isBlank(customClaims)) {
				urlPath += "&customClaims=" + ueh.encryptParam(customClaims);
			}
			if (!StringUtils.isBlank(clientIdConstant)) {
				urlPath += "&clientId=" + ueh.encryptParam(clientIdConstant);
			}
			if (!StringUtils.isBlank(clientSecretConstant)) {
				urlPath += "&clientSecret=" + ueh.encryptParam(clientSecretConstant);
			}
			result = ConfigurationLoader.getConfiguration().getScheme() + "://"
					+ ConfigurationLoader.getConfiguration().getServerAndPort() + urlPath;
			if (result.length() > BROWSER_URL_LIMIT) {
				LOG.warn("URL length would exceed the maximum browser URL length of " + BROWSER_URL_LIMIT);
				return "";
			}
		} catch (Exception e) {
			LOG.error("Unable to build the URL: " + e.getMessage());
			result = null;
		}
		return result;
	}
}