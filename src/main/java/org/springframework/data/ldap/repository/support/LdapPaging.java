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
package org.springframework.data.ldap.repository.support;

import java.util.List;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.util.Assert;

/**
 * LDAP paging queries utilities.
 * <p>
 * Only intended for internal use.
 *
 * @author Houcheng Lin
 * @author Mark Paluch
 */
public abstract class LdapPaging {

	private LdapPaging() {}

	/**
	 * Create a {@link PagedResultsDirContextProcessor} given {@link Pageable}. The {@link Pageable} must point either to
	 * the first page or be obtained through the resulting {@link Slice slice's} {@link Slice#nextPageable()}.
	 *
	 * @param pageable must not be {@literal null}.
	 * @return the {@link PagedResultsDirContextProcessor} for the {@link Pageable}.
	 */
	public static PagedResultsDirContextProcessor createPagedContext(Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null!");

		PagedResultsCookie pagedResultCookie = null;

		if (pageable instanceof LdapPageRequest) {
			pagedResultCookie = ((LdapPageRequest) pageable).getCookie();
		} else {
			Assert.isTrue(pageable.getPageNumber() == 0,
					"Pageable must continue from Slice.nextPageable() or PageNumber must be zero!");
		}

		return new PagedResultsDirContextProcessor(pageable.getPageSize(), pagedResultCookie);
	}

	/**
	 * Create a {@link Slice} given {@code content}, the requested {@link Pageable} and
	 * {@link PagedResultsDirContextProcessor}.
	 *
	 * @param content must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @param pagedContextProcessor must not be {@literal null}.
	 * @return the {@link Slice} containing the page query results.
	 */
	public static <T> Slice<T> createSlice(List<T> content, Pageable pageable,
			PagedResultsDirContextProcessor pagedContextProcessor) {

		Assert.notNull(content, "Content must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");
		Assert.notNull(pagedContextProcessor, "PagedResultsDirContextProcessor must not be null!");

		return new LdapSlice<>(content, pageable, pagedContextProcessor);
	}

	/**
	 * LDAP result {@link Slice}. Obtains the paging {@link PagedResultsCookie} for the next page from
	 * {@link PagedResultsDirContextProcessor}.
	 *
	 * @author Houcheng Lin
	 * @author Mark Paluch
	 */
	static class LdapSlice<T> extends SliceImpl<T> {

		private static final long serialVersionUID = 8097875186812535571L;

		private final Pageable pageable;
		private final PagedResultsCookie cookieForNextPageable;

		LdapSlice(List<T> content, Pageable pageable, PagedResultsDirContextProcessor processor) {

			super(content, pageable, processor.hasMore());

			this.pageable = pageable;
			this.cookieForNextPageable = processor.getCookie();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.SliceImpl#nextPageable()
		 */
		@Override
		public Pageable nextPageable() {

			if (hasNext()) {
				return new LdapPageRequest(pageable.getPageNumber() + 1, pageable.getPageSize(), cookieForNextPageable);
			}

			return Pageable.unpaged();
		}
	}

	/**
	 * {@link org.springframework.data.domain.PageRequest} for a next LDAP {@link Slice} query. This request is only valid
	 * for the single requested query and does not allow navigation to first/next/previous pages.
	 *
	 * @author Houcheng Lin
	 * @author Mark Paluch
	 */
	static class LdapPageRequest extends AbstractPageRequest {

		private static final long serialVersionUID = 8186132694880755626L;

		private final PagedResultsCookie cookie;

		LdapPageRequest(int page, int size, PagedResultsCookie cookie) {

			super(page, size);

			this.cookie = cookie;
		}

		PagedResultsCookie getCookie() {
			return cookie;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.Pageable#getSort()
		 */
		@Override
		public Sort getSort() {
			return Sort.unsorted();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.AbstractPageRequest#next()
		 */
		@Override
		public Pageable next() {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.AbstractPageRequest#previous()
		 */
		@Override
		public Pageable previous() {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.AbstractPageRequest#hasPrevious()
		 */
		@Override
		public boolean hasPrevious() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.domain.AbstractPageRequest#first()
		 */
		@Override
		public Pageable first() {
			throw new UnsupportedOperationException();
		}
	}
}
