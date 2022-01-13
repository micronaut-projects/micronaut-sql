/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.jooq;

import jakarta.inject.Singleton;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import reactor.core.publisher.Flux;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.SQLDataType.INTEGER;

@Singleton
public class AsyncTxTestService {

  private final DSLContext db;

  public AsyncTxTestService(DSLContext db) {
    this.db = db;
  }

  @PostConstruct
  public void init() {
    init(db);
  }

  private void init(DSLContext db) {
    Flux.from(db.dropTable("foo"))
        .thenMany(db.createTable("foo")
                    .column(name("id"), INTEGER)
                 )
        .subscribe();
  }

  @Transactional
  public Flux<Record1<Integer>> abortTransaction() {
    return Flux.from(db.insertInto(table(name("foo")),
                                   field(name("id")))
                       .values(120)
                       .returning(field(name("id"))))
               .thenMany(db.insertInto(table(name("foo")),
                                       field(name("id")))
                           .values(122)
                           .returning(field(name("id"))))
               .thenMany(getCountStatement())
               .doOnNext(x -> {
                 throw new RuntimeException("count=" + x.value1());
               });
  }

  @Transactional
  public Flux<Record1<Integer>> count() {
    return Flux.from(getCountStatement())
               .doOnNext(x -> System.out.println("roll back count: " + x.value1()));
  }

  @NotNull
  private SelectJoinStep<Record1<Integer>> getCountStatement() {
    return db.selectCount().from(table(name("foo")));
  }

  @Transactional
  public void commitTransaction() {

  }
}
