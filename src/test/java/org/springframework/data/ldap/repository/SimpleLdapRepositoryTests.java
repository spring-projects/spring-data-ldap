/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.ldap.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.naming.Name;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsResponseControl;

import com.sun.jndi.ldap.BerEncoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Persistable;
import org.springframework.data.ldap.repository.support.SimpleLdapRepository;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.support.CountNameClassPairCallbackHandler;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapUtils;

/**
 * Unit tests for {@link SimpleLdapRepository}.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleLdapRepositoryTests {

	@Mock LdapOperations ldapOperationsMock;
	@Mock ObjectDirectoryMapper odmMock;
	@Mock LdapContext contextMock;

	SimpleLdapRepository<Object> tested;

	@Before
	public void prepareTestedInstance() {
		tested = new SimpleLdapRepository<>(ldapOperationsMock, odmMock, Object.class);
	}

	@Test
	public void testCount() {

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
	public void testSaveNonPersistableWithIdSet() {

		Object expectedEntity = new Object();

		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	public void testSaveNonPersistableWithIdChanged() {

		Object expectedEntity = new Object();
		LdapName expectedName = LdapUtils.newLdapName("ou=newlocation");

		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	public void testSaveNonPersistableWithNoIdCalculatedId() {

		Object expectedEntity = new Object();
		LdapName expectedName = LdapUtils.emptyLdapName();

		when(odmMock.getId(expectedEntity)).thenReturn(null);

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	public void testSavePersistableNewWithDeclaredId() {

		Persistable expectedEntity = mock(Persistable.class);

		when(expectedEntity.isNew()).thenReturn(true);
		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	public void testSavePersistableNewWithCalculatedId() {

		Persistable expectedEntity = mock(Persistable.class);
		LdapName expectedName = LdapUtils.emptyLdapName();

		when(expectedEntity.isNew()).thenReturn(true);
		when(odmMock.getId(expectedEntity)).thenReturn(null);

		tested.save(expectedEntity);

		verify(ldapOperationsMock).create(expectedEntity);
	}

	@Test
	public void testSavePersistableNotNew() {

		Persistable expectedEntity = mock(Persistable.class);

		when(expectedEntity.isNew()).thenReturn(false);
		when(odmMock.getId(expectedEntity)).thenReturn(LdapUtils.emptyLdapName());

		tested.save(expectedEntity);

		verify(ldapOperationsMock).update(expectedEntity);
	}

	@Test
	public void testFindOneWithName() {

		LdapName expectedName = LdapUtils.emptyLdapName();
		Object expectedResult = new Object();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenReturn(expectedResult);

		Optional<Object> actualResult = tested.findOne(expectedName);

		assertThat(actualResult).contains(expectedResult);
	}

	@Test // DATALDAP-21
	public void verifyThatNameNotFoundInFindOneWithNameReturnsEmptyOptional() {

		LdapName expectedName = LdapUtils.emptyLdapName();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenThrow(new NameNotFoundException(""));

		Optional<Object> actualResult = tested.findOne(expectedName);

		assertThat(actualResult).isNotPresent();
	}

	@Test // DATALDAP-21
	public void verifyThatNoResultFoundInFindOneWithNameReturnsEmptyOptional() {

		LdapName expectedName = LdapUtils.emptyLdapName();

		when(ldapOperationsMock.findByDn(expectedName, Object.class)).thenReturn(null);

		Optional<Object> actualResult = tested.findOne(expectedName);

		assertThat(actualResult).isNotPresent();
	}

	@Test
	public void testFindAll() {

		Name expectedName1 = LdapUtils.newLdapName("ou=aa");
		Name expectedName2 = LdapUtils.newLdapName("ou=bb");

		Object expectedResult1 = new Object();
		Object expectedResult2 = new Object();

		when(ldapOperationsMock.findByDn(expectedName1, Object.class)).thenReturn(expectedResult1);
		when(ldapOperationsMock.findByDn(expectedName2, Object.class)).thenReturn(expectedResult2);

		Iterable<Object> actualResult = tested.findAll(Arrays.asList(expectedName1, expectedName2));

		Iterator<Object> iterator = actualResult.iterator();
		assertThat(iterator.next()).isSameAs(expectedResult1);
		assertThat(iterator.next()).isSameAs(expectedResult2);

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void testFindAllWhereOneEntryIsNotFound() {

		Name expectedName1 = LdapUtils.newLdapName("ou=aa");
		Name expectedName2 = LdapUtils.newLdapName("ou=bb");

		Object expectedResult2 = new Object();

		when(ldapOperationsMock.findByDn(expectedName1, Object.class)).thenReturn(null);
		when(ldapOperationsMock.findByDn(expectedName2, Object.class)).thenReturn(expectedResult2);

		Iterable<Object> actualResult = tested.findAll(Arrays.asList(expectedName1, expectedName2));

		Iterator<Object> iterator = actualResult.iterator();
		assertThat(iterator.next()).isSameAs(expectedResult2);

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void testFindAllWithOnePage() {

		String cookieForLastPage = "";
		Object expectedResult1 = new Object();
		Object expectedResult2 = new Object();

		setupMocksForLdapPagedSearchResult(Arrays.asList(expectedResult1, expectedResult2), cookieForLastPage);

		Page<Object> pagedResult = tested.findAll(PageRequest.of(0, 2));

		assertThat(pagedResult).containsExactly(expectedResult1, expectedResult2);
		assertThat(pagedResult.getSize()).isEqualTo(2);
		assertThat(pagedResult.hasNext()).isFalse();
	}


	@Test
	public void testFindAllByLdapQueryWithOnePage() {

		String cookieForLastPage = "";
		Object expectedResult1 = new Object();
		Object expectedResult2 = new Object();

		setupMocksForLdapPagedSearchResult(Arrays.asList(expectedResult1, expectedResult2), cookieForLastPage);
		LdapQuery query = LdapQueryBuilder.query().base("").filter("");

		Page<Object> pagedResult = tested.findAll(query, PageRequest.of(0, 2));

		assertThat(pagedResult).containsExactly(expectedResult1, expectedResult2);
		assertThat(pagedResult.getSize()).isEqualTo(2);
		assertThat(pagedResult.hasNext()).isFalse();
	}


	@Test
	public void testFindAllWithTwoPages() {

		String cookieForLastPage = "";
		String cookieForMorePages = "more-pages-remaining";
		Object expectedResult1 = new Object();
		Object expectedResult2 = new Object();
		Object expectedResult3 = new Object();

		setupMocksForLdapPagedSearchResult(Arrays.asList(expectedResult1, expectedResult2), cookieForMorePages);

		Page<Object> pagedResult = tested.findAll(PageRequest.of(0, 2));

		assertThat(pagedResult).containsExactly(expectedResult1, expectedResult2);
		assertThat(pagedResult.getNumber()).isEqualTo(0);
		assertThat(pagedResult.getNumberOfElements()).isEqualTo(2);
		assertThat(pagedResult.hasNext()).isTrue();
		assertThat(pagedResult.nextPageable()).isNotNull();

		setupMocksForLdapPagedSearchResult(Arrays.asList(expectedResult3), cookieForLastPage);

		pagedResult = tested.findAll(pagedResult.nextPageable());
		assertThat(pagedResult).containsExactly(expectedResult3);
		assertThat(pagedResult.getNumber()).isEqualTo(1);
		assertThat(pagedResult.getNumberOfElements()).isEqualTo(1);
		assertThat(pagedResult.hasNext()).isFalse();
	}

	private void setupMocksForLdapPagedSearchResult(List<Object> resultList, String cookie) {

		Filter filterMock = mock(Filter.class);
		when(odmMock.filterFor(Object.class, null)).thenReturn(filterMock);

		when(ldapOperationsMock.search(any(Name.class), any(), any(SearchControls.class),
				any(ContextMapper.class), any(DirContextProcessor.class))).thenAnswer(invocation -> {

			DirContextProcessor contextProcessor = (DirContextProcessor) invocation.getArguments()[4];
			if(contextProcessor == null) {
				return null;
			}

			Control[] pagedResponseControls = createPagedResponseControls(resultList.size(), cookie);
			when(contextMock.getResponseControls()).thenReturn(pagedResponseControls);
			contextProcessor.postProcess(contextMock);
			return resultList;
		});
	}

	private Control[] createPagedResponseControls(int size, String cookie) throws IOException {

		String ldapOidString = "1.2.840.113556.1.4.319";

		BerEncoder cookieBerCode = new BerEncoder();
		cookieBerCode.beginSeq(2);
		cookieBerCode.encodeInt(size);
		cookieBerCode.encodeOctetString(cookie.getBytes(), 4);
		cookieBerCode.endSeq();

		Control[] controls = new Control[1];
		controls[0] = new PagedResultsResponseControl(ldapOidString, false, cookieBerCode.getBuf());
		return controls;
	}
}
