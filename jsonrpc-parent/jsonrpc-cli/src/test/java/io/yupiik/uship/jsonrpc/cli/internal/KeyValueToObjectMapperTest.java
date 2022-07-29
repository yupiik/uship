/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.jsonrpc.cli.internal;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyValueToObjectMapperTest {
    private final KeyValueToObjectMapper mapper = new KeyValueToObjectMapper();

    @Test
    void caseInsensitiveMapping() {
        assertEquals(
                new Root(null, 0, new Person("simple", 30, new Address("there"), null), null, null),
                mapper.getOrCreate(Root.class).bind(new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
                    put("nEsted.naMe", "simple");
                    put("nesTed.agE", "30");
                    put("nesteD.hEre.sTreet", "there");
                }}));
        // env var case (just _ replaced by .)
        assertEquals(
                new Root(null, 0, new Person("simple", 30, new Address("there"), null), null, asList(
                        new Person("first", 1, new Address("1st"), null),
                        new Person("second", 2, new Address("2nd"), null)
                )),
                mapper.getOrCreate(Root.class).bind(new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
                    put("NESTED.NAME", "simple");
                    put("NESTED.AGE", "30");
                    put("NESTED.HERE.STREET", "there");
                    put("LIST.0.NAME", "first");
                    put("LIST.0.AGE", "1");
                    put("LIST.0.HERE.STREET", "1st");
                    put("LIST.1.NAME", "second");
                    put("LIST.1.AGE", "2");
                    put("LIST.1.HERE.STREET", "2nd");
                }}));
        assertEquals(
                new People(new LinkedHashMap<String, Person>() {{
                    put("first", new Person("uno", 1, null, null));
                    put("second", new Person("dos", 2, null, null));
                }}),
                mapper.getOrCreate(People.class).bind(new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
                    put("PEOPLE.0.KEY", "first");
                    put("PEOPLE.0.VALUE.NAME", "uno");
                    put("PEOPLE.0.VALUE.AGE", "1");
                    put("PEOPLE.1.KEY", "second");
                    put("PEOPLE.1.VALUE.NAME", "dos");
                    put("PEOPLE.1.VALUE.AGE", "2");
                }}));
    }

    @Test
    void primitive() {
        assertEquals(
                new Root("yes", 1234, null, null, null),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("string", "yes");
                    put("integer", "1234");
                }}));
    }

    @Test
    void primitiveDefaults() {
        assertEquals(
                new Root(null, 0, null, null, null),
                mapper.getOrCreate(Root.class).bind(emptyMap()));
    }

    @Test
    void nested() {
        assertEquals(
                new Root(null, 0, new Person("simple", 30, new Address("there"), null), null, null),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("nested.name", "simple");
                    put("nested.age", "30");
                    put("nested.here.street", "there");
                }}));
    }

    @Test
    void complex() {
        assertEquals(
                new Root(null, 0, null, new Person[]{
                        new Person("first", 1, new Address("1st"), null),
                        new Person("second", 2, new Address("2nd"), null)
                }, null),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("array.0.name", "first");
                    put("array.0.age", "1");
                    put("array.0.here.street", "1st");
                    put("array.1.name", "second");
                    put("array.1.age", "2");
                    put("array.1.here.street", "2nd");
                }}));
    }

    @Test
    void array() {
        assertEquals(
                new Root(null, 0, null, new Person[]{
                        new Person(null, 0, null, asList(new Address("a1"), new Address("a2")))
                }, null),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("array.0.otherAddresses.0.street", "a1");
                    put("array.0.otherAddresses.1.street", "a2");
                }}));
    }

    @Test
    void list() {
        assertEquals(
                new Root(null, 0, null, null, asList(
                        new Person("first", 1, new Address("1st"), null),
                        new Person("second", 2, new Address("2nd"), null)
                )),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("list.0.name", "first");
                    put("list.0.age", "1");
                    put("list.0.here.street", "1st");
                    put("list.1.name", "second");
                    put("list.1.age", "2");
                    put("list.1.here.street", "2nd");
                }}));
    }

    @Test
    void arrayLimitedSize() {
        assertEquals(
                new Root(null, 0, null, new Person[]{
                        new Person("first", 1, new Address("1st"), null)
                }, null),
                mapper.getOrCreate(Root.class).bind(new HashMap<String, String>() {{
                    put("array.length", "1");
                    put("array.0.name", "first");
                    put("array.0.age", "1");
                    put("array.0.here.street", "1st");
                    put("array.1.name", "second");
                    put("array.1.age", "2");
                    put("array.1.here.street", "2nd");
                }}));
    }

    @Test
    void map() {
        assertEquals(
                new People(new LinkedHashMap<String, Person>() {{
                    put("first", new Person("uno", 1, null, null));
                    put("second", new Person("dos", 2, null, null));
                }}),
                mapper.getOrCreate(People.class).bind(new HashMap<String, String>() {{
                    put("people.0.key", "first");
                    put("people.0.value.name", "uno");
                    put("people.0.value.age", "1");
                    put("people.1.key", "second");
                    put("people.1.value.name", "dos");
                    put("people.1.value.age", "2");
                }}));
    }

    @Test
    void mapLimitedSize() {
        assertEquals(
                new People(singletonMap("first", new Person("uno", 1, null, null))),
                mapper.getOrCreate(People.class).bind(new HashMap<String, String>() {{
                    put("people.length", "1");
                    put("people.0.key", "first");
                    put("people.0.value.name", "uno");
                    put("people.0.value.age", "1");
                    put("people.1.key", "second");
                    put("people.1.value.name", "dos");
                    put("people.1.value.age", "2");
                }}));
    }

    @Test
    void setter() {
        assertEquals(
                new Address("success"),
                mapper.getOrCreate(Address.class).bind(singletonMap("value", "success")));
    }

    public static class People {
        private Map<String, Person> people;

        public People() {
            // no-op
        }

        public People(final Map<String, Person> people) {
            this.people = people;
        }

        public Map<String, Person> getPeople() {
            return people;
        }

        public People setPeople(final Map<String, Person> people) {
            this.people = people;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final var people1 = (People) o;
            return people.equals(people1.people);
        }

        @Override
        public int hashCode() {
            return Objects.hash(people);
        }
    }

    public static class Root {
        private String string;
        private int integer;

        private Person nested;

        private Person[] array;
        private List<Person> list;

        public Root() {
            // no-op
        }

        public Root(final String string, final int integer, final Person nested,
                    final Person[] array, final List<Person> list) {
            this.string = string;
            this.integer = integer;
            this.nested = nested;
            this.array = array;
            this.list = list;
        }

        public String getString() {
            return string;
        }

        public Root setString(final String string) {
            this.string = string;
            return this;
        }

        public int getInteger() {
            return integer;
        }

        public Root setInteger(final int integer) {
            this.integer = integer;
            return this;
        }

        public Person getNested() {
            return nested;
        }

        public Root setNested(final Person nested) {
            this.nested = nested;
            return this;
        }

        public Person[] getArray() {
            return array;
        }

        public Root setArray(final Person[] array) {
            this.array = array;
            return this;
        }

        public List<Person> getList() {
            return list;
        }

        public Root setList(final List<Person> list) {
            this.list = list;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final var root = (Root) o;
            return integer == root.integer &&
                    Objects.equals(string, root.string) &&
                    Objects.equals(nested, root.nested) &&
                    Arrays.equals(array, root.array) &&
                    Objects.equals(list, root.list);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(string, integer, nested, list);
            result = 31 * result + Arrays.hashCode(array);
            return result;
        }
    }

    public static class Person {
        private String name;
        private int age;
        private Address here;
        private List<Address> otherAddresses;

        public Person() {
            // no-op
        }

        public Person(final String name, final int age, final Address here, final List<Address> otherAddresses) {
            this.name = name;
            this.age = age;
            this.here = here;
            this.otherAddresses = otherAddresses;
        }

        public String getName() {
            return name;
        }

        public Person setName(final String name) {
            this.name = name;
            return this;
        }

        public int getAge() {
            return age;
        }

        public Person setAge(final int age) {
            this.age = age;
            return this;
        }

        public Address getHere() {
            return here;
        }

        public Person setHere(final Address here) {
            this.here = here;
            return this;
        }

        public List<Address> getOtherAddresses() {
            return otherAddresses;
        }

        public Person setOtherAddresses(final List<Address> otherAddresses) {
            this.otherAddresses = otherAddresses;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final var person = (Person) o;
            return age == person.age &&
                    Objects.equals(name, person.name) &&
                    Objects.equals(here, person.here) &&
                    Objects.equals(otherAddresses, person.otherAddresses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, here, otherAddresses);
        }
    }

    public static class Address {
        private String street;

        public Address() {
            // no-op
        }

        public Address(final String street) {
            this.street = street;
        }

        public String getStreet() {
            return street;
        }

        public void setValue(final String s) {
            street = s;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final var address = (Address) o;
            return street.equals(address.street);
        }

        @Override
        public int hashCode() {
            return Objects.hash(street);
        }
    }
}

