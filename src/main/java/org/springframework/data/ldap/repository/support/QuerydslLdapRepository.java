/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.ldap.repository.support;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.ldap.core.mapping.LdapMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery;
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
 * @deprecated since 2.6, use {@link QuerydslLdapPredicateExecutor} instead.
 */
@Deprecated
public class QuerydslLdapRepository<T> extends SimpleLdapRepository<T> implements QuerydslPredicateExecutor<T> {

	private final QuerydslLdapPredicateExecutor<T> executor;

	/**
	 * Creates a new {@link QuerydslLdapRepository}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param odm must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 */
	public QuerydslLdapRepository(LdapOperations ldapOperations, ObjectDirectoryMapper odm, Class<T> entityType) {

		super(ldapOperations, odm, entityType);

		this.executor = new QuerydslLdapPredicateExecutor<>(entityType, new SpelAwareProxyProjectionFactory(),
				ldapOperations, new LdapMappingContext());
	}

	/**
	 * Creates a new {@link QuerydslLdapRepository}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param odm must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @since 2.6
	 */
	QuerydslLdapRepository(LdapOperations ldapOperations,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context,
			ObjectDirectoryMapper odm, Class<T> entityType) {

		super(ldapOperations, context, odm, entityType);

		this.executor = new QuerydslLdapPredicateExecutor<>(entityType, new SpelAwareProxyProjectionFactory(),
				ldapOperations, new LdapMappingContext());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findOne(com.querydsl.core.types.Predicate)
	 */
	@Override
	public Optional<T> findOne(Predicate predicate) {
		return executor.findOne(predicate);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate)
	 */
	@Override
	public List<T> findAll(Predicate predicate) {
		return executor.findAll(predicate);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#count(com.querydsl.core.types.Predicate)
	 */
	@Override
	public long count(Predicate predicate) {
		return executor.count(predicate);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#exists(com.querydsl.core.types.Predicate)
	 */
	public boolean exists(Predicate predicate) {
		return executor.exists(predicate);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Sort)
	 */
	public Iterable<T> findAll(Predicate predicate, Sort sort) {
		return executor.findAll(predicate, sort);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.OrderSpecifier[])
	 */
	public Iterable<T> findAll(OrderSpecifier<?>... orders) {
		return executor.findAll(orders);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, com.querydsl.core.types.OrderSpecifier[])
	 */
	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return executor.findAll(predicate, orders);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Pageable)
	 */
	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {
		return executor.findAll(predicate, pageable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QuerydslPredicateExecutor#findBy(com.querydsl.core.types.Predicate, java.util.function.Function)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <S extends T, R> R findBy(Predicate predicate,
			Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		return executor.findBy(predicate, queryFunction);
	}

}
