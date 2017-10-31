package com.appiancorp.ps.plugins.boxFileUtilities;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTClaim {
	private Date now = new Date();      
	private Long longTime = new Long(now.getTime()/1000);
	private String iss = "ns0weg9eyvsspdgk7yposw1z1m2pg88p";
	private String sub = "382841";
	private String aud = "https://api.box.com/oauth2/token";
	private String jti = "AppianBoxIntegration" + longTime.intValue();
	private Map<String, String> customClaims = new HashMap<String, String>();
	private long exp = 1505244926;
    
    public JWTClaim() {
    	customClaims.put("box_sub_type", "enterprise");
    }

    public JWTClaim(long exp) {
    }

	public String getIss() {
		return iss;
	}

	public void setIss(String iss) {
		this.iss = iss;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	public Map<String,String> getCustomClaims() {
		return customClaims;
	}

	public void setCustomClaims(Map<String,String> map) {
		this.customClaims = map;
	}

	public String getCustomClaim(String name) {
		return customClaims.get(name);
	}

	public void setCustomClaim(String key, String val) {
		this.customClaims.put(key, val);
	}

	public String getAud() {
		return aud;
	}

	public void setAud(String aud) {
		this.aud = aud;
	}

	public String getJti() {
		return jti;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}

	public long getExp() {
		return exp;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}
}