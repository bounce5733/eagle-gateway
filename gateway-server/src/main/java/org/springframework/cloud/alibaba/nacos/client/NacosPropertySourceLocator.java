/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.alibaba.nacos.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.alibaba.nacos.NacosConfigProperties;
import org.springframework.cloud.alibaba.nacos.NacosPropertySourceRepository;
import org.springframework.cloud.alibaba.nacos.refresh.NacosContextRefresher;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import com.eagle.gateway.server.constant.SysConst;

/**
 * 做了路由合并修改
 * 
 * @author jiangyonghua
 * @date 2019年6月14日
 */
@Order(0)
public class NacosPropertySourceLocator implements PropertySourceLocator {

	private static final Logger log = LoggerFactory.getLogger(NacosPropertySourceLocator.class);
	private static final String NACOS_PROPERTY_SOURCE_NAME = "NACOS";
	private static final String SEP1 = "-";
	private static final String DOT = ".";
	private static final String SHARED_CONFIG_SEPARATOR_CHAR = "[,]";
	private static final List<String> SUPPORT_FILE_EXTENSION = Arrays.asList("properties", "yaml", "yml");

	private NacosPropertySourceBuilder nacosPropertySourceBuilder;

	private NacosConfigProperties nacosConfigProperties;

	public NacosPropertySourceLocator(NacosConfigProperties nacosConfigProperties) {
		this.nacosConfigProperties = nacosConfigProperties;
	}

	@Override
	public PropertySource<?> locate(Environment env) {

		ConfigService configService = nacosConfigProperties.configServiceInstance();

		if (null == configService) {
			log.warn("no instance of config service found, can't load config from nacos");
			return null;
		}
		long timeout = nacosConfigProperties.getTimeout();
		nacosPropertySourceBuilder = new NacosPropertySourceBuilder(configService, timeout);
		String name = nacosConfigProperties.getName();

		String dataIdPrefix = nacosConfigProperties.getPrefix();
		if (StringUtils.isEmpty(dataIdPrefix)) {
			dataIdPrefix = name;
		}

		if (StringUtils.isEmpty(dataIdPrefix)) {
			dataIdPrefix = env.getProperty("spring.application.name");
		}

		CompositePropertySource composite = new CompositePropertySource(NACOS_PROPERTY_SOURCE_NAME);

		loadSharedConfiguration(composite);
		loadExtConfiguration(composite);
		loadApplicationConfiguration(composite, dataIdPrefix, nacosConfigProperties, env);

		// <<======== modified(spring.cloud.gateway.routes多文件分割功能）========

		CompositePropertySource newComposite = new CompositePropertySource(NACOS_PROPERTY_SOURCE_NAME);
		NacosPropertySource appBasePropertySource = null;

		for (PropertySource<?> ps : composite.getPropertySources()) {
			if (SysConst.GATEWAY_SERVER_YAML.equals(ps.getName())) {
				newComposite.addPropertySource(ps);
			}
			if (SysConst.GATEWAY_CUSTOM_YAML.equals(ps.getName())) {
				appBasePropertySource = (NacosPropertySource) ps;
				newComposite.addPropertySource(appBasePropertySource);
			}
		}

		// 计算GATEWAY_SERVER_YAML里面的公共路由数量
		Set<String> routeDataIdArry = new HashSet<>();
		Set<Integer> baseRouteIndexSet = new HashSet<>();
		for (PropertySource<?> ps : composite.getPropertySources()) {
			if (SysConst.GATEWAY_SERVER_YAML.equals(ps.getName()) && ps instanceof NacosPropertySource) {
				NacosPropertySource basePropertySource = (NacosPropertySource) ps;
				for (String k : basePropertySource.getSource().keySet()) {
					if (k.startsWith("spring.cloud.gateway.routes")) {
						int routeIndexStart = k.indexOf("routes[") + 6;
						int routeIndexEnd = k.indexOf("]", routeIndexStart);
						int routeIndex = Integer.valueOf(k.substring(routeIndexStart + 1, routeIndexEnd));
						baseRouteIndexSet.add(routeIndex);
					}
					if (k.startsWith("app.routes.filenames")) {
						routeDataIdArry.add(basePropertySource.getSource().get(k).toString());
					}
				}
			}
		}

		// 加载路由配置文件
		loadMyConfiguration(composite, routeDataIdArry.toArray(new String[routeDataIdArry.size()]));

		int mergeBeginRouteIndex = baseRouteIndexSet.size();
		for (PropertySource<?> ps : composite.getPropertySources()) {
			if (!SysConst.GATEWAY_SERVER_YAML.contentEquals(ps.getName())
					&& !SysConst.GATEWAY_CUSTOM_YAML.contentEquals(ps.getName())) {
				if (ps instanceof NacosPropertySource) {
					NacosPropertySource nps = (NacosPropertySource) ps;
					if (nps.getSource() != null && nps.getSource().get("spring.cloud.gateway.routes[0].id") != null) {
						mergeBeginRouteIndex = mergeBeginRouteIndex + this.mergeRoute(appBasePropertySource.getSource(),
								nps.getSource(), mergeBeginRouteIndex);
					}
				}
			}
		}
		log.debug("路由信息：" + JSON.toJSONString(newComposite.getPropertySources()));
		return newComposite;

		// ================modified================>>
	}

