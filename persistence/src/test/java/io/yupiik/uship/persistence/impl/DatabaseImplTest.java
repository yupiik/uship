/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.uship.persistence.impl;

import io.yupiik.uship.persistence.api.Column;
import io.yupiik.uship.persistence.api.Database;
import io.yupiik.uship.persistence.api.Id;
import io.yupiik.uship.persistence.api.StatementBinder;
import io.yupiik.uship.persistence.api.Table;
import io.yupiik.uship.persistence.api.bootstrap.Configuration;
import io.yupiik.uship.persistence.api.lifecycle.OnInsert;
import io.yupiik.uship.persistence.api.lifecycle.OnLoad;
import io.yupiik.uship.persistence.api.operation.Operation;
import io.yupiik.uship.persistence.api.operation.Statement;
import io.yupiik.uship.persistence.impl.test.EnableH2;
import io.yupiik.uship.persistence.impl.translation.H2Translation;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseImplTest {
    @Test
    @EnableH2
    void operations(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.name = "test_" + i;
            database.insert(instance);
        }

        final var ops = database.operation(MyOps.class);
        final var all = ops.findAll();
        assertEquals(3, all.size());
        assertEquals("test_0", all.get(0).name);
        assertEquals(all.get(0), ops.findOne("test_0"));
        assertEquals(all.get(0), ops.findOneWithPlaceholders("test_0"));
        assertEquals(all.subList(0, 2), ops.findByName(List.of("test_0", "test_1")));

        final IntSupplier counter = () -> database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY order by name", StatementBinder.NONE).size();
        assertEquals(1, ops.delete("test_1"), () -> ops.findAll().toString());
        assertEquals(2, ops.countAll(), () -> ops.findAll().toString());
        assertEquals(counter.getAsInt(), ops.countAll(), () -> ops.findAll().toString());
        ops.deleteWithoutReturnedValue("test_0");
        assertEquals(1, counter.getAsInt(), () -> ops.findAll().toString());
        assertEquals(1, ops.delete("test_%"), () -> ops.findAll().toString());
        assertEquals(0, counter.getAsInt(), () -> ops.findAll().toString());
    }

    @Test
    @EnableH2
    void execute(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.name = "test_" + i;
            database.insert(instance);
        }

        final var all = database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY order by name", StatementBinder.NONE);
        assertEquals(3, all.size());

        assertEquals(3, database.execute("delete from FLAT_ENTITY where name like ?", b -> b.bind("test%")));
        assertEquals(0, database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY order by name", StatementBinder.NONE).size());
    }

    @Test
    @EnableH2
    void findAll(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        final var entities = new ArrayList<MyFlatEntity>();
        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.name = "test_" + i;
            database.insert(instance);
            entities.add(instance);
        }
        // query
        final var all = database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY order by name", StatementBinder.NONE);

        // cleanup
        entities.forEach(database::delete);

        // asserts
        assertEquals(entities, all);
    }

    @Test
    @EnableH2
    void findAllUsingAliases(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        final var entities = new ArrayList<MyFlatEntity>();
        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.name = "test_" + i;
            database.insert(instance);
            entities.add(instance);
        }
        // query
        final var entity = database.getOrCreateEntity(MyFlatEntity.class);
        final var all = database.query(
                "select name as pName, id as pId, age as pAge from FLAT_ENTITY order by pName", StatementBinder.NONE,
                r -> {
                    final var binder = entity
                            .mapFromPrefix("p", r.get()); // this can be cached in the context of this query (caller code) if query is stable
                    return r.mapAll(binder::apply);
                });

        // cleanup
        entities.forEach(database::delete);

        // asserts
        assertEquals(entities, all);
    }

    @Test
    @EnableH2
    void findWithBinding(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        final var entities = new ArrayList<MyFlatEntity>();
        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.name = "test_" + i;
            database.insert(instance);
            entities.add(instance);
        }
        // query
        final var all = database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY where name = ?", b -> b.bind("test_1"));

        // cleanup
        entities.forEach(database::delete);

        // asserts
        assertEquals(entities.subList(1, 2), all);
    }

    @Test
    @EnableH2
    void batch(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        final var entities = new ArrayList<MyFlatEntity>();
        for (int i = 0; i < 3; i++) { // seed data
            final var instance = new MyFlatEntity();
            instance.id = "test_" + i;
            instance.name = instance.id;
            entities.add(instance);
        }

        assertArrayEquals(
                new int[]{1, 1, 1},
                database.batch(
                        "insert into FLAT_ENTITY(id, age, name) values(?, ?, ?)",
                        entities.stream()
                                .map(it -> (Consumer<StatementBinder>) binder -> {
                                    binder.bind(String.class, it.id);
                                    binder.bind(int.class, it.age);
                                    binder.bind(String.class, it.name);
                                })
                                .iterator()));

        // check all was insert by batch
        final var all = database.query(MyFlatEntity.class, "select name, id, age from FLAT_ENTITY order by name", StatementBinder.NONE);
        entities.forEach(database::delete);
        assertEquals(entities, all);
    }

    @Test
    @EnableH2
    void guessTranslation(final DataSource dataSource) {
        final var configuration = new Configuration().setDataSource(dataSource);
        final var database = new DatabaseImpl(configuration);
        assertEquals(H2Translation.class, database.getTranslation().getClass());

    }

    @Test
    @EnableH2
    void ddl(final DataSource dataSource) throws SQLException {
        final var database = Database.of(new Configuration().setDataSource(dataSource));
        assertEquals(List.of(), listTables(dataSource));

        final var entity = database.getOrCreateEntity(MyFlatEntity.class);
        final var ddl = entity.ddl();
        assertArrayEquals(new String[]{
                "CREATE TABLE FLAT_ENTITY (id VARCHAR(255), age INTEGER, name VARCHAR(255), PRIMARY KEY (id))"
        }, ddl);
        assertEquals(List.of(), listTables(dataSource));

        try (final var connection = dataSource.getConnection();
             final var stmt = connection.createStatement()) {
            stmt.executeUpdate(ddl[0]);
        }
        assertEquals(List.of("FLAT_ENTITY"), listTables(dataSource));
    }

    @Test
    @EnableH2
    void crud(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        // insert
        final var instance = new MyFlatEntity();
        instance.name = "test";
        database.insert(instance);
        assertEquals(1, count(dataSource));
        assertEquals("MyFlatEntity[id='test', name='test', age=0]", instance.toString());

        // find
        final var firstLookup = database.findById(MyFlatEntity.class, "test");
        assertEquals(instance.toString(), firstLookup.toString());

        // update
        instance.age = 35;
        database.update(instance);
        assertEquals(1, count(dataSource));
        assertEquals(instance.toString(), database.findById(MyFlatEntity.class, "test").toString());

        // insert another one
        final var instance2 = new MyFlatEntity();
        instance2.name = "test2";
        database.insert(instance2);
        assertEquals(2, count(dataSource));

        // delete
        database.delete(instance);
        assertEquals(1, count(dataSource));
        database.delete(instance2);
        assertEquals(0, count(dataSource));
    }

    @Test
    @EnableH2
    void onLoad(final DataSource dataSource) throws SQLException {
        final Database database = init(dataSource);

        final var instance = new MyFlatEntity();
        instance.name = "loaded";
        database.insert(instance);
        assertEquals(1, count(dataSource));
        assertEquals("MyFlatEntity[id='loaded', name='loaded', age=0]", instance.toString());

        final var firstLookup = database.findById(MyFlatEntity.class, "loaded");
        assertEquals("MyFlatEntity[id='loaded', name='loaded', age=1]", firstLookup.toString());

        database.delete(instance);
    }

    private Database init(final DataSource dataSource) throws SQLException {
        final var database = Database.of(new Configuration().setDataSource(dataSource));
        final var entity = database.getOrCreateEntity(MyFlatEntity.class);

        // ddl
        try (final var connection = dataSource.getConnection();
             final var stmt = connection.createStatement()) {
            for (final var sql : entity.ddl()) {
                stmt.execute(sql);
            }
        }

        assertEquals(0, count(dataSource));
        return database;
    }

    private List<String> listTables(final DataSource dataSource) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var stmt = connection.createStatement();
             final var set = stmt.executeQuery("SHOW TABLES")) {
            final var tables = new ArrayList<String>();
            while (set.next()) {
                tables.add(set.getString("TABLE_NAME"));
            }
            return tables;
        }
    }

    private long count(final DataSource dataSource) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var stmt = connection.createStatement();
             final var set = stmt.executeQuery("SELECT count(*) FROM FLAT_ENTITY")) {
            final var tables = new ArrayList<String>();
            if (set.next()) {
                return set.getLong(1);
            }
        }
        throw new IllegalStateException("no count");
    }

    @Table("FLAT_ENTITY")
    public static class MyFlatEntity {
        @Id
        private String id;

        @Column
        private String name;

        @Column
        private int age;

        @OnInsert
        private void init() {
            id = name;
        }

        @OnLoad
        private void load() {
            if ("loaded".equals(name)) {
                age = 1;
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MyFlatEntity.class.getSimpleName() + "[", "]")
                    .add("id='" + id + "'")
                    .add("name='" + name + "'")
                    .add("age=" + age)
                    .toString();
        }

        @Override // should only be "id" but for the test it is convenient
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final MyFlatEntity that = MyFlatEntity.class.cast(o);
            return age == that.age && Objects.equals(id, that.id) && Objects.equals(name, that.name);
        }

        @Override // should only be "id" but for the test it is convenient
        public int hashCode() {
            return Objects.hash(id, name, age);
        }
    }

    @Operation(aliases = @Operation.Alias(alias = "e", type = MyFlatEntity.class))
    public interface MyOps {
        @Statement("select count(*) from ${e#table}")
        long countAll();

        @Statement("select ${e#fields} from ${e#table} order by name")
        List<MyFlatEntity> findAll();

        @Statement("select ${e#fields} from ${e#table} where name = ?")
        MyFlatEntity findOne(String name);

        @Statement("select ${e#fields} from ${e#table} where name = ${parameters#name}")
        MyFlatEntity findOneWithPlaceholders(String name);

        @Statement("select ${e#fields} from ${e#table} where name ${parameters#name#in} order by name")
        List<MyFlatEntity> findByName(List<String> name);

        @Statement("delete from ${e#table} where name like ?")
        int delete(String name);

        @Statement("delete from ${e#table} where name like ?")
        void deleteWithoutReturnedValue(String name);
    }
}
