package org.springframework.data.ldap.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Embedded LDAP. Taken from Spring Boot.
 *
 * @author Eddú Meléndez
 * @author Mathieu Ouellet
 */
public class EmbeddedLdapProperties {

	/**
	 * Embedded LDAP port.
	 */
	private int port = 0;

	/**
	 * Embedded LDAP credentials.
	 */
	private Credential credential = new Credential();

	/**
	 * List of base DNs.
	 */
	private List<String> baseDn = new ArrayList<>();

	/**
	 * Schema (LDIF) script resource reference.
	 */
	private String ldif = "classpath:schema.ldif";

	/**
	 * Schema validation.
	 */
	private final Validation validation = new Validation();

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Credential getCredential() {
		return this.credential;
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
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

	public Validation getValidation() {
		return this.validation;
	}

	public static class Credential {

		/**
		 * Embedded LDAP username.
		 */
		private String username;

		/**
		 * Embedded LDAP password.
		 */
		private String password;

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		boolean isAvailable() {
			return StringUtils.hasText(this.username) && StringUtils.hasText(this.password);
		}

	}

	public static class Validation {

		/**
		 * Whether to enable LDAP schema validation.
		 */
		private boolean enabled = false;

		/**
		 * Path to the custom schema.
		 */
		private Resource schema;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Resource getSchema() {
			return this.schema;
		}

		public void setSchema(Resource schema) {
			this.schema = schema;
		}

	}

}