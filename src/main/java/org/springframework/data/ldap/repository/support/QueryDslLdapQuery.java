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

import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.util.Assert;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.FilteredClause;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;

/**
 * Spring LDAP specific {@link FilteredClause} implementation.
 *
 * @author Mattias Hellborg Arthursson
 * @author Eddu Melendez
 * @author Mark Paluch
 */
public class QueryDslLdapQuery<K> implements FilteredClause<QueryDslLdapQuery<K>> {

	private final LdapOperations ldapOperations;
	private final Class<? extends K> entityType;
	private final LdapSerializer filterGenerator;

	private QueryMixin<QueryDslLdapQuery<K>> queryMixin = new QueryMixin<>(this,
			new DefaultQueryMetadata().noValidate());

	/**
	 * Creates a new {@link QueryDslLdapQuery}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param entityPath must not be {@literal null}.
	 */
	public QueryDslLdapQuery(LdapOperations ldapOperations, EntityPath<K> entityPath) {
		this(ldapOperations, entityPath.getType());
	}

	/**
	 * Creates a new {@link QueryDslLdapQuery}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 */
	public QueryDslLdapQuery(LdapOperations ldapOperations, Class<? extends K> entityType) {

		Assert.notNull(ldapOperations, "LdapOperations must not be null!");
		Assert.notNull(entityType, "Type must not be null!");

		this.ldapOperations = ldapOperations;
		this.entityType = entityType;
		this.filterGenerator = new LdapSerializer(ldapOperations.getObjectDirectoryMapper(), this.entityType);
	}

	/* (non-Javadoc)
	 * @see com.querydsl.core.FilteredClause#where(com.querydsl.core.types.Predicate[])
	 */
	@Override
	public QueryDslLdapQuery<K> where(Predicate... o) {
		return queryMixin.where(o);
	}

	@SuppressWarnings("unchecked")
	public List<K> list() {
		return (List<K>) ldapOperations.find(buildQuery(), entityType);
	}

	public K uniqueResult() {
		return ldapOperations.findOne(buildQuery(), entityType);
	}

	LdapQuery buildQuery() {
		return query().filter(filterGenerator.handle(queryMixin.getMetadata().getWhere()));
	}

}
