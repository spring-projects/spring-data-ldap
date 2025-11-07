/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.domain.Limit;
import org.springframework.data.ldap.core.mapping.LdapMappingContext;
import org.springframework.data.ldap.repository.LdapEncode;
import org.springframework.data.ldap.repository.LdapEncoder;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.query.LdapQuery;

/**
 * Unit tests for {@link AnnotatedLdapRepositoryQuery}
 *
 * @author Marcin Grzejszczak
 * @author Mark Paluch
 */
class AnnotatedLdapRepositoryQueryUnitTests {

	LdapOperations ldapOperations = Mockito.mock();

	ValueExpressionDelegate valueExpressionDelegate = ValueExpressionDelegate.create();

	@Test
	void shouldEncodeQuery() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("namedParameters", String.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(
				new LdapParametersParameterAccessor(method, new Object[] { "John)(cn=Doe)" }));

		assertThat(ldapQuery.filter().encode()).isEqualTo("(cn=John\\29\\28cn=Doe\\29)");
	}

	@Test
	void messageFormatParametersShouldWork() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("messageFormatParameters", String.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(new LdapParametersParameterAccessor(method, new Object[] { "John" }));

		assertThat(ldapQuery.filter().encode()).isEqualTo("(cn=John)");
	}

	@Test
	void shouldEncodeBase() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("baseNamedParameters", String.class, String.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(
				new LdapParametersParameterAccessor(method, new Object[] { "foo", "cn=John)" }));

		assertThat(ldapQuery.base()).hasToString("cn=John\\29");
	}

	@Test
	void shouldEncodeWithCustomEncoder() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("customEncoder", String.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(
				new LdapParametersParameterAccessor(method, new Object[] { "Doe" }));

		assertThat(ldapQuery.filter().encode()).isEqualTo("(cn=Doebar)");
	}

	@Test // GH-586
	void shouldConsiderLimit() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("limited", String.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(new LdapParametersParameterAccessor(method, new Object[] { "John Doe" }));

		assertThat(ldapQuery.countLimit()).isEqualTo(123);
	}

	@Test // GH-586
	void shouldConsiderLimitParameter() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("limited", String.class, Limit.class);
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query
				.createQuery(new LdapParametersParameterAccessor(method, new Object[] { "John Doe", Limit.unlimited() }));

		assertThat(ldapQuery.countLimit()).isEqualTo(0);

		ldapQuery = query
				.createQuery(new LdapParametersParameterAccessor(method, new Object[] { "John Doe", Limit.of(10) }));

		assertThat(ldapQuery.countLimit()).isEqualTo(10);
	}

	private LdapQueryMethod queryMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		return new LdapQueryMethod(QueryRepository.class.getMethod(methodName, parameterTypes),
				new DefaultRepositoryMetadata(QueryRepository.class), new SpelAwareProxyProjectionFactory());
	}

	private AnnotatedLdapRepositoryQuery repositoryQuery(LdapQueryMethod method) {
		return new AnnotatedLdapRepositoryQuery(method, SchemaEntry.class, ldapOperations, new LdapMappingContext(),
				new EntityInstantiators(), valueExpressionDelegate);
	}

	interface QueryRepository extends LdapRepository<SchemaEntry> {

		@Query(value = "(cn=:fullName)")
		List<SchemaEntry> namedParameters(String fullName);

		@Query(value = "(cn=:fullName)", countLimit = 123)
		List<SchemaEntry> limited(String fullName);

		@Query(value = "(cn=:fullName)", countLimit = 123)
		List<SchemaEntry> limited(String fullName, Limit limit);

		@Query(value = "(cn={0})")
		List<SchemaEntry> messageFormatParameters(String fullName);

		@Query(base = ":dc", value = "(cn=:fullName)")
		List<SchemaEntry> baseNamedParameters(String fullName, String dc);

		@Query(value = "(cn=:fullName)")
		List<SchemaEntry> customEncoder(@LdapEncode(MyEncoder.class) String fullName);

	}

	static class MyEncoder implements LdapEncoder {

		@Override
		public String encode(String value) {
			return value + "bar";
		}
	}
}
