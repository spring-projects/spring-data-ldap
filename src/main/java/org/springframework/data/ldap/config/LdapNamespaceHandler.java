/*
 * Copyright 2016-2025 the original author or authors.
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

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.ldap.repository.config.LdapRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler} for LDAP configuration.
 *
 * @author Mark Paluch
 */
public class LdapNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {

		registerBeanDefinitionParser("repositories",
				new RepositoryBeanDefinitionParser(new LdapRepositoryConfigurationExtension()));
	}
}
