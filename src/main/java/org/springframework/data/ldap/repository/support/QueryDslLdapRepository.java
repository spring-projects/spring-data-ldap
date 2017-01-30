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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Base repository implementation for QueryDSL support.
 *
 * @author Mattias Hellborg Arthursson
 * @author Eddu Melendez
 * @author Mark Paluch
 */
public class QueryDslLdapRepository<T> extends SimpleLdapRepository<T> implements QuerydslPredicateExecutor<T> {

	private final LdapOperations ldapOperations;
	private final Class<T> entityType;

	/**
	 * Creates a new {@link QueryDslLdapRepository}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param odm must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 */
	public QueryDslLdapRepository(LdapOperations ldapOperations, ObjectDirectoryMapper odm, Class<T> entityType) {
		super(ldapOperations, odm, entityType);

		this.ldapOperations = ldapOperations;
		this.entityType = entityType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findOne(com.querydsl.core.types.Predicate)
	 */
	@Override
	public T findOne(Predicate predicate) {
		return queryFor(predicate).uniqueResult();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate)
	 */
	@Override
	public List<T> findAll(Predicate predicate) {
		return queryFor(predicate).list();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#count(com.querydsl.core.types.Predicate)
	 */
	@Override
	public long count(Predicate predicate) {
		return findAll(predicate).size();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#exists(com.querydsl.core.types.Predicate)
	 */
	public boolean exists(Predicate predicate) {
		return count(predicate) > 0;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Sort)
	 */
	public Iterable<T> findAll(Predicate predicate, Sort sort) {
		throw new UnsupportedOperationException();
	}

	private QueryDslLdapQuery<T> queryFor(Predicate predicate) {
		return new QueryDslLdapQuery<>(ldapOperations, entityType).where(predicate);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.OrderSpecifier[])
	 */
	public Iterable<T> findAll(OrderSpecifier<?>... orders) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, com.querydsl.core.types.OrderSpecifier[])
	 */
	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Pageable)
	 */
	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {
		throw new UnsupportedOperationException();
	}
}
