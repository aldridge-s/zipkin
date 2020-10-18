/*
 * Copyright 2015-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.storage.cassandra.internal;

import org.junit.Test;
import zipkin2.storage.StorageComponent;

import static com.datastax.oss.driver.api.core.config.DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

public class CassandraStorageBuilderTest {
  CassandraStorageBuilder<?> builder = new CassandraStorageBuilder("zipkin3") {
    @Override public StorageComponent build() {
      return null;
    }
  };

  @Test public void maxConnections_setsMaxConnectionsPerDatacenterLocalHost() {
    assertThat(builder.maxConnections(16).poolingOptions().get(CONNECTION_POOL_LOCAL_SIZE))
      .isEqualTo(16);
  }
}
