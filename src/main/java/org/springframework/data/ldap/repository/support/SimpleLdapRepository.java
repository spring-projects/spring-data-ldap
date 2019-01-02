/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.naming.Name;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Persistable;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.support.CountNameClassPairCallbackHandler;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.util.Assert;

/**
 * Base repository implementation for LDAP.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 */
public class SimpleLdapRepository<T> implements LdapRepository<T> {

	private static final String OBJECTCLASS_ATTRIBUTE = "objectclass";

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

	protected LdapOperations getLdapOperations() {
		return ldapOperations;
	}

	protected Class<T> getEntityType() {
		return entityType;
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

		return new TransformingIterable<S, S>(entities, new Function<S, S>() {
			@Override
			public S transform(S entry) {
				return save(entry);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
	 */
	@Override
	public T findOne(Name name) {

		Assert.notNull(name, "Id must not be null");

		try {
			return ldapOperations.findByDn(name, entityType);
		} catch (NameNotFoundException e) {
			return null;
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
	 * @see org.springframework.data.ldap.repository.LdapRepository#findOne(org.springframework.ldap.query.LdapQuery)
	 */
	@Override
	public T findOne(LdapQuery ldapQuery) {

		Assert.notNull(ldapQuery, "LdapQuery must not be null");

		try {
			return ldapOperations.findOne(ldapQuery, entityType);
		} catch (EmptyResultDataAccessException e) {
			return null;
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

		Iterable<T> found = new TransformingIterable<Name, T>(names, new Function<Name, T>() {
			@Override
			public T transform(Name name) {
				return findOne(name);
			}
		});

		LinkedList<T> list = new LinkedList<T>();
		for (T entry : found) {
			if (entry != null) {
				list.add(entry);
			}
		}

		return list;
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

		for (T entity : entities) {
			delete(entity);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#deleteAll()
	 */
	@Override
	public void deleteAll() {
		delete(findAll());
	}

	private static final class TransformingIterable<F, T> implements Iterable<T> {

		private final Iterable<F> target;
		private final Function<F, T> function;

		private TransformingIterable(Iterable<F> target, Function<F, T> function) {
			this.target = target;
			this.function = function;
		}

		@Override
		public Iterator<T> iterator() {

			final Iterator<F> targetIterator = target.iterator();
			return new Iterator<T>() {

				@Override
				public boolean hasNext() {
					return targetIterator.hasNext();
				}

				@Override
				public T next() {
					return function.transform(targetIterator.next());
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException("Remove is not supported for this iterator");
				}
			};
		}
	}

	private interface Function<F, T> {
		T transform(F entry);
	}
}
