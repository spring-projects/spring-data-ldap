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
package org.springframework.data.ldap.repository.query;

import java.util.List;

import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.ldap.repository.support.UnitTestPerson;
import org.springframework.data.repository.query.Param;
import org.springframework.ldap.query.SearchScope;

/**
 * @author Marcin Grzejszczak
 */
public interface QueryRepository extends LdapRepository<SchemaEntry> {
	@Query(value = "(cn=:fullName)")
	List<SchemaEntry> namedParameters(@Param("fullName") String fullName, @Param("lastName") String lastName);

	@Query(value = "(cn=?1)")
	List<SchemaEntry> indexedParameters(String fullName, String lastName);

	@Query(value = "(cn=#{'John ' + 'Doe'})")
	List<SchemaEntry> spelParameters();

	@Query( value = "(cn=${full.name})")
	List<SchemaEntry> propertyPlaceholderParameters();

}
