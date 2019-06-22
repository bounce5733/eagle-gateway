package com.eagle.gateway.server.enums;

/**
 * 路由配置文件key
 * 
 * @author jiangyonghua
 * @date 2019年6月20日
 */
public enum AppConfigKey {

	ROUTES_FILENAMES("routes.filenames");

	private final String key;

	private final static String PREFIX = "app";

	private AppConfigKey(String key) {
		this.key = key;
	}

	public String value() {
		return PREFIX + "." + key;
	}
}
