package com.eagle.gateway.auth;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonTest {

	@Test
	@Ignore
	public void encoderPwd() {
		String encryptStr = new BCryptPasswordEncoder().encode("myPassword");
		log.info(encryptStr);
	}
}