	/**
	 * <<========modified（加载routes.filenames合并路由）========>>
	 * 
	 * @param routes
	 * @param addRoutes
	 * @param beginRouteIndex 其实路由下标
	 * @return 被合并路由文件的路由数量
	 */
	private int mergeRoute(Map<String, Object> routes, Map<String, Object> addRoutes, int beginRouteIndex) {
		Set<Integer> routeIndexSet = new HashSet<Integer>();
		for (String k : addRoutes.keySet()) {
			if (k.startsWith("spring.cloud.gateway.routes")) {
				int routeIndexStart = k.indexOf("routes[") + 6;
				int routeIndexEnd = k.indexOf("]", routeIndexStart);
				int routeIndex = Integer.valueOf(k.substring(routeIndexStart + 1, routeIndexEnd));
				routeIndexSet.add(routeIndex);
				routes.put(k.replace("routes[" + routeIndex + "]", "routes[" + (beginRouteIndex + routeIndex) + "]"),
						addRoutes.get(k));
			}
		}
		return routeIndexSet.size();
	}

	/**
	 * <<========modified（加载routes.filenames路由配置）========>>
	 * 
	 * @param compositePropertySource
	 * @param routeDataIdArry
	 */
	private void loadMyConfiguration(CompositePropertySource compositePropertySource, String[] routeDataIdArry) {
		if (routeDataIdArry.length == 0)
			return;

		checkDataIdFileExtension(routeDataIdArry);

		for (int i = 0; i < routeDataIdArry.length; i++) {
			String dataId = routeDataIdArry[i];
			String fileExtendsion = dataId.substring(dataId.lastIndexOf(".") + 1);
			loadNacosDataIfPresent(compositePropertySource, dataId, "DEFAULT_GROUP", fileExtendsion, true);
		}
	}

	private void loadSharedConfiguration(CompositePropertySource compositePropertySource) {
		String sharedDataIds = nacosConfigProperties.getSharedDataids();
		String refreshDataIds = nacosConfigProperties.getRefreshableDataids();

		if (sharedDataIds == null || sharedDataIds.trim().length() == 0) {
			return;
		}

		String[] sharedDataIdArry = sharedDataIds.split(SHARED_CONFIG_SEPARATOR_CHAR);
		checkDataIdFileExtension(sharedDataIdArry);

		for (int i = 0; i < sharedDataIdArry.length; i++) {
			String dataId = sharedDataIdArry[i];
			String fileExtension = dataId.substring(dataId.lastIndexOf(".") + 1);
			boolean isRefreshable = checkDataIdIsRefreshbable(refreshDataIds, sharedDataIdArry[i]);

			loadNacosDataIfPresent(compositePropertySource, dataId, "DEFAULT_GROUP", fileExtension, isRefreshable);
		}
	}

