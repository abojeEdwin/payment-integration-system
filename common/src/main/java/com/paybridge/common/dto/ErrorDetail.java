package com.paybridge.common.dto;

public record ErrorDetail(
	int status,
	String message
) {}
//# RFC 7807 compliant