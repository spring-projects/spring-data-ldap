/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for LDAP.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 */
public abstract class AbstractLdapRepositoryQuery implements RepositoryQuery {

	private final LdapQueryMethod queryMethod;
	private final Class<?> entityType;
	private final LdapOperations ldapOperations;

	/**
	 * Creates a new {@link AbstractLdapRepositoryQuery} instance given {@link LdapQuery}, {@link Class} and
	 * {@link LdapOperations}.
	 *
	 * @param queryMethod must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param ldapOperations must not be {@literal null}.
	 */
	public AbstractLdapRepositoryQuery(LdapQueryMethod queryMethod, Class<?> entityType, LdapOperations ldapOperations) {

		Assert.notNull(queryMethod, "LdapQueryMethod must not be null!");
		Assert.notNull(entityType, "Entity type must not be null!");
		Assert.notNull(ldapOperations, "LdapOperations must not be null!");

		this.queryMethod = queryMethod;
		this.entityType = entityType;
		this.ldapOperations = ldapOperations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Override
	@SuppressWarnings("ConstantConditions")
	public final Object execute(Object[] parameters) {

		LdapQuery query = createQuery(parameters);

		if (queryMethod.isCollectionQuery()) {
			return ldapOperations.find(query, entityType);
		} else {
			try {
				return ldapOperations.findOne(query, entityType);
			} catch (EmptyResultDataAccessException e) {
				return null;
			}
		}
	}

	/**
	 * Creates a {@link Query} instance using the given {@literal parameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @return
	 */
	protected abstract LdapQuery createQuery(Object[] parameters);

	/**
	 * @return
	 */
	protected Class<?> getEntityClass() {
		return entityType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	@Override
	public final QueryMethod getQueryMethod() {
		return queryMethod;
	}
}
