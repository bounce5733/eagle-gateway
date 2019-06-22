package com.eagle.gateway.server.util;

import java.util.UUID;

/**
 * 主键生成器
 * 
 * @author jiangyonghua
 * @date 2019年6月4日
 */
public class IdGenUtil {

	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
