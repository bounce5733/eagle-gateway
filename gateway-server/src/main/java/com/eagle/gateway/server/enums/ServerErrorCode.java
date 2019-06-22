package com.eagle.gateway.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 网关异常枚举
 * 
 * @author jiangyonghua
 * @date 2019年6月21日
 */
@Getter
@AllArgsConstructor
public enum ServerErrorCode {

	INTERNAL_GATEWAY_ERROR(1000, "网关内部异常"),

	AUTHENTICATE_FAILED(1001, "认证失败"),

	ILLEGAL_URL(1002, "非法请求地址"),

	ILLEGAL_SECURITY_HEADER(1004, "非法安全请求头"),

	DATA_DECRYPT_ERROR(1005, "解码数据出错"),

	REPLAY_ATTACK_ERROR(1006, "重放攻击出错"),

	REQUEST_FUSE_ERROR(1007, "请求已被熔断"),

	SESSION_INVALID(1008, "会话失效"),

	INVALID_URI_QUERY_PARAM(1009, "URI查询参数不合法"),

	SQL_INJECT_ERROR(1010, "请求数据包含SQL注入风险");

	private final int code;

	private final String reason;
}
