package com.booknest.common;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PageRequestFactory {

	public static final int MAX_PAGE_SIZE = 100;

	private PageRequestFactory() {
	}

	public static PageRequest create(
			int page,
			int size,
			String requestedSort,
			String requestedDirection,
			Map<String, String> allowedSorts,
			String defaultSort,
			String defaultDirection
	) {
		if (page < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_page");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_page_size");
		}

		String sortName = requestedSort == null || requestedSort.isBlank() ? defaultSort : requestedSort;
		String sortProperty = allowedSorts.get(sortName);
		if (sortProperty == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_sort_field");
		}
		String direction = requestedDirection == null || requestedDirection.isBlank()
				? defaultDirection
				: requestedDirection.toLowerCase(java.util.Locale.ROOT);
		Sort.Direction sortDirection = switch (direction) {
			case "asc" -> Sort.Direction.ASC;
			case "desc" -> Sort.Direction.DESC;
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_sort_direction");
		};

		return PageRequest.of(
				page,
				size,
				Sort.by(sortDirection, sortProperty).and(Sort.by(sortDirection, "id"))
		);
	}
}
