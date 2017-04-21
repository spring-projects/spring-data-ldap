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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.ldap.repository.support.LdapPaging;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.support.LdapUtils;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractLdapRepositoryQuery} can be executed
 * in various flavors.
 *
 * @author Mark Paluch
 * @since 2.0
 */
interface LdapQueryExecution {

	Object execute(LdapQuery query, Class<?> type);

	/**
	 * {@link LdapQueryExecution} for query returning multiple objects.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class CollectionExecution implements LdapQueryExecution {

		private final @NonNull LdapOperations operations;

		@Override
		public Object execute(LdapQuery query, Class<?> type) {
			return operations.find(query, type);
		}
	}

	/**
	 * {@link LdapQueryExecution} for {@link Slice} query methods.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class SlicedExecution implements LdapQueryExecution {

		private static final String[] ALL_ATTRIBUTES = null;
		private static final SearchControls searchControls = createSearchControls();

		private final @NonNull LdapOperations operations;
		private final @NonNull Pageable pageable;
		private final @NonNull ContextMapper<?> contextMapper;

		private static SearchControls createSearchControls() {

			SearchControls searchControls = new SearchControls();

			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			searchControls.setReturningObjFlag(true);
			searchControls.setReturningAttributes(ALL_ATTRIBUTES);

			return searchControls;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object execute(LdapQuery query, Class<?> type) {

			PagedResultsDirContextProcessor pagedContextProcessor = LdapPaging.createPagedContext(pageable);
			Filter filter = operations.getObjectDirectoryMapper().filterFor(type, null);

			List<?> content = operations.search(getBaseAnnotationFromEntityType(type), filter.encode(), searchControls,
					contextMapper, pagedContextProcessor);

			return LdapPaging.createSlice(content, pageable, pagedContextProcessor);
		}

		private static Name getBaseAnnotationFromEntityType(Class<?> type) {

			Entry entryAnnotation = type.getAnnotation(Entry.class);
			return (entryAnnotation == null) ? LdapUtils.emptyLdapName() : LdapUtils.newLdapName(entryAnnotation.base());
		}
	}

	/**
	 * {@link LdapQueryExecution} to return a single entity.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class SingleEntityExecution implements LdapQueryExecution {

		private final LdapOperations operations;

		@Override
		public Object execute(LdapQuery query, Class<?> type) {
			try {
				return operations.findOne(query, type);
			} catch (EmptyResultDataAccessException e) {
				return null;
			}
		}
	}
}
