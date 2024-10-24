/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.data.ldap.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.ldap.config.EmbeddedLdapProperties;
import org.springframework.data.ldap.config.InMemoryLdapConfiguration;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for Repositories using value expressions.
 *
 * @author Marcin Grzejszczak
 */
@SpringJUnitConfig
@TestPropertySource(properties = { "full.name=John Doe" })
class AnnotatedLdapRepositoryQueryIntegrationTests {

	@Autowired QueryRepository queryRepository;

	@Test // GH-453
	void filterWithNamedParameters() {

		List<SchemaEntry> objects = queryRepository.namedParameters("John Doe");

		assertThatReturnedObjectIsJohnDoe(objects);
	}

	@Test // GH-453
	void filterWithPositionalParameters() {

		List<SchemaEntry> objects = queryRepository.indexedParameters("John Doe");

		assertThatReturnedObjectIsJohnDoe(objects);
	}

	@Test // GH-453
	void filterWithSpelExpression() {

		List<SchemaEntry> objects = queryRepository.spelParameters();

		assertThatReturnedObjectIsJohnDoe(objects);
	}

	@Test // GH-453
	void filterWithPropertyPlaceholder() {

		List<SchemaEntry> objects = queryRepository.propertyPlaceholderParameters();

		assertThatReturnedObjectIsJohnDoe(objects);
	}

	@Test // GH-453
	void baseWithNamedParameters() {

		List<SchemaEntry> objects = queryRepository.baseNamedParameters("John Doe", "memorynotfound");

		assertThatReturnedObjectIsJohnDoe(objects);
	}

	private static void assertThatReturnedObjectIsJohnDoe(List<SchemaEntry> objects) {

		assertThat(objects).hasSize(1);
		assertThat(objects.get(0).fullName).isEqualTo("John Doe");
		assertThat(objects.get(0).lastName).isEqualTo("Doe");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(InMemoryLdapConfiguration.class)
	@EnableLdapRepositories(considerNestedRepositories = true)
	static class TestConfig {

		@Bean
		EmbeddedLdapProperties embeddedLdapProperties() {
			EmbeddedLdapProperties embeddedLdapProperties = new EmbeddedLdapProperties();
			embeddedLdapProperties.setBaseDn(Arrays.asList("dc=com", "dc=memorynotfound"));
			return embeddedLdapProperties;
		}
	}

	interface QueryRepository extends LdapRepository<SchemaEntry> {

		@Query(value = "(cn=:fullName)")
		List<SchemaEntry> namedParameters(String fullName);

		@Query(value = "(cn=?0)")
		List<SchemaEntry> indexedParameters(String fullName);

		@Query(value = "(cn=:#{'John ' + 'Doe'})")
		List<SchemaEntry> spelParameters();

		@Query(value = "(cn=?${full.name})")
		List<SchemaEntry> propertyPlaceholderParameters();

		@Query(base = "dc=:dc", value = "(cn=:fullName)")
		List<SchemaEntry> baseNamedParameters(String fullName, String dc);
	}
}
