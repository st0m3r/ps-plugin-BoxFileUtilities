package com.appiancorp.ps.plugins.boxFileUtilities;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;

@Category("jwtCategory")
public class CreateJWT {
	private static final Logger LOG = Logger.getLogger(CreateJWT.class);

	/* Inputs */
	private long privateKeyId;
	private long publicKeyId;
	private JWTHeader head = new JWTHeader();
	private JWTClaim claim = new JWTClaim();

	/* Outputs */
	private String token;
	private String exceptionMessage;

	@Function
	public String createToken(
		ContentService cs,
		@Parameter @Name("privateKey") Long privKey,
		@Parameter @Name("publicKey") Long pubKey,
		@Parameter @Name("sub") String sub,
		@Parameter @Name("customClaims") String claims,
		@Parameter(required = false) @Name("alg") String alg,
		@Parameter(required = false) @Name("typ") String typ,
		@Parameter(required = false) @Name("kid") String kid,
		@Parameter(required = false) @Name("iss") String iss,
		@Parameter(required = false) @Name("aud") String aud,
		@Parameter(required = false) @Name("jti") String jti
	) {
		if (privKey > 0) {
			privateKeyId = privKey;
		}
		if (pubKey > 0) {
			publicKeyId = pubKey;
		}
		if (alg != null && alg.length() > 0) {
			head.setAlg(alg);
		}
		if (typ != null && typ.length() > 0) {
			head.setTyp(typ);
		}
		if (kid != null && kid.length() > 0) {
			head.setKid(kid);
		}
		if (iss != null && iss.length() > 0) {
			claim.setIss(iss);
		}
		if (sub != null && sub.length() > 0) {
			claim.setSub(sub);
		}
		if (claims != null && claims.length() > 0) {
			claim.setCustomClaim("box_sub_type", claims);
		}
		if (aud != null && aud.length() > 0) {
			claim.setAud(aud);
		}
		if (jti != null && jti.length() > 0) {
			claim.setJti(jti);
		}
		RSAPublicKey publicKey = null;
		RSAPrivateKey privateKey = null;
		try {
			String privKeyName = cs.getInternalFilename(cs.getVersionId(privateKeyId, ContentConstants.VERSION_CURRENT));
			String pubKeyName = cs.getInternalFilename(cs.getVersionId(publicKeyId, ContentConstants.VERSION_CURRENT));
			privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile(privKeyName, "RSA");
			publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile(pubKeyName, "RSA");
		} catch (Exception e) {
			exceptionMessage = exceptionMessage + System.getProperty("line.separator") + e.toString();
			LOG.error("Error reading certificates", e);
			return "Error reading certificates";
		}
		try {
			Date now = new Date();
			Date nowish = new Date(now.getTime() + 60000);
		    Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
		    Builder builder = JWT.create();
		    builder.withHeader(head.getAsMap());
		    builder.withIssuer(claim.getIss());
		    builder.withSubject(claim.getSub());
		    Set<String> keys = claim.getCustomClaims().keySet();
		    for (String key : keys) {
			    builder.withClaim(key, claim.getCustomClaim(key));
		    }
		    builder.withAudience(claim.getAud());
		    builder.withJWTId(claim.getJti());
		    builder.withExpiresAt(nowish);
		    token = builder.sign(algorithm);
		} catch (JWTCreationException e) {
			exceptionMessage = exceptionMessage + System.getProperty("line.separator") + e.toString();
			LOG.error("Error creating JWT", e);
			return "Error creating JWT";
		}
		return token;
	}
}