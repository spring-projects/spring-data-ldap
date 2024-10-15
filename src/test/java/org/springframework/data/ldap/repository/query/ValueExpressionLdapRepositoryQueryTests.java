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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.ldap.config.EmbeddedLdapProperties;
import org.springframework.data.ldap.config.InMemoryLdapConfiguration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 */
@SpringJUnitConfig
@ContextConfiguration
@TestPropertySource(properties = "full.name=John Doe")
class ValueExpressionLdapRepositoryQueryTests {

	@Autowired private QueryRepository queryRepository;

	@ParameterizedTest
	@EnumSource(QueryType.class)
	void should_work_with_value_expression(QueryType queryType) {
		List<SchemaEntry> objects = queryType.run(queryRepository);

		assertThat(objects).hasSize(1);
		assertThat(objects.get(0).fullName).isEqualTo("John Doe");
		assertThat(objects.get(0).lastName).isEqualTo("Doe");
	}

	interface RunnableTestCase {
		List<SchemaEntry> run(QueryRepository queryRepository);
	}

	enum QueryType implements RunnableTestCase {
		NAMED_QUERY {
			@Override
			public List<SchemaEntry> run(QueryRepository queryRepository) {
				return queryRepository.namedParameters("John Doe", "Bar");
			}
		},
		INDEXED_PARAMS_QUERY {
			@Override
			public List<SchemaEntry> run(QueryRepository queryRepository) {
				return queryRepository.indexedParameters("John Doe", "Bar");
			}
		},
		SPEL_QUERY {
			@Override
			public List<SchemaEntry> run(QueryRepository queryRepository) {
				return queryRepository.spelParameters();
			}
		},
		PROPERTY_PLACEHOLDER_QUERY {
			@Override
			public List<SchemaEntry> run(QueryRepository queryRepository) {
				return queryRepository.propertyPlaceholderParameters();
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Import(InMemoryLdapConfiguration.class)
	@EnableLdapRepositories
	static class TestConfig {

		@Bean
		EmbeddedLdapProperties embeddedLdapProperties() {
			EmbeddedLdapProperties embeddedLdapProperties = new EmbeddedLdapProperties();
			embeddedLdapProperties.setBaseDn(Arrays.asList("dc=com", "dc=memorynotfound"));
			return embeddedLdapProperties;
		}
	}
}
