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
import javax.naming.directory.SearchControls;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Slice;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.util.Optionals;
import org.springframework.ldap.NameNotFoundException;
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
	private static final SearchControls searchControls = createSearchControls();

	private final LdapOperations ldapOperations;
	private final ObjectDirectoryMapper odm;
	private final Class<T> entityType;
	private final ContextMapper<T> contextMapper;

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
		this.contextMapper = ctx -> odm.mapFromLdapDataEntry((DirContextOperations) ctx, entityType);
	}

	private static SearchControls createSearchControls() {

		SearchControls searchControls = new SearchControls();

		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchControls.setReturningObjFlag(true);
		searchControls.setReturningAttributes(ALL_ATTRIBUTES);

		return searchControls;
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
	public Slice<T> findAll(Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null!");

		PagedResultsDirContextProcessor pagedContextProcessor = LdapPaging.createPagedContext(pageable);
		Filter filter = odm.filterFor(entityType, null);

		List<T> content = ldapOperations.search(getBaseAnnotationFromEntityType(), filter.encode(), searchControls,
				contextMapper, pagedContextProcessor);

		return LdapPaging.createSlice(content, pageable, pagedContextProcessor);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.ldap.repository.LdapRepository#findAll(org.springframework.ldap.query.LdapQuery, org.springframework.data.domain.Pageable)
	 */
	@Override
	public Slice<T> findAll(LdapQuery ldapQuery, Pageable pageable) {

		Assert.notNull(ldapQuery, "LdapQuery must not be null");
		Assert.notNull(pageable, "Pageable must not be null!");

		PagedResultsDirContextProcessor pagedContextProcessor = LdapPaging.createPagedContext(pageable);

		List<T> content = ldapOperations.search(ldapQuery.base(), ldapQuery.filter().encode(), searchControls,
				contextMapper, pagedContextProcessor);

		return LdapPaging.createSlice(content, pageable, pagedContextProcessor);
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

	private Name getBaseAnnotationFromEntityType() {

		Entry entryAnnotation = entityType.getAnnotation(Entry.class);
		return (entryAnnotation == null) ? LdapUtils.emptyLdapName() : LdapUtils.newLdapName(entryAnnotation.base());
	}
}
