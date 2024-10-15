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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.ldap.core.mapping.LdapMappingContext;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.query.LdapQuery;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedLdapRepositoryQueryTests {

	LdapOperations ldapOperations = Mockito.mock();

	ValueExpressionDelegate valueExpressionDelegate = ValueExpressionDelegate.create();

	@Test
	void shouldEncodeQuery() throws NoSuchMethodException {
		LdapQueryMethod method = queryMethod("namedParameters");
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(
				new LdapParametersParameterAccessor(method, new Object[] { "John)(cn=Doe)", "foo" }));

		assertThat(ldapQuery.filter().encode()).isEqualTo("(cn=John\\29\\28cn=Doe\\29)");
	}

	@Test
	void shouldEncodeBase() throws NoSuchMethodException {
		LdapQueryMethod method = queryMethod("baseNamedParameters");
		AnnotatedLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(
				new LdapParametersParameterAccessor(method, new Object[] { "foo", "cn=John)" }));

		assertThat(ldapQuery.base()).hasToString("cn=John\\29");
	}

	private LdapQueryMethod queryMethod(String methodName) throws NoSuchMethodException {
		return new LdapQueryMethod(QueryRepository.class.getMethod(methodName, String.class, String.class),
				new DefaultRepositoryMetadata(QueryRepository.class), new SpelAwareProxyProjectionFactory());
	}

	private AnnotatedLdapRepositoryQuery repositoryQuery(LdapQueryMethod method) {
		return new AnnotatedLdapRepositoryQuery(method, SchemaEntry.class, ldapOperations, new LdapMappingContext(),
				new EntityInstantiators(), valueExpressionDelegate);
	}

	interface QueryRepository extends LdapRepository<SchemaEntry> {

		@Query(value = "(cn=:fullName)")
		List<SchemaEntry> namedParameters(@Param("fullName") String fullName, @Param("lastName") String lastName);

		@Query(base = ":dc", value = "(cn=:fullName)")
		List<SchemaEntry> baseNamedParameters(@Param("fullName") String fullName, @Param("dc") String dc);

	}
}