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
package io.atlantisframework.tridenter.election;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;

import com.github.paganini2008.springdessert.reditools.RedisComponentNames;
import com.github.paganini2008.springdessert.reditools.common.RedisTtlKeeper;

import io.atlantisframework.tridenter.ApplicationInfo;
import io.atlantisframework.tridenter.ClusterConstants;
import io.atlantisframework.tridenter.InstanceId;
import io.atlantisframework.tridenter.ccr.CcrRequestConfirmationEvent;
import io.atlantisframework.tridenter.ccr.CcrRequestLauncher;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * CcrLeaderElection
 *
 * @author Fred Feng
 * @since 2.0.1
 */
@Slf4j
public class CcrLeaderElection implements LeaderElection, ApplicationContextAware, SmartApplicationListener {

	private ApplicationContext applicationContext;

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Value("${atlantis.framework.tridenter.election.leader.lease:10}")
	private int leaderLease;

	@Autowired
	private InstanceId instanceId;

	@Qualifier(RedisComponentNames.REDIS_TEMPLATE)
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private RedisTtlKeeper ttlKeeper;

	@Autowired
	private CcrRequestLauncher requestLauncher;

	@Override
	public void launch() {
		final String leaderIdentify = ClusterConstants.APPLICATION_CLUSTER_NAMESPACE + clusterName + ":leader";
		requestLauncher.propose(leaderIdentify, instanceId.getApplicationInfo(), DEFAULT_TIMEOUT);
		log.info("Start leader election. Identify: " + leaderIdentify);
	}

	@Override
	public void onTriggered(ApplicationEvent applicationEvent) {
		final CcrRequestConfirmationEvent event = (CcrRequestConfirmationEvent) applicationEvent;
		ApplicationInfo leaderInfo = (ApplicationInfo) event.getRequest().getValue();
		if (instanceId.getApplicationInfo().equals(leaderInfo)) {
			applicationContext.publishEvent(new ApplicationClusterLeaderEvent(applicationContext));
			log.info("This is the leader of application cluster '{}'. Current application event type is '{}'", clusterName,
					ApplicationClusterLeaderEvent.class.getName());
		} else {
			applicationContext.publishEvent(new ApplicationClusterFollowerEvent(applicationContext, leaderInfo));
			log.info("This is the follower of application cluster '{}'. Current application event type is '{}'", clusterName,
					ApplicationClusterFollowerEvent.class.getName());
		}
		leaderInfo.setLeader(true);
		instanceId.setLeaderInfo(leaderInfo);
		log.info("Leader's info: " + leaderInfo);

		final String key = ClusterConstants.APPLICATION_CLUSTER_NAMESPACE + clusterName;
		redisTemplate.opsForList().leftPush(key, instanceId.getApplicationInfo());
		if (instanceId.isLeader()) {
			ttlKeeper.watchKey(key, leaderLease, TimeUnit.SECONDS);
		}

		applicationContext.publishEvent(new ApplicationClusterRefreshedEvent(applicationContext, leaderInfo));
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return eventType == CcrRequestConfirmationEvent.class;
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return sourceType == ApplicationInfo.class;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		final CcrRequestConfirmationEvent event = (CcrRequestConfirmationEvent) applicationEvent;
		final String leaderIdentify = ClusterConstants.APPLICATION_CLUSTER_NAMESPACE + clusterName + ":leader";
		if (leaderIdentify.equals(event.getRequest().getName())) {
			if (event.isOk()) {
				onTriggered(event);
			} else {
				throw new LeaderNotFoundException();
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
