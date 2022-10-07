/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.atlantisframework.tridenter.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.paganini2008.devtools.CharsetUtils;

import io.atlantisframework.tridenter.ApplicationClusterLoadBalancer;
import io.atlantisframework.tridenter.ClusterConstants;
import io.atlantisframework.tridenter.LoadBalancer;

/**
 * 
 * RestClientConfig
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Configuration
public class RestClientConfig {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@ConditionalOnMissingBean(name = "applicationClusterLoadBalancer")
	@Bean
	public LoadBalancer applicationClusterLoadBalancer(RedisConnectionFactory connectionFactory) {
		final String name = ClusterConstants.APPLICATION_CLUSTER_NAMESPACE + clusterName + ":counter";
		return new ApplicationClusterLoadBalancer(name, connectionFactory);
	}

	@ConditionalOnMissingBean
	@Bean
	public RoutingAllocator routingAllocator() {
		return new LoadBalanceRoutingAllocator();
	}

	@Bean
	public LoadBalanceHttpClientInterceptor loadBalanceHttpClientInterceptor() {
		return new LoadBalanceHttpClientInterceptor();
	}

	@Bean
	public RestClientPerformer restClientPerformer(ClientHttpRequestFactory clientHttpRequestFactory) {
		DefaultRestClientPerformer restClientPerformer = new DefaultRestClientPerformer(clientHttpRequestFactory, CharsetUtils.UTF_8);
		restClientPerformer.getInterceptors().add(loadBalanceHttpClientInterceptor());
		return restClientPerformer;
	}

	@Bean
	public RetryTemplateFactory retryTemplateFactory() {
		return new RetryTemplateFactory();
	}

	@Bean
	public RequestInterceptorContainer requestInterceptorContainer() {
		return new RequestInterceptorContainer();
	}

	@Bean
	public RequestTemplate genericRequestTemplate(RoutingAllocator routingAllocator, RestClientPerformer restClientPerformer,
			RetryTemplateFactory retryTemplateFactory, @Qualifier("applicationClusterTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
			RequestInterceptorContainer requestInterceptorContainer, @Qualifier("requestStatistic") StatisticIndicator statisticIndicator) {
		return new RequestTemplate(routingAllocator, restClientPerformer, retryTemplateFactory, taskExecutor, requestInterceptorContainer,
				statisticIndicator);
	}

	@Bean
	public LoggingRetryListener loggingRetryListener() {
		return new LoggingRetryListener();
	}

	@Bean("requestStatistic")
	public StatisticIndicator requestStatistic() {
		return new RequestStatisticIndicator();
	}

	/**
	 * 
	 * ResponseStatisticConfig
	 *
	 * @author Fred Feng
	 * @since 2.0.1
	 */
	@Configuration
	public static class ResponseStatisticConfig implements WebMvcConfigurer {

		@Bean("responseStatistic")
		public StatisticIndicator responseStatistic() {
			return new ResponseStatisticIndicator();
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor((HandlerInterceptor) responseStatistic());
		}

	}

	/**
	 * 
	 * HttpClientConfig
	 *
	 * @author Fred Feng
	 * 
	 * @since 2.0.1
	 */
	@ConditionalOnMissingBean(ClientHttpRequestFactory.class)
	@Configuration(proxyBeanMethods = false)
	public static class HttpClientConfig {

		@Value("${spring.application.cluster.httpclient.pool.maxTotal:200}")
		private int maxTotal;

		@Value("${spring.application.cluster.httpclient.connectionTimeout:60000}")
		private int connectionTimeout;

		@Bean
		public ClientHttpRequestFactory clientHttpRequestFactory() {
			return new HttpComponentsClientHttpRequestFactory(defaultHttpClient());
		}

		@Bean
		public HttpClient defaultHttpClient() {
			Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
			connectionManager.setMaxTotal(maxTotal);
			connectionManager.setDefaultMaxPerRoute(maxTotal / 4);
			connectionManager.setValidateAfterInactivity(10000);
			RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
					.setCircularRedirectsAllowed(false).setRedirectsEnabled(false).setSocketTimeout(connectionTimeout)
					.setConnectTimeout(connectionTimeout).setConnectionRequestTimeout(connectionTimeout);
			HttpClientBuilder builder = HttpClients.custom().disableAutomaticRetries().setConnectionManager(connectionManager)
					.setDefaultRequestConfig(requestConfigBuilder.build());
			return builder.build();
		}
	}

}
