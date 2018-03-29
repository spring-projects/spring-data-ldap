/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.ldap.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.filter.AbsoluteTrueFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;

/**
 * Unit tests for {@link QueryDslLdapQuery}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryDslLdapQueryUnitTests {

	@Mock LdapOperations ldapOperations;

	@Before
	public void before() {
		when(ldapOperations.getObjectDirectoryMapper()).thenReturn(new DefaultObjectDirectoryMapper());
	}

	@Test // DATALDAP-65
	public void shouldCreateFilter() {

		QueryDslLdapQuery<UnitTestPerson> query = new QueryDslLdapQuery<UnitTestPerson>(ldapOperations,
				UnitTestPerson.class);

		LdapQuery ldapQuery = query.where(QPerson.person.fullName.eq("foo")).buildQuery();

		assertThat(ldapQuery.filter()).isInstanceOf(EqualsFilter.class);
	}

	@Test // DATALDAP-65
	public void shouldCreateEmptyFilter() {

		QueryDslLdapQuery<UnitTestPerson> query = new QueryDslLdapQuery<UnitTestPerson>(ldapOperations,
				UnitTestPerson.class);

		LdapQuery ldapQuery = query.buildQuery();

		assertThat(ldapQuery.filter()).isInstanceOf(AbsoluteTrueFilter.class);
	}

	@Test // DATALDAP-65
	public void shouldCreateEmptyFilterFromWhereNull() {

		QueryDslLdapQuery<UnitTestPerson> query = new QueryDslLdapQuery<UnitTestPerson>(ldapOperations, QPerson.person);

		LdapQuery ldapQuery = query.where(null).buildQuery();

		assertThat(ldapQuery.filter()).isInstanceOf(AbsoluteTrueFilter.class);
	}
}
