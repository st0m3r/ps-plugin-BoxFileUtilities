package com.appiancorp.ps.plugins.boxFileUtilities.security;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Jay Berkenbilt
 *
 * This class provides two static methods intended to be called by smart service nodes or any
 * other code that wants to obfuscate passwords.  The encrypt and decrypt routines provide
 * reversible encryption given a static key.  The intended use is for code to hard-code a
 * key for passwords that would otherwise have to be stored in clear text somewhere.  Since
 * someone with access to the code could trivially recover the encrypted password, we refer
 * to this as obfuscation rather than encryption.  This just enables storing the reversibly
 * encrypted string, which is one step better than storing a clear-text password.
 *
 * 11/21/2014: updated obfuscator code to be URL friendly 
 */
public class Obfuscator 
{
	static private final Pattern ENCRYPTED_INPUT = Pattern.compile("^\\$([^\\$]+)\\$([^\\$]+)\\$$");
	private byte[] key;

	/**
	 * Create an Obfuscator object that will use the given key for encryption and decryption.
	 *
	 * @param key
	 */
	public Obfuscator(byte[] key) 
	{
		this.key = key;
	}

	/**
	 * Given a clear text password, return an encrypted version of the password.  The original password
	 * can be recovered by passing the result to the decrypt function with the same key.
	 *
	 * @param input -- clear text password
	 * @return encrypted password
	 */
	public String encrypt(String input) 
	{
		byte[] salt = new byte[8];
		Random r = new SecureRandom();
		r.nextBytes(salt);
		byte[] data = input.getBytes();
		byte[] encryptedSalt = new byte[salt.length];
		RC4 rc4 = new RC4(this.key);
		rc4.process(salt, encryptedSalt);
		rc4.process(data);
		byte[] finalData = new byte[encryptedSalt.length + data.length];
		System.arraycopy(encryptedSalt,0,finalData,0,encryptedSalt.length);
		System.arraycopy(data,0,finalData,encryptedSalt.length,data.length);
		String encoded = "$" + DatatypeConverter.printBase64Binary(salt) + "$" +
				DatatypeConverter.printBase64Binary(finalData) + "$";
		return encoded.replace("+", "_");
	}

	/**
	 * Given a password encrypted by encrypt and the encryption key used to encrypt it, return the
	 * original password.  Given an input string that doesn't look like something this class encrypted,
	 * if the parameter allowCleartext is true, return the input unchanged.  This makes it possible for
	 * callers to accept either encrypted or clear-text passwords.
	 *
	 * @param input
	 * @param allowCleartext
	 * @return
	 */
	public String decrypt(String input, boolean allowCleartext) 
	{
		String result = input.replace("_", "+");
		Matcher m = ENCRYPTED_INPUT.matcher(result);
		if (m.matches()) {
			try {
				byte[] expectedSalt = DatatypeConverter.parseBase64Binary(m.group(1));
				byte[] finalData = DatatypeConverter.parseBase64Binary(m.group(2));
				RC4 rc4 = new RC4(this.key);
				rc4.process(finalData);
				byte[] actualSalt = new byte[expectedSalt.length];
				byte[] decrypted = new byte[finalData.length - expectedSalt.length];
				System.arraycopy(finalData,0,actualSalt,0,expectedSalt.length);
				System.arraycopy(finalData,actualSalt.length,decrypted,0,decrypted.length);
				if (! Arrays.equals(actualSalt, expectedSalt)) {
					throw new IllegalStateException("salt mismatch after decryption");
				}
				result = new String(decrypted);
			}
			catch (Exception e) {
				throw new RuntimeException("encrypted data invalid: " + e.getMessage(), e);
			}
		}
		else if (! allowCleartext) {
			throw new IllegalStateException("encrypted data invalid: input not encrypted");
		}
		return result;
	}
}