	private void loadExtConfiguration(CompositePropertySource compositePropertySource) {
		if (nacosConfigProperties.getExtConfig() == null || nacosConfigProperties.getExtConfig().isEmpty()) {
			return;
		}

		List<NacosConfigProperties.Config> extConfigs = nacosConfigProperties.getExtConfig();
		checkExtConfiguration(extConfigs);

		for (NacosConfigProperties.Config config : extConfigs) {
			String dataId = config.getDataId();
			String fileExtension = dataId.substring(dataId.lastIndexOf(".") + 1);
			loadNacosDataIfPresent(compositePropertySource, dataId, config.getGroup(), fileExtension,
					config.isRefresh());
		}
	}

	private void checkExtConfiguration(List<NacosConfigProperties.Config> extConfigs) {
		String[] dataIds = new String[extConfigs.size()];
		for (int i = 0; i < extConfigs.size(); i++) {
			String dataId = extConfigs.get(i).getDataId();
			if (dataId == null || dataId.trim().length() == 0) {
				throw new IllegalStateException(
						String.format("the [ spring.cloud.nacos.config.ext-config[%s] ] must give a dataid", i));
			}
			dataIds[i] = dataId;
		}
		checkDataIdFileExtension(dataIds);
	}

	private void loadApplicationConfiguration(CompositePropertySource compositePropertySource, String dataIdPrefix,
			NacosConfigProperties properties, Environment environment) {

		String fileExtension = properties.getFileExtension();
		String nacosGroup = properties.getGroup();

		loadNacosDataIfPresent(compositePropertySource, dataIdPrefix + DOT + fileExtension, nacosGroup, fileExtension,
				true);
		for (String profile : environment.getActiveProfiles()) {
			String dataId = dataIdPrefix + SEP1 + profile + DOT + fileExtension;
			loadNacosDataIfPresent(compositePropertySource, dataId, nacosGroup, fileExtension, true);
		}
	}

	private void loadNacosDataIfPresent(final CompositePropertySource composite, final String dataId,
			final String group, String fileExtension, boolean isRefreshable) {
		if (NacosContextRefresher.getRefreshCount() != 0) {
			NacosPropertySource ps;
			if (!isRefreshable) {
				ps = NacosPropertySourceRepository.getNacosPropertySource(dataId);
			} else {
				ps = nacosPropertySourceBuilder.build(dataId, group, fileExtension, true);
			}

			composite.addFirstPropertySource(ps);
		} else {
			NacosPropertySource ps = nacosPropertySourceBuilder.build(dataId, group, fileExtension, isRefreshable);
			composite.addFirstPropertySource(ps);
		}
	}

	private static void checkDataIdFileExtension(String[] dataIdArray) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < dataIdArray.length; i++) {
			boolean isLegal = false;
			for (String fileExtension : SUPPORT_FILE_EXTENSION) {
				if (dataIdArray[i].indexOf(fileExtension) > 0) {
					isLegal = true;
					break;
				}
			}
			// add tips
			if (!isLegal) {
				stringBuilder.append(dataIdArray[i] + ",");
			}
		}

		if (stringBuilder.length() > 0) {
			String result = stringBuilder.substring(0, stringBuilder.length() - 1);
			throw new IllegalStateException(
					String.format("[%s] must contains file extension with properties|yaml|yml", result));
		}
	}

	private boolean checkDataIdIsRefreshbable(String refreshDataIds, String sharedDataId) {
		if (refreshDataIds == null || "".equals(refreshDataIds)) {
			return false;
		}

		String[] refreshDataIdArry = refreshDataIds.split(SHARED_CONFIG_SEPARATOR_CHAR);
		for (String refreshDataId : refreshDataIdArry) {
			if (refreshDataId.equals(sharedDataId)) {
				return true;
			}
		}

		return false;
	}

}
