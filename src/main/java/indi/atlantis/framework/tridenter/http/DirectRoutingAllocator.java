/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.tridenter.http;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 
 * DirectRoutingAllocator
 *
 * @author Fred Feng
 * 
 * @since 1.0
 */
public class DirectRoutingAllocator implements RoutingAllocator {

	private final boolean testUrl;

	public DirectRoutingAllocator() {
		this(false);
	}

	public DirectRoutingAllocator(boolean testUrl) {
		this.testUrl = testUrl;
	}

	@Override
	public String allocateHost(String provider, String path, Request request) {
		String url = provider + path;
		if (testUrl) {
			try {
				new URL(url);
			} catch (MalformedURLException e) {
				throw new RoutingPolicyException("Invalid url: " + url, e);
			}
		}
		return url;
	}

}
