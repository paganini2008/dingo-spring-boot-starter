/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.tridenter.multiprocess;

import java.lang.reflect.Method;
import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;

import com.github.paganini2008.devtools.StringUtils;
import com.github.paganini2008.devtools.date.DateUtils;

/**
 * 
 * MultiSchedulingInterpreter
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
@Aspect
public class MultiSchedulingInterpreter {

	@Value("${spring.application.name}")
	private String applicationName;

	@Autowired
	private ScheduledProcessPool processPool;

	@Autowired
	private MultiProcessingMethodInspector methodInspector;

	@Pointcut("execution(public * *(..))")
	public void signature() {
	}

	@Around("signature() && @annotation(multiScheduling)")
	public Object arround(ProceedingJoinPoint pjp, MultiScheduling multiScheduling) throws Throwable {
		final Method method = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getMethod();
		Signature signature = (Signature) methodInspector.getSignature(ClassUtils.getUserClass(method.getDeclaringClass()),
				method.getName());
		MethodInvocation invocation = new MethodInvocation(signature, pjp.getArgs());
		if (processPool.hasScheduled(invocation)) {
			return pjp.proceed();
		} else {
			if (StringUtils.isNotBlank(multiScheduling.cron())) {
				processPool.schedule(invocation, multiScheduling.cron());
			} else if (multiScheduling.delay() > 0) {
				if (multiScheduling.repeatable()) {
					Date startDate = multiScheduling.initialDelay() > 0 ? new Date(System.currentTimeMillis()
							+ DateUtils.convertToMillis(multiScheduling.initialDelay(), multiScheduling.timeUnit())) : new Date();
					if (multiScheduling.fixedRate()) {
						processPool.scheduleAtFixedRate(invocation, startDate, multiScheduling.delay(), multiScheduling.timeUnit());
					} else {
						processPool.scheduleWithFixedDelay(invocation, startDate, multiScheduling.delay(), multiScheduling.timeUnit());
					}
				} else {
					processPool.schedule(invocation, multiScheduling.delay(), multiScheduling.timeUnit());
				}
			}
			return multiScheduling.alwaysExecuted() ? pjp.proceed() : null;
		}
	}

}
