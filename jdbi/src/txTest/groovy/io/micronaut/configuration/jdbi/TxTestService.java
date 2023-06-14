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
package io.micronaut.configuration.jdbi;

import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Singleton
public class TxTestService {

    private final Jdbi db;

    public TxTestService(Jdbi db) {
        this.db = db;
    }

    @PostConstruct
    void init() {
        setupData();
    }

    @Transactional
    void setupData() {
        db.useHandle(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS foo(id INTEGER);");
            handle.execute("INSERT INTO foo(id) VALUES (0);");
        });
    }

    @ReadOnly
    public int count() {
        return db.withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM foo").mapTo(Integer.class).one()
        );
    }

    @Transactional
    public void abortTransaction() {
        db.useHandle(handle -> {
            handle.execute("INSERT INTO foo(id) VALUES (1);");
            int count = count();
            throw new RuntimeException("count=" + count);
        });
    }

    @Transactional
    public void commitTransaction() {
        db.useHandle(handle ->
                handle.execute("INSERT INTO foo(id) VALUES (1);")
        );
    }

    @Transactional
    public void dropData() {
        db.useHandle(handle ->
                handle.execute("DELETE FROM foo")
        );
    }
}
