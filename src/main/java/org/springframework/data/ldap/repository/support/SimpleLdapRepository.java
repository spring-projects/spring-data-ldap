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
package org.springframework.data.ldap.repository.support;

import static org.springframework.ldap.query.LdapQueryBuilder.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Sort;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.util.Optionals;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.support.CountNameClassPairCallbackHandler;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.util.Assert;

/**
 * Base repository implementation for LDAP.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 * @author Houcheng Lin
 */
public class SimpleLdapRepository<T> implements LdapRepository<T> {

	private static final String OBJECTCLASS_ATTRIBUTE = "objectclass";
	private static final String[] ALL_ATTRIBUTES = null;
	private static final int DEFAULT_PAGE_SIZE = 25;

	private final LdapOperations ldapOperations;
	private final ObjectDirectoryMapper odm;
	private final Class<T> entityType;

	/**
	 * Creates a new {@link SimpleLdapRepository}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param odm must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 */
	public SimpleLdapRepository(LdapOperations ldapOperations, ObjectDirectoryMapper odm, Class<T> entityType) {

		Assert.notNull(ldapOperations, "LdapOperations must not be null!");
		Assert.notNull(odm, "ObjectDirectoryMapper must not be null!");
		Assert.notNull(entityType, "Entity type must not be null!");

		this.ldapOperations = ldapOperations;
		this.odm = odm;
		this.entityType = entityType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#count()
	 */
	@Override
	public long count() {

		Filter filter = odm.filterFor(entityType, null);
		CountNameClassPairCallbackHandler callback = new CountNameClassPairCallbackHandler();
		LdapQuery query = query().attributes(OBJECTCLASS_ATTRIBUTE).filter(filter);
		ldapOperations.search(query, callback);

		return callback.getNoOfRows();
	}

	private <S extends T> boolean isNew(S entity, Name id) {

		if (entity instanceof Persistable) {
			Persistable<?> persistable = (Persistable<?>) entity;
			return persistable.isNew();
		} else {
			return id == null;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#save(java.lang.Object)
	 */
	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be null");

		Name declaredId = odm.getId(entity);

		if (isNew(entity, declaredId)) {
			ldapOperations.create(entity);
		} else {
			ldapOperations.update(entity);
		}

		return entity;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#save(java.lang.Iterable)
	 */
	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {

		return StreamSupport.stream(entities.spliterator(), false) //
				.map(this::save) //
				.collect(Collectors.toList());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
	 */
	@Override
	public Optional<T> findOne(Name name) {

		Assert.notNull(name, "Id must not be null");

		try {
			return Optional.ofNullable(ldapOperations.findByDn(name, entityType));
		} catch (NameNotFoundException e) {
			return Optional.empty();
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.ldap.repository.LdapRepository#findAll(org.springframework.ldap.query.LdapQuery)
	 */
	@Override
	public List<T> findAll(LdapQuery ldapQuery) {

		Assert.notNull(ldapQuery, "LdapQuery must not be null");
		return ldapOperations.find(ldapQuery, entityType);
	}

	/* (non-Javadoc)
     * @see org.springframework.data.ldap.repository.LdapRepository#findAll(org.springframework.data.domain.Pageable)
     */
	@Override
	public Page<T> findAll(Pageable pageable) {

		PagedResultsDirContextProcessor pagedContextProcessor = createPagedContext(pageable);
		ContextMapper<T> mapper = createContextMapper();
		SearchControls searchControls = createSearchControls();
		Filter filter = odm.filterFor(entityType, null);

		List<T> content = ldapOperations.search(getBaseAnnotationFromEntityType(), filter.encode(),
				searchControls, mapper, pagedContextProcessor);

		return createLdapPage(pageable, pagedContextProcessor, content);
	}

	/* (non-Javadoc)
     * @see org.springframework.data.ldap.repository.LdapRepository#findAll(org.springframework.ldap.query.LdapQuery, org.springframework.data.domain.Pageable)
     */
	@Override
	public Page<T> findAll(LdapQuery ldapQuery, Pageable pageable) {

		PagedResultsDirContextProcessor pagedContextProcessor = createPagedContext(pageable);
		ContextMapper<T> mapper = createContextMapper();
		SearchControls searchControls = createSearchControls();

		List<T> content =  ldapOperations.search(ldapQuery.base(), ldapQuery.filter().encode(), searchControls,
				mapper, pagedContextProcessor);

		return createLdapPage(pageable, pagedContextProcessor, content);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.ldap.repository.LdapRepository#findOne(org.springframework.ldap.query.LdapQuery)
	 */
	@Override
	public Optional<T> findOne(LdapQuery ldapQuery) {

		Assert.notNull(ldapQuery, "LdapQuery must not be null");

		try {
			return Optional.ofNullable(ldapOperations.findOne(ldapQuery, entityType));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#exists(java.io.Serializable)
	 */
	@Override
	public boolean exists(Name name) {

		Assert.notNull(name, "Id must not be null");

		return findOne(name) != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll()
	 */
	@Override
	public List<T> findAll() {
		return ldapOperations.findAll(entityType);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
	 */
	@Override
	public List<T> findAll(final Iterable<Name> names) {

		return StreamSupport.stream(names.spliterator(), false) //
				.map(this::findOne) //
				.flatMap(Optionals::toStream) //
				.collect(Collectors.toList());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.io.Serializable)
	 */
	@Override
	public void delete(Name name) {

		Assert.notNull(name, "Id must not be null");

		ldapOperations.unbind(name);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Object)
	 */
	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Entity must not be null");

		ldapOperations.delete(entity);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<? extends T> entities) {
		entities.forEach(this::delete);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#deleteAll()
	 */
	@Override
	public void deleteAll() {
		delete(findAll());
	}


	private PagedResultsDirContextProcessor createPagedContext(Pageable pageable) {

		PagedResultsCookie pagedResultCookie = null;
		if(pageable instanceof SimpleLdapRepository.LdapPageRequest) {
			pagedResultCookie = ((LdapPageRequest) pageable).getCookie();
		}

		int pageSize = (pageable == null) ? DEFAULT_PAGE_SIZE : pageable.getPageSize();

		return new PagedResultsDirContextProcessor(pageSize,
				pagedResultCookie);
	}

	private Page<T> createLdapPage(Pageable pageable, PagedResultsDirContextProcessor pagedContextProcessor, List<T> content) {

		LdapPageImpl ldapPageImpl =  new LdapPageImpl(content, pageable, content.size());
		ldapPageImpl.setContextProcessorForNextPageable(pagedContextProcessor);
		return ldapPageImpl;
	}

	private Name getBaseAnnotationFromEntityType() {

		Entry entryAnnotation = entityType.getAnnotation(Entry.class);
		return (entryAnnotation == null) ? LdapUtils.emptyLdapName() : LdapUtils.newLdapName(entryAnnotation.base());
	}

	private ContextMapper<T> createContextMapper() {

		return new ContextMapper<T> () {
			public T mapFromContext(Object ctx) throws NamingException {
				return odm.mapFromLdapDataEntry((DirContextOperations)ctx, entityType);
			}
		};
	}

	private SearchControls createSearchControls() {

		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchControls.setReturningObjFlag(true);
		searchControls.setReturningAttributes(ALL_ATTRIBUTES);
		return searchControls;
	}

	static class LdapPageImpl<T> extends PageImpl<T> {

		private final Pageable pageable;
		private PagedResultsCookie cookieForNextPageable;
		private boolean hasMore;

		LdapPageImpl(List<T> content, Pageable pageable, int total) {

			super(content, pageable, total);
			this.pageable = pageable;
		}

		void setContextProcessorForNextPageable(PagedResultsDirContextProcessor contextProcessor) {

			this.cookieForNextPageable = contextProcessor.getCookie();
			this.hasMore = contextProcessor.hasMore();
		}

		@Override
		public Pageable nextPageable() {

			if (hasMore) {
				LdapPageRequest nextPageable = LdapPageRequest.newInstance(pageable.getPageNumber() + 1, pageable.getPageSize());
				nextPageable.setCookie(cookieForNextPageable);
				return nextPageable;
			}

			return null;
		}

		@Override
		public boolean hasNext() {
			return hasMore;
		}
	}

	static class LdapPageRequest extends AbstractPageRequest {
		private PagedResultsCookie cookie;

		private LdapPageRequest(int page, int size){
			super(page, size);
		}

		static LdapPageRequest newInstance(int page, int size) {
			return new LdapPageRequest(page, size);
		}

		void setCookie(PagedResultsCookie cookie) {
			this.cookie = cookie;
		}

		PagedResultsCookie getCookie() {
			return cookie;
		}

		@Override
		public Sort getSort() {
			return Sort.unsorted();
		}

		@Override
		public Pageable next() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Pageable previous() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Pageable first() {
			throw new UnsupportedOperationException();
		}
	}
}
