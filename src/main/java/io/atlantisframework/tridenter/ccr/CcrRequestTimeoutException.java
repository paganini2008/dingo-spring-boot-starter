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
package io.atlantisframework.tridenter.ccr;

/**
 * 
 * CcrRequestTimeoutException
 *
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class CcrRequestTimeoutException extends CcrRequestException {

	private static final long serialVersionUID = -8547355390875394014L;

	public CcrRequestTimeoutException(String name, long serial, long round, int timeout) {
		super(name, serial, round);
		this.timeout = timeout;
	}

	private final int timeout;

	public int getTimeout() {
		return timeout;
	}

}
