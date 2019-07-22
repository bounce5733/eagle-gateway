package com.eagle.gateway.server.constant;

/**
 * 系统全局常量
 * 
 * @author jiangyonghua
 * @date 2019年6月4日
 */
public class SysConst {

	public static final String ENCODING = "UTF-8";

	public static final String APP_ID = "appid";

	/**
	 * 网关自定义异常起始值
	 */
	public static final int MIN_ERROR_CODE = 1000;

	/**
	 * 客户端加密数据key
	 */
	public static final String ENCRYPT_DATA_KEY = "cipher";

	/**
	 * 加密秘钥
	 */
	public static final String ENCRYPT_KEY = "test,34343";

	// 网关固定配置文件
	public static final String GATEWAY_SERVER_YAML = "gateway_server.yaml";
	public static final String GATEWAY_CUSTOM_YAML = "gateway_custom.yaml";

}
