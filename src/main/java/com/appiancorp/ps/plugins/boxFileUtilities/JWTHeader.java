package com.appiancorp.ps.plugins.boxFileUtilities;

import java.util.HashMap;
import java.util.Map;

public class JWTHeader {
    private String alg = "RS256";
    private String typ = "JWT";
    private String kid = "lcgrayjq";
    private Map<String, Object> map = new HashMap<String, Object>();
    
    public JWTHeader() {
    }

    public JWTHeader(String alg, String typ, String kid) {
    	setAlg(alg);
    	setTyp(typ);
    	setKid(kid);
    }

	public String getAlg() {
		return alg;
	}

	public void setAlg(String alg) {
		this.alg = alg;
	}

	public String getTyp() {
		return typ;
	}

	public void setTyp(String typ) {
		this.typ = typ;
	}

	public String getKid() {
		return kid;
	}

	public void setKid(String kid) {
		this.kid = kid;
	}

	public Map<String, Object> getAsMap() {
		map.put("alg", alg);
		map.put("typ", typ);
		map.put("kid", kid);
		return map;
	}
}