/*
 * Copyright 2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.domain.Limit;
import org.springframework.data.ldap.core.mapping.LdapMappingContext;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;

/**
 * Unit tests for {@link PartTreeLdapRepositoryQuery}.
 *
 * @author Mark Paluch
 */
class PartTreeLdapRepositoryQueryUnitTests {

	LdapOperations ldapOperations = Mockito.mock();

	@BeforeEach
	void setUp() {
		when(ldapOperations.getObjectDirectoryMapper()).thenReturn(new DefaultObjectDirectoryMapper());
	}

	@Test // GH-586
	void shouldConsiderLimit() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("findTop5ByFullName", String.class);
		PartTreeLdapRepositoryQuery query = repositoryQuery(method);

		LdapQuery ldapQuery = query.createQuery(new LdapParametersParameterAccessor(method, new Object[] { "John Doe" }));

		assertThat(ldapQuery.countLimit()).isEqualTo(5);
	}

	@Test // GH-586
	void shouldConsiderLimitParameter() throws NoSuchMethodException {

		LdapQueryMethod method = queryMethod("findTop5ByFullName", String.class, Limit.class);
		PartTreeLdapRepositoryQuery query = repositoryQuery(method);

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

	private PartTreeLdapRepositoryQuery repositoryQuery(LdapQueryMethod method) {
		return new PartTreeLdapRepositoryQuery(method, SchemaEntry.class, ldapOperations, new LdapMappingContext(),
				new EntityInstantiators());
	}

	interface QueryRepository extends LdapRepository<SchemaEntry> {

		List<SchemaEntry> findTop5ByFullName(String fullName);

		List<SchemaEntry> findTop5ByFullName(String fullName, Limit limit);

	}

}
