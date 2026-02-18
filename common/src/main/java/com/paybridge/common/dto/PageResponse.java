package com.paybridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PageResponse<T> {

	private List<T> content;
	private long totalElements;
	private int pageNumber;
	private int pageSize;
	private int totalPages;
	private boolean last;

	public static <T> PageResponse<T> of(List<T> content, long totalElements, int pageNumber, int pageSize) {
		return PageResponse.<T>builder()
				.content(content)
				.totalElements(totalElements)
				.pageNumber(pageNumber)
				.pageSize(pageSize)
				.totalPages((int) Math.ceil((double) totalElements / pageSize))
				.last((long) (pageNumber + 1) * pageSize >= totalElements)
				.build();
	}
}