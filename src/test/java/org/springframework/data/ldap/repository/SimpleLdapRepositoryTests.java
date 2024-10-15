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
package org.springframework.data.ldap.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import org.springframework.data.domain.Persistable;
import org.springframework.data.ldap.repository.support.SimpleLdapRepository;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.support.CountNameClassPairCallbackHandler;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.support.LdapUtils;

/**
 * Unit tests for {@link SimpleLdapRepository}.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 * @author Jens Schauder
 */
@MockitoSettings
class SimpleLdapRepositoryTests {

	@Mock LdapOperations ldapOperationsMock;
	@Mock ObjectDirectoryMapper odmMock;

	private SimpleLdapRepository<Object> tested;

	@BeforeEach
	void prepareTestedInstance() {
		tested = new SimpleLdapRepository<>(ldapOperationsMock, odmMock, Object.class);
	}

	@Test
	void testCount() {

		Filter filterMock = mock(Filter.class);
		when(odmMock.filterFor(Object.class, null)).thenReturn(filterMock);
		ArgumentCaptor<LdapQuery> ldapQuery = ArgumentCaptor.forClass(LdapQuery.class);
		doNothing().when(ldapOperationsMock).search(ldapQuery.capture(), any(CountNameClassPairCallbackHandler.class));

		long count = tested.count();

		assertThat(count).isEqualTo(0);
		LdapQuery query = ldapQuery.getValue();
		assertThat(query.filter()).isEqualTo(filterMock);
		assertThat(query.attributes()).isEqualTo(new String[] { "objectclass" });
	}

	@Test
	void testSaveNonPersistableWithIdSet() {

		Object expectedEntity = new Object();

		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	void testSaveNonPersistableWithIdChanged() {

		Object expectedEntity = new Object();
		LdapName expectedName = LdapUtils.newLdapName("ou=newlocation");

		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	void testSaveNonPersistableWithNoIdCalculatedId() {

		Object expectedEntity = new Object();
		LdapName expectedName = LdapUtils.emptyLdapName();

		when(odmMock.getId(expectedEntity)).thenReturn(null);

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	void testSavePersistableNewWithDeclaredId() {

		Persistable expectedEntity = mock(Persistable.class);

		when(expectedEntity.isNew()).thenReturn(true);
		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	void testSavePersistableNewWithCalculatedId() {

		Persistable expectedEntity = mock(Persistable.class);
		LdapName expectedName = LdapUtils.emptyLdapName();

		when(expectedEntity.isNew()).thenReturn(true);
		when(odmMock.getId(expectedEntity)).thenReturn(null);

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	void testSavePersistableNotNew() {

		Persistable expectedEntity = mock(Persistable.class);

		when(expectedEntity.isNew()).thenReturn(false);
		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	void testFindOneWithName() {

		LdapName expectedName = LdapUtils.emptyLdapName();
		Object expectedResult = new Object();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenReturn(expectedResult);

		Optional<Object> actualResult = tested.findById(expectedName);

		assertThat(actualResult).contains(expectedResult);
	}

	@Test // DATALDAP-21
	void verifyThatNameNotFoundInFindOneWithNameReturnsEmptyOptional() {

		LdapName expectedName = LdapUtils.emptyLdapName();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenThrow(new NameNotFoundException(""));

		Optional<Object> actualResult = tested.findById(expectedName);

		assertThat(actualResult).isNotPresent();
	}

	@Test // DATALDAP-21
	void verifyThatNoResultFoundInFindOneWithNameReturnsEmptyOptional() {

		LdapName expectedName = LdapUtils.emptyLdapName();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenReturn(null);

		Optional<Object> actualResult = tested.findById(expectedName);

		assertThat(actualResult).isNotPresent();
	}

	@Test
	void testFindAll() {

		Name expectedName1 = LdapUtils.newLdapName("ou=aa");
		Name expectedName2 = LdapUtils.newLdapName("ou=bb");

		Object expectedResult1 = new Object();
		Object expectedResult2 = new Object();

		when(ldapOperationsMock.findByDn(expectedName1, Object.class)).thenReturn(expectedResult1);
		when(ldapOperationsMock.findByDn(expectedName2, Object.class)).thenReturn(expectedResult2);

		Iterable<Object> actualResult = tested.findAllById(Arrays.asList(expectedName1, expectedName2));

		Iterator<Object> iterator = actualResult.iterator();
		assertThat(iterator.next()).isSameAs(expectedResult1);
		assertThat(iterator.next()).isSameAs(expectedResult2);

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void testFindAllWhereOneEntryIsNotFound() {

		Name expectedName1 = LdapUtils.newLdapName("ou=aa");
		Name expectedName2 = LdapUtils.newLdapName("ou=bb");

		Object expectedResult2 = new Object();

		when(ldapOperationsMock.findByDn(expectedName1, Object.class)).thenReturn(null);
		when(ldapOperationsMock.findByDn(expectedName2, Object.class)).thenReturn(expectedResult2);

		Iterable<Object> actualResult = tested.findAllById(Arrays.asList(expectedName1, expectedName2));

		Iterator<Object> iterator = actualResult.iterator();
		assertThat(iterator.next()).isSameAs(expectedResult2);

		assertThat(iterator.hasNext()).isFalse();
	}
}
