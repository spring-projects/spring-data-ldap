/*
 * Copyright 2024-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.ldap.config;

import jakarta.annotation.PreDestroy;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;

/**
 * Taken from Spring Boot
 */
@Configuration(proxyBeanMethods = false)
public class InMemoryLdapConfiguration {

	private static final String PROPERTY_SOURCE_NAME = "ldap.ports";

	private InMemoryDirectoryServer server;

	private final EmbeddedLdapProperties embeddedProperties;

	public InMemoryLdapConfiguration(EmbeddedLdapProperties embeddedLdapProperties) {
		this.embeddedProperties = embeddedLdapProperties;
	}

	@Bean
	public InMemoryDirectoryServer directoryServer(ApplicationContext applicationContext)
			throws LDAPException {
		String[] baseDn = StringUtils.toStringArray(this.embeddedProperties.getBaseDn());
		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDn);
		setSchema(config);
		InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig("LDAP",
				this.embeddedProperties.getPort());
		config.setListenerConfigs(listenerConfig);
		this.server = new InMemoryDirectoryServer(config);
		importLdif(applicationContext);
		this.server.startListening();
		setPortProperty(applicationContext, this.server.getListenPort());
		return this.server;
	}

	@Bean
	@DependsOn("directoryServer")
	LdapContextSource ldapContextSource(Environment environment, EmbeddedLdapProperties properties) {
		LdapContextSource source = new LdapContextSource();
		Assert.notEmpty(properties.getBaseDn(), "Base DN must be set with at least one value");
		source.setBase(properties.getBaseDn().get(0));
		source.setUrls(determineUrls(environment, properties.getPort()));
		return source;
	}

	@Bean
	LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
		return new LdapTemplate(ldapContextSource);
	}

	private String[] determineUrls(Environment environment, int port) {
		return new String[] { "ldap://localhost:" + (port != 0 ? port : environment.getProperty("local.ldap.port")) };
	}

	private void setSchema(InMemoryDirectoryServerConfig config) {
			config.setSchema(null);
	}

	private void importLdif(ApplicationContext applicationContext) {
		String location = this.embeddedProperties.getLdif();
		if (StringUtils.hasText(location)) {
			try {
				Resource resource = applicationContext.getResource(location);
				if (resource.exists()) {
					try (InputStream inputStream = resource.getInputStream()) {
						this.server.importFromLDIF(true, new LDIFReader(inputStream));
					}
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to load LDIF " + location, ex);
			}
		}
	}

	private void setPortProperty(ApplicationContext context, int port) {
		if (context instanceof ConfigurableApplicationContext configurableContext) {
			MutablePropertySources sources = configurableContext.getEnvironment().getPropertySources();
			getLdapPorts(sources).put("local.ldap.port", port);
		}
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), port);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getLdapPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get(PROPERTY_SOURCE_NAME);
		if (propertySource == null) {
			propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

	@PreDestroy
	public void close() {
		if (this.server != null) {
			this.server.shutDown(true);
		}
	}
}
