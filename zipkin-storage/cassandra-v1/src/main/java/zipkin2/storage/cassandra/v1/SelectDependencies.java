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
package zipkin2.storage.cassandra.v1;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import zipkin2.Call;
import zipkin2.DependencyLink;
import zipkin2.internal.Dependencies;
import zipkin2.internal.DependencyLinker;
import zipkin2.storage.cassandra.internal.call.ResultSetFutureCall;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

final class SelectDependencies extends ResultSetFutureCall<List<DependencyLink>> {
  static final class Factory {
    final Session session;
    final PreparedStatement preparedStatement;

    Factory(Session session) {
      this.session = session;
      this.preparedStatement = session.prepare(select("dependencies").from("dependencies")
        .where(in("day", bindMarker())));
    }

    Call<List<DependencyLink>> create(long endTs, long lookback) {
      List<Date> days = CassandraUtil.getDays(endTs, lookback);
      return new SelectDependencies(this, days);
    }
  }

  final Factory factory;
  final List<Date> epochDays;

  SelectDependencies(Factory factory, List<Date> epochDays) {
    this.factory = factory;
    this.epochDays = epochDays;
  }

  @Override protected ResultSetFuture newFuture() {
    return factory.session.executeAsync(factory.preparedStatement.bind().setList(0, epochDays));
  }

  @Override public String toString() {
    return "SelectDependencies{days=" + epochDays + "}";
  }

  @Override public SelectDependencies clone() {
    return new SelectDependencies(factory, epochDays);
  }

  @Override public List<DependencyLink> map(ResultSet rs) {
    List<DependencyLink> unmerged = new ArrayList<>();
    for (Row row : rs) {
      ByteBuffer encodedDayOfDependencies = row.getBytes("dependencies");
      unmerged.addAll(Dependencies.fromThrift(encodedDayOfDependencies).links());
    }
    return DependencyLinker.merge(unmerged);
  }
}
