package com.appiancorp.ps.plugins.boxFileUtilities.security;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.appiancorp.ps.plugins.boxFileUtilities.security.Obfuscator;
import com.google.common.base.Strings;

/**
 * Encrypts and decrypts the URL parameters for the servlet using the Obfuscator class written by Jay Berkenbilt
 * Requires a .properties file that has a key "secretkey" 
 * @author betty.huang 
 */

public class UrlEncryptHelper {
	
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(UrlEncryptHelper.class);	
	private static final String CONFIG_BUNDLE_FILE = "box";
	private static final String URL_ENCRYPTION_KEY = "secretkey";
	private Obfuscator obfuscator;
	
	public UrlEncryptHelper() { 
		ResourceBundle rb = ResourceBundle.getBundle(CONFIG_BUNDLE_FILE);
		obfuscator = new Obfuscator(rb.getString(URL_ENCRYPTION_KEY).getBytes()); 		
	}

	public String encryptParam(String param) {
		if (Strings.isNullOrEmpty(param)) {
			return "";
		}
		return obfuscator.encrypt(param);
	}
	
	public  String decryptParam(String cipherText) {
		if (Strings.isNullOrEmpty(cipherText)) {
			return "";
		}		
		return obfuscator.decrypt(cipherText, false);

	}


	
}