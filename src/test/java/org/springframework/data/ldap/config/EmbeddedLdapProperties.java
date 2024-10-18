package org.springframework.data.ldap.config;

import java.util.ArrayList;
import java.util.List;

public class EmbeddedLdapProperties {

	/**
	 * Embedded LDAP port.
	 */
	private int port = 0;

	/**
	 * List of base DNs.
	 */
	private List<String> baseDn = new ArrayList<>();

	/**
	 * Schema (LDIF) script resource reference.
	 */
	private String ldif = "classpath:schema.ldif";

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public List<String> getBaseDn() {
		return this.baseDn;
	}

	public void setBaseDn(List<String> baseDn) {
		this.baseDn = baseDn;
	}

	public String getLdif() {
		return this.ldif;
	}

	public void setLdif(String ldif) {
		this.ldif = ldif;
	}

}
