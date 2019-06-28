package com.eagle.gateway.server.filter.factory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.fastjson.JSON;
import com.eagle.gateway.server.constant.SysConst;
import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.enums.ServerExchangeKey;
import com.eagle.gateway.server.exception.ServerException;
import com.eagle.gateway.server.util.EncryptUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 请求解密过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class RequestDecryptGatewayFilterFactory
		extends AbstractGatewayFilterFactory<RequestDecryptGatewayFilterFactory.Config> {

	/**
	 * 校验数据分隔符
	 */
	private static final String CHECK_VALS_SPLIT = " ";

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入请求解密过滤器========");

			ServerHttpRequest serverRequest = exchange.getRequest();

			// 解码请求参数
			if (config.includeQueryParams) {
				StringBuffer queryParamsExchangeVals = new StringBuffer();
				MultiValueMap<String, String> params = serverRequest.getQueryParams();
				Map<String, String> newSingleKeyParams = new HashMap<>();
				Map<String, List<String>> newMultiKeyParams = new HashMap<>();

				params.forEach((key, vals) -> {
					if (null != vals) {
						if (vals.size() > 1) {
							List<String> decryptVals = new ArrayList<>();
							vals.forEach(val -> {
								String decryptVal = EncryptUtil.decryptAES(val, SysConst.ENCRYPT_KEY);
								queryParamsExchangeVals.append(decryptVal).append(CHECK_VALS_SPLIT);// 空格用于区分每个健值
								try {
									decryptVals.add(URLEncoder.encode(decryptVal, SysConst.ENCODING));
								} catch (UnsupportedEncodingException e) {
									throw new ServerException(ServerErrorCode.DATA_DECRYPT_ERROR);
								}
							});
							newMultiKeyParams.put(key, decryptVals);
						} else {
							String decryptVal = EncryptUtil.decryptAES(vals.get(0), SysConst.ENCRYPT_KEY);
							queryParamsExchangeVals.append(decryptVal).append(CHECK_VALS_SPLIT);// 空格用于区分每个健值
							try {
								newSingleKeyParams.put(key, URLEncoder.encode(decryptVal, SysConst.ENCODING));
							} catch (UnsupportedEncodingException e) {
								throw new ServerException(ServerErrorCode.DATA_DECRYPT_ERROR);
							}
						}
					}
				});

				// 查询参数入过滤链
				exchange.getAttributes().put(ServerExchangeKey.requestQueryParams.name(),
						queryParamsExchangeVals.toString());

				// 组装新的查询参数
				StringBuilder query = new StringBuilder();
				newSingleKeyParams.forEach((key, val) -> {
					query.append(key).append("=").append(val).append("&");
				});
				newMultiKeyParams.forEach((key, vals) -> {
					vals.forEach(val -> {
						query.append(key).append("=").append(val).append("&");
					});
				});
				log.debug("解密后的查询参数：" + query.toString());

				try {
					URI newUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
							.replaceQuery(query.toString()).build(true).toUri();

					serverRequest = exchange.getRequest().mutate().uri(newUri).build();
				} catch (RuntimeException ex) {
					throw new ServerException(ServerErrorCode.INVALID_URI_QUERY_PARAM);
				}
			}

			// 解码请求体
			if (config.includeBody) {
				if (!checkCanModifyBody(serverRequest)) {
					chain.filter(exchange);
				}

				ServerRequest defaultServerRequest = new DefaultServerRequest(exchange);
				MediaType mediaType = serverRequest.getHeaders().getContentType();
				Mono<String> modifiedBody = defaultServerRequest.bodyToMono(String.class).flatMap(body -> {
					if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
						@SuppressWarnings("unchecked")
						Map<String, String> jsonDataMap = JSON.parseObject(body, Map.class);
						String decryptBody = EncryptUtil.decryptAES(jsonDataMap.get(SysConst.ENCRYPT_DATA_KEY),
								SysConst.ENCRYPT_KEY);

						// body数据入过滤链
						exchange.getAttributes().put(ServerExchangeKey.requestBody.name(), decryptBody);

						return Mono.just(decryptBody);
					}
					if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
						Map<String, String> formDataMap = new HashMap<>();
						String[] formDataArry = body.split("&");
						for (int i = 0; i < formDataArry.length; i++) {
							String[] formDataItem = formDataArry[i].split("=");
							try {
								formDataMap.put(formDataItem[0], EncryptUtil.decryptAES(
										URLDecoder.decode(formDataItem[1], SysConst.ENCODING), SysConst.ENCRYPT_KEY));
							} catch (UnsupportedEncodingException e) {
								throw new ServerException(ServerErrorCode.DATA_DECRYPT_ERROR);
							}
						}

						// 组装回明文数据
						StringBuffer decryptFormDataSb = new StringBuffer();
						StringBuffer decryptFormDataValsSb = new StringBuffer();
						formDataMap.forEach((key, val) -> {
							decryptFormDataSb.append(key).append("=").append(val).append("&");
							decryptFormDataValsSb.append(val).append(CHECK_VALS_SPLIT);
						});
						if (decryptFormDataSb.length() > 0) {
							String decryptFormDataStr = decryptFormDataSb.substring(0, decryptFormDataSb.length() - 1);
							// form data values数据入过滤链
							exchange.getAttributes().put(ServerExchangeKey.requestBody.name(),
									decryptFormDataValsSb.toString());

							return Mono.just(decryptFormDataStr);
						}
					}
					return Mono.empty();
				});

				BodyInserter<Mono<String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters
						.fromPublisher(modifiedBody, String.class);

				HttpHeaders headers = new HttpHeaders();
				headers.putAll(exchange.getRequest().getHeaders());

				// the new content type will be computed by bodyInserter
				// and then set in the request decorator
				headers.remove(HttpHeaders.CONTENT_LENGTH);

				final ServerHttpRequest newServerRequest = serverRequest;
				CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
				return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
					ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(newServerRequest) {

						@Override
						public HttpHeaders getHeaders() {
							long contentLength = headers.getContentLength();
							HttpHeaders httpHeaders = new HttpHeaders();
							httpHeaders.putAll(super.getHeaders());
							if (contentLength > 0) {
								httpHeaders.setContentLength(contentLength);
							} else {
								// TODO: this causes a 'HTTP/1.1 411 Length Required on httpbin.org
								httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
							}
							return httpHeaders;
						}

						@Override
						public Flux<DataBuffer> getBody() {
							return outputMessage.getBody();
						}
					};
					return chain.filter(exchange.mutate().request(decorator).build());
				}));
			}

			return chain.filter(exchange);
		};

	}

	public RequestDecryptGatewayFilterFactory() {
		super(Config.class);
	}

	@Data
	public static class Config {

		private boolean includeQueryParams = true;

		private boolean includeBody = true;
	}

	private boolean checkCanModifyBody(ServerHttpRequest httpRequest) {
		boolean isVaild = true;

		HttpMethod method = httpRequest.getMethod();
		if (method != HttpMethod.POST && method != HttpMethod.PUT && method != HttpMethod.PATCH) {
			isVaild = false;
		}

		String contentType = httpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		if (contentType == null || !contentType.contentEquals(MediaType.APPLICATION_JSON_UTF8_VALUE))
			isVaild = false;

		return isVaild;
	}

}
