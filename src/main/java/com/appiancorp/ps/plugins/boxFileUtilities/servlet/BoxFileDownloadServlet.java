package com.appiancorp.ps.plugins.boxFileUtilities.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.json.JSONObject;

import com.appiancorp.ps.plugins.boxFileUtilities.jwt.CreateJWT;
import com.appiancorp.ps.plugins.boxFileUtilities.security.UrlEncryptHelper;
import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.WebServiceContextFactory;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.personalization.GroupService;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.rules.Constant;
import com.appiancorp.suiteapi.security.external.SecureCredentialsStore;
import com.appiancorp.suiteapi.servlet.AppianServlet;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;

/**
 * The URL for the service is: <appian>/suite/plugins/servlet/boxFileDownload
 */
@SuppressWarnings("unused")
public class BoxFileDownloadServlet extends AppianServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(BoxFileDownloadServlet.class);
	private static final String ACCESS_GROUP_UUID = "_a-0000e002-7d67-8000-06a9-01ef9001ef90_35240";

	@Inject
	private SecureCredentialsStore scs;

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Get the content service
		ServiceContext sc = WebServiceContextFactory.getServiceContext(req);
		ContentService cs = ServiceLocator.getContentService(sc);
		GroupService gs = ServiceLocator.getGroupService(sc);
		ProcessDesignService pds = ServiceLocator.getProcessDesignService(sc);
		DataSource ds;

		try {
			// Retrieve the parameters from the URL
			// document: the Box ID of the requested document
			// token: a JWT for authentication
			// groupConstant: pointer to a group of allowed downloaders
			// ds: Datasource to query
			// table: table or view to query
			// ownerColumn: column with the Appian ID of the document owner
			// docColumn: column with the Box ID of the document
			// Some of the parameters will be obfuscated, must decrypt first
		    UrlEncryptHelper ueh = new UrlEncryptHelper();
		    String document = ueh.decryptParam(req.getParameter("document"));
			String token = ueh.decryptParam(req.getParameter("token"));
//			String groupConstant = ueh.decryptParam(req.getParameter("groupConstant"));
			String jndi = ueh.decryptParam(req.getParameter("ds"));
			String table = ueh.decryptParam(req.getParameter("table"));
			String ownerColumn = ueh.decryptParam(req.getParameter("ownerColumn"));
			String docColumn = ueh.decryptParam(req.getParameter("documentColumn"));

			// Check that this user is in the access group allowed to download from Box
			String userName = sc.getName();
//			Long accessGroupId = getAccessGroupId(cs, pds, groupConstant);
			Long accessGroupId = cs.getIdByUuid(ACCESS_GROUP_UUID);
			if(!gs.isUserMember(userName, accessGroupId)) {
				// Check if the user ID is in a table for this doc ID
				String owner = null;
				boolean ownerMatched = false;
				if(jndi != null) {
					Context ctx = new InitialContext();
				    ds = (DataSource)ctx.lookup(jndi);
				    String sql = "select " + ownerColumn + " from " + table + " where " + docColumn + " = " + document;
				    try (Connection con = ds.getConnection();
				            PreparedStatement stmt = con.prepareStatement(sql);
				            ResultSet rs = stmt.executeQuery();) {

				          LOG.info(" Connected to JNDI = " + con);
				          LOG.info(" Statement = " + stmt);
				          LOG.info(" Resultset = " + rs);

				          while (rs.next()) {
				        	  	owner = rs.getString(ownerColumn);
						    //Compare the owner retrieved from the DB to the logged in user
				        	  	if (owner != null && owner.equalsIgnoreCase(userName)) {
				        	  		ownerMatched = true;
				        	  		break;
				        	  	}
				          }
				    }

				    //See if the owner matched
				    if (!ownerMatched) {
						throw new IllegalStateException("User does not own this Box file and is not in the group allowed to download from Box");
				    }
				} else {
					throw new IllegalStateException("User is not allowed to download from Box");
				}
			}

			Boolean haveToken;
			String clientId = null;
			String clientSecret = null;
//			String clientIdConst = null;
//			String clientSecretConst = null;
			Long privKey = null;
			Long pubKey = null;
			String sub = null;
			String customClaims = null;
			String jwt = null;

			if (token != null && token.length() > 0) {
				haveToken = true;
			} else {
				haveToken = false;
				privKey = Long.valueOf(req.getParameter("privateKey"));
				pubKey = Long.valueOf(req.getParameter("publicKey"));
				sub = ueh.decryptParam(req.getParameter("sub"));
				customClaims = ueh.decryptParam(req.getParameter("customClaims"));
				clientId = ueh.decryptParam(req.getParameter("clientId"));
				clientSecret = ueh.decryptParam(req.getParameter("clientSecret"));
//				clientId = getStringFromConstant(clientIdConst, cs, pds);
//				clientSecret = getStringFromConstant(clientSecretConst, cs, pds);
			}

			FileOutputStream stream = null;
			OutputStream respStream = null;
			FileInputStream fin = null;
			try {
				if (!haveToken) {
					// Get a JWT token for Box API
					CreateJWT util = new CreateJWT();
					jwt = util.createToken(cs, privKey, pubKey, sub, customClaims, null, null, null, null, null, null);
					try {
						token = getAccessToken(clientId, clientSecret, jwt);
					} catch (Exception e) {
						LOG.error("There was an error getting the access token from Box: " + e.getMessage(), e);
						resp.setContentType("text/html");
						resp.setStatus(500);
						resp.getWriter().write("There was an error retrieving the access token from Box."
								+ "  Please contact your administrator for further details.");
					}
				}

				// Connect to Box and download the requested file
				BoxAPIConnection api = new BoxAPIConnection(token);

				// Get the information about the file
				BoxFile file = new BoxFile(api, document);
				BoxFile.Info info = file.getInfo();
				File tmpFile = new File(info.getName());

				// Download the file
				stream = new FileOutputStream(tmpFile);
				file.download(stream);

				// Determine the file's type & inform the response object
				String mimeType = new Tika().detect(tmpFile);
				resp.setContentLength((int) tmpFile.length());
				resp.setContentType(mimeType);
				String fileName = URLEncoder.encode(info.getName(), "UTF-8");
				fileName = URLDecoder.decode(fileName, "ISO8859_1");
				resp.setHeader("Content-disposition", "attachment; filename=" + fileName);

				// Get the OutputStream of the response object
				respStream = resp.getOutputStream();

				// Get a FileInputStream for the file
				fin = new FileInputStream(tmpFile);

				// Setup a byte array the size of the file
				byte fileContent[] = new byte[(int) tmpFile.length()];

				// Place the file content into the byte array
				fin.read(fileContent);

				// Place the byte array into the response object stream
				respStream.write(fileContent);
			} catch (BoxAPIException e) {
				LOG.error("There was an error getting the document from Box: " + e.getMessage(), e);
				resp.setContentType("text/html");
				resp.setStatus(500);
				resp.getWriter().write("There was an error retrieving the document from Box."
						+ "  Please contact your administrator for further details.");
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
		} catch (IllegalStateException ise) {
			LOG.error("There was an error getting the document from Box: " + ise.getMessage(), ise);
			resp.setContentType("text/html");
			resp.setStatus(500);
			resp.getWriter().write("There was an error retrieving the document from Box: " + ise.getMessage()
					+ ".  Please contact your administrator for further details.");
		} catch (Exception e) {
			LOG.error("There was an error getting the document from Box: " + e.getMessage(), e);
			resp.setContentType("text/html");
			resp.setStatus(500);
			resp.getWriter().write("There was an error retrieving the document from Box."
					+ "  Please contact your administrator for further details.");
		}
	}

	private String getStringFromConstant(String cons, ContentService cs, ProcessDesignService pds) {
		String val = null;
		Content cv = null;
	    String uuid = pds.externalizeExpression(cons);
	    if(uuid != null){
	      uuid = uuid.replace("#", "").replace("\"", "");            
	    }
	    Long id = cs.getIdByUuid(uuid);
	    try {
			cv = cs.getVersion(id, ContentConstants.VERSION_CURRENT);
			val = (String) cv.getAttributes().get("value");
		} catch (PrivilegeException e) {
			e.printStackTrace();
		} catch (InvalidContentException e) {
			e.printStackTrace();
		} catch (InvalidVersionException e) {
			e.printStackTrace();
		}
	    return val;
	}

	public Long getAccessGroupId(ContentService cs, ProcessDesignService pds, String cons)
			throws PrivilegeException, InvalidContentException, InvalidVersionException {
		Long accessGroupId = null;
	    String uuid = pds.externalizeExpression(cons);
	    if(uuid != null){
	      uuid = uuid.replace("#", "").replace("\"", "");            
	    }
		Long constantId = cs.getIdByUuid(uuid);
		if (constantId == null) {
			throw new IllegalStateException(
					"The constant containing the access group for the documents cannot be found on this environment. UUID: "
							+ constantId);
		} else {
			Content content = cs.getVersion(constantId, -1);
			Constant constant = (Constant) content;
			accessGroupId = (Long) constant.getTypedValue().getValue();
		}
		return accessGroupId;
	}

	public String getAccessToken(String clientId, String clientSecret, String jwt) throws Exception {
		String token = null;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://api.box.com/oauth2/token");
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"));
		params.add(new BasicNameValuePair("client_id", clientId));
		params.add(new BasicNameValuePair("client_secret", clientSecret));
		params.add(new BasicNameValuePair("assertion", jwt));
		httpPost.setEntity(new UrlEncodedFormEntity(params));
		CloseableHttpResponse tokenResponse = null;
		try {
			tokenResponse = httpclient.execute(httpPost);

			if (tokenResponse.getStatusLine().getStatusCode() >= 300) {
				LOG.error("Status code greater than 200 for request: " + httpPost.toString());
				throw new IllegalStateException(
						"There was an error retrieving the access code: " + tokenResponse.getStatusLine());
			} else {
				HttpEntity respEntity = tokenResponse.getEntity();

				if (respEntity != null) {
					// EntityUtils to get the response content
					String content = EntityUtils.toString(respEntity);
					// Parse the JSON content to get the access_token
					// {"access_token":"ChcFymZf4uXOICa2rAUOrQvjFzstNuh3","expires_in":4122,"restricted_to":[],"token_type":"bearer"}
					JSONObject obj = new JSONObject(content);
					token = obj.getString("access_token");
				}
			}
		} finally {
			if (tokenResponse != null) {
				try {
					tokenResponse.close();
				} catch (IOException e) {
					// swallow the exception
				}
			}
		}
		return token;
	}

	@Override
	protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
		doGet(q, r);
	}
}