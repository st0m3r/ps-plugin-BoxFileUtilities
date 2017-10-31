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
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.palette.ConnectivityServices;
import com.appiancorp.suiteapi.expression.annotations.Category;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;

@ConnectivityServices
@Order({
	"alg", "typ", "kid", "iss", "sub", "box_sub_type", "aud", "jti", "token", "exceptionMessage"
})

@Category("jwtCategory")
public class CreateJWT extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(CreateJWT.class);
	
	/* Inputs */
	private JWTHeader head = new JWTHeader();
	private JWTClaim claim = new JWTClaim();

	/* Outputs */
	private String token;
	private String exceptionMessage;

	public CreateJWT() {
	}

	@Override
	public void run() throws SmartServiceException {
		createToken();
	}

	public String createToken() {
		RSAPublicKey publicKey = null;
		RSAPrivateKey privateKey = null;
		try {
			publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile("/tmp/box-pub-key.pem", "RSA");
			privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile("/tmp/box-open-key.pem", "RSA");
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

	@Function
	public String createToken(
		@Parameter @Name("sub") String sub,
		@Parameter @Name("box_sub_type") String claims,
		@Parameter(required = false) @Name("alg") String alg,
		@Parameter(required = false) @Name("typ") String typ,
		@Parameter(required = false) @Name("kid") String kid,
		@Parameter(required = false) @Name("iss") String iss,
		@Parameter(required = false) @Name("aud") String aud,
		@Parameter(required = false) @Name("jti") String jti
	) {
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
		return createToken();
	}
	@Name("alg")
	public void setAlg(String s) {
		head.setAlg(s);
	}

	@Name("typ")
	public void setTyp(String s) {
		head.setTyp(s);
	}

	@Name("kid")
	public void setKid(String s) {
		head.setKid(s);
	}

	@Name("iss")
	public void setIss(String s) {
		claim.setIss(s);
	}

	@Name("sub")
	public void setSub(String s) {
		claim.setSub(s);
	}

	@Name("customClaims")
	public void setCustomClaims(String claim) {
		this.claim.setCustomClaim("box_sub_type", claim);
	}

	@Name("aud")
	public void setAud(String s) {
		claim.setAud(s);
	}

	@Name("jti")
	public void setJti(String s) {
		claim.setJti(s);
	}

	@Name("expiration")
	public void setExperation(int val) {
		claim.setExp(val);
	}

	@Name("token")
	public String getToken() {
		return token;
	}

	@Name("exceptionMessage")
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}