/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code ApplicationInstanceStatus} receives the status and properties of an application instance.
 *
 * @author Steve Powell
 */
class ApplicationInstanceStatus {

	private volatile String state;

	private volatile Statistics statistics;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	@JsonProperty("stats")
	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}

	static class Statistics {

		private volatile Usage usage;

		private volatile String name;

		private volatile List<String> uris;

		private volatile String host;

		private volatile Integer port;

		private volatile Integer uptime;

		private volatile Integer memoryQuota;

		private volatile Integer diskQuota;

		private volatile Integer fdsQuota;

		public Usage getUsage() {
			return usage;
		}

		public void setUsage(Usage usage) {
			this.usage = usage;
		}

		static class Usage {

			private volatile Integer disk;

			private volatile Integer memory;

			private volatile Double cpu;

			private volatile String time;

			public Integer getDisk() {
				return disk;
			}

			public void setDisk(Integer disk) {
				this.disk = disk;
			}

			public Integer getMemory() {
				return memory;
			}

			@JsonProperty("mem")
			public void setMemory(Integer memory) {
				this.memory = memory;
			}

			public Double getCpu() {
				return cpu;
			}

			public void setCpu(Double cpu) {
				this.cpu = cpu;
			}

			public String getTime() {
				return time;
			}

			public void setTime(String time) {
				this.time = time;
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getUris() {
			return uris;
		}

		public void setUris(List<String> uris) {
			this.uris = uris;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public Integer getUptime() {
			return uptime;
		}

		public void setUptime(Integer uptime) {
			this.uptime = uptime;
		}

		public Integer getMemoryQuota() {
			return memoryQuota;
		}

		@JsonProperty("mem_quota")
		public void setMemoryQuota(Integer memoryQuota) {
			this.memoryQuota = memoryQuota;
		}

		public Integer getDiskQuota() {
			return diskQuota;
		}

		@JsonProperty("disk_quota")
		public void setDiskQuota(Integer diskQuota) {
			this.diskQuota = diskQuota;
		}

		public Integer getFdsQuota() {
			return fdsQuota;
		}

		@JsonProperty("fds_quota")
		public void setFdsQuota(Integer fdsQuota) {
			this.fdsQuota = fdsQuota;
		}
	}
}
