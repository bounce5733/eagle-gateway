package com.eagle.gateway.server.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import com.eagle.gateway.server.enums.ErrorAttributeKey;
import com.eagle.gateway.server.enums.ServerErrorCode;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

	public GlobalErrorAttributes() {
		super(false);
	}

	public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = new LinkedHashMap<>();
		Throwable error = super.getError(request);
		log.error(error.getMessage() == null ? "网关异常" : error.getMessage(), error);
		if (error instanceof ServerException) {
			ServerException serverException = (ServerException) error;
			errorAttributes.put(ErrorAttributeKey.code.name(), serverException.getError().getCode());
			errorAttributes.put(ErrorAttributeKey.reason.name(), serverException.getError().getReason());
		} else if (error instanceof ResponseStatusException) {
			ResponseStatusException responseStatusException = (ResponseStatusException) error;
			errorAttributes.put(ErrorAttributeKey.code.name(), responseStatusException.getStatus().value());
			errorAttributes.put(ErrorAttributeKey.reason.name(), responseStatusException.getMessage());
		} else if (error instanceof TimeoutException || error instanceof HystrixRuntimeException) {
			errorAttributes.put(ErrorAttributeKey.code.name(), ServerErrorCode.REQUEST_FUSE_ERROR.getCode());
			errorAttributes.put(ErrorAttributeKey.reason.name(), ServerErrorCode.REQUEST_FUSE_ERROR.getReason());
		} else {
			errorAttributes.put(ErrorAttributeKey.code.name(), ServerErrorCode.INTERNAL_GATEWAY_ERROR.getCode());
			errorAttributes.put(ErrorAttributeKey.reason.name(), ServerErrorCode.INTERNAL_GATEWAY_ERROR.getReason());
		}
		errorAttributes.put(ErrorAttributeKey.uri.name(), request.uri().getPath());
		return errorAttributes;
	}
}
