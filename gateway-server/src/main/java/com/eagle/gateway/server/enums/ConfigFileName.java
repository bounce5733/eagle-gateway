package com.eagle.gateway.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 固定加载配置文件名称
 * 
 * @author jiangyonghua
 * @date 2019年6月21日
 */
@Getter
@AllArgsConstructor
public enum ConfigFileName {

	GATEWAY_SERVER_YAML("gateway_server.yaml"), APP_BASE_YAML("app_base.yaml");

	private final String value;
}
