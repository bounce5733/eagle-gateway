package com.eagle.gateway.server.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.eagle.gateway.server.constant.SysConst;
import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.exception.ServerException;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 加密工具
 * 
 * @author jiangyonghua
 * @date 2019年6月14日
 */
@Slf4j
public class EncryptUtil {

	/**
	 * 加密算法
	 */
	private static final String ENCRY_ALGORITHM = "AES";

	/**
	 * CBC模式必须设置16位偏移量
	 */
	private static final String IV = "abcdef0123456789";

	/**
	 * 加密算法|加密模式|填充类型
	 */
	private static final String CIPHER_MODE = "AES/ECB/PKCS5Padding";

	/**
	 * 加密
	 * 
	 * @param text 明文
	 * @param key  密钥
	 * @return 密文
	 */
	public static String encryptAES(String text, String key) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_MODE);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(SysConst.ENCODING), ENCRY_ALGORITHM),
					new IvParameterSpec(IV.getBytes(SysConst.ENCODING)));
			byte[] cipherTextBytes = cipher.doFinal(text.getBytes(SysConst.ENCODING));
			return new BASE64Encoder().encode(cipherTextBytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | UnsupportedEncodingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new ServerException(ServerErrorCode.DATA_DECRYPT_ERROR);
		}
	}

	/**
	 * 解密
	 * 
	 * @param cipherText 密文
	 * @param key        密钥
	 * @return 明文
	 */
	public static String decryptAES(String cipherText, String key) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_MODE);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(SysConst.ENCODING), ENCRY_ALGORITHM),
					new IvParameterSpec(IV.getBytes(SysConst.ENCODING)));
			byte[] textBytes = cipher.doFinal(new BASE64Decoder().decodeBuffer(cipherText));
			return new String(textBytes, SysConst.ENCODING);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| IOException e) {
			throw new ServerException(ServerErrorCode.DATA_DECRYPT_ERROR);
		}
	}

	public static void main(String[] args) throws IOException {
		String text = "明文 123 abc";
		String key = "gate_way_test111";
		String cipherText = encryptAES(text, key);
		log.info("加密：" + cipherText);
		log.info("解密：" + decryptAES(cipherText, key));
	}
}
