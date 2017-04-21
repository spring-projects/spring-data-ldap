/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.ldap.repository.query;

import static org.mockito.Mockito.*;

import lombok.Data;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.ldap.repository.query.LdapQueryExecution.CollectionExecution;
import org.springframework.data.ldap.repository.query.LdapQueryExecution.SingleEntityExecution;
import org.springframework.data.ldap.repository.query.LdapQueryExecution.SlicedExecution;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;

/**
 * Unit tests for {@link LdapQueryExecution}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapQueryExecutionUnitTests {

	@Mock LdapOperations ldapOperations;
	@Mock LdapQuery ldapQuery;
	@Mock PagedResultsDirContextProcessor dirContextProcessor;

	@Before
	public void before() {
		when(ldapOperations.getObjectDirectoryMapper()).thenReturn(new DefaultObjectDirectoryMapper());
	}

	@Test // DATALDAP-30
	public void shouldExecuteCollectionQuery() throws Exception {

		CollectionExecution execution = new CollectionExecution(ldapOperations);

		execution.execute(ldapQuery, Person.class);

		verify(ldapOperations).find(ldapQuery, Person.class);
	}

	@Test // DATALDAP-30
	public void shouldExecuteFindOneQuery() {

		SingleEntityExecution execution = new SingleEntityExecution(ldapOperations);

		execution.execute(ldapQuery, Person.class);

		verify(ldapOperations).findOne(ldapQuery, Person.class);
	}

	@Test // DATALDAP-30
	@SuppressWarnings("unchecked")
	public void shouldExecuteSliceQuery() {

		SlicedExecution execution = new SlicedExecution(ldapOperations, PageRequest.of(0, 5), ctx -> null);

		execution.execute(ldapQuery, Person.class);

		verify(ldapOperations).search(any(Name.class), anyString(), any(SearchControls.class), any(ContextMapper.class),
				any(PagedResultsDirContextProcessor.class));
	}

	@Entry(objectClasses = "person", base = "o=pivotal")
	@Data
	static class Person {

		@Id Name id;
	}
}
