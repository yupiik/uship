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
package io.yupiik.uship.jsonrpc.cli.api;

import io.yupiik.uship.jsonrpc.cli.test.CliSupport;
import io.yupiik.uship.jsonrpc.cli.test.CliTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CliSupport
class JsonRpcCliExecutorTest {
    @CliTest(command = "foo", stderr = CliTest.IGNORE_STREAM)
    void unknownCommand(@StdErr final CliSupport.ResettableStream stderr) {
        assertHelp(stderr);
    }

    @CliTest(command = {"obj", "--name", "test"}, stdout = "\n  name: test\n")
    void obj(@StdErr final CliSupport.ResettableStream stderr) {
    }

    @CliTest(command = {}, stderr = CliTest.IGNORE_STREAM)
    void noCommand(@StdErr final CliSupport.ResettableStream stderr) {
        assertHelp(stderr);
    }

    @CliTest(command = {"sample-to-string", "--first"}, stderr = "Arguments parity should be pair (name + value): [--first]\n")
    void wrongArgParity() {
    }

    @CliTest(command = {
            "sample-to-string",
            "--first", "sample",
            "--second", "1234",
            "--third", "12.34",
            "--fourth.0", "s1",
            "--fourth.1", "s2",
            "--fifth.name", "nested",
            "--sixth.0.name", "nested-in-list1",
            "--sixth.1.name", "nested-in-list2"
    }, stdout = "sample,1234,12.34,[s1, s2],nested,[nested-in-list1, nested-in-list2]\n")
    void parameterMapping() {
    }

    @CliTest(command = {
            "sample-to-string",
            "--cli-env", "src/test/resources/clienv.properties"
    }, stdout = "sample,1234,12.34,[s1, s2],nested,[nested-in-list1, nested-in-list2]\n")
    void cliEnv() {
    }

    @CliTest(command = {
            "sample-to-string",
            "--cli-response-dump", "target/test/command-executor/cliResponseDump.properties",
            "--first", "sample",
            "--second", "1234",
            "--third", "12.34",
            "--fourth.length", "0",
            "--sixth.length", "0"
    }, stdout = CliTest.IGNORE_STREAM)
    void cliResponseDump() throws IOException {
        final var properties = new Properties();
        try (final var reader = Files.newBufferedReader(Paths.get("target/test/command-executor/cliResponseDump.properties"))) {
            properties.load(reader);
        }
        assertEquals("sample,1234,12.34,[],null,[]", properties.getProperty("value"));
    }

    private void assertHelp(final CliSupport.ResettableStream stderr) {
        assertEquals("" +
                "Yupiik JSON-RPC Cli Help:\n" +
                "\n" +
                "Commands:\n" +
                "\n" +
                "  help:\n" +
                "\n" +
                "    Show help (available commands, options).\n" +
                "\n" +
                "    --command (String): Filter the documentation for a single command.\n" +
                "    --format (HelpFormat): Output format (TEXT, ADOC)\n" +
                "\n" +
                "  obj:\n" +
                "\n" +
                "    \n" +
                "\n" +
                "    --name (String): \n" +
                "\n" +
                "  openrpc:\n" +
                "\n" +
                "    Returns the Open-RPC specification.\n" +
                "\n" +
                "\n" +
                "\n" +
                "  sample-to-string:\n" +
                "\n" +
                "    \n" +
                "\n" +
                "    --fifth (Foo - JSON object): \n" +
                "    --fifth-name (object): null\n" +
                "    --first (String): \n" +
                "    --fourth-<index> (List<String>): \n" +
                "    --second (Long): \n" +
                "    --sixth-<index> (List<Foo> - JSON array): \n" +
                "    --sixth-<index>-name (object): null\n" +
                "    --third (Double): \n" +
                "\n" +
                "\n" +
                "Options syntax:\n" +
                "\n" +
                "  - List can be specified expanding the option name with an index, for example '--my-list' will specify values using '--my-list-0', '--my-list-1', etc...\n" +
                "  - Objects can be specified expanding the option name with suboption names, for example an object '--my-object' with a name attribute will specify the name with '--my-object-name'\n" +
                "  - Maps follow the list pattern suffixed with '-key' and '-value', for instance to set [a:b] for the option 'my-map', you will set '--my-map-0-key a --my-map-0-value b\n" +
                "  - A file content can be injected in an option prefixing it with '@', for example '--my-json @content.json', if you really want to pass the value '@content.json' you need to escape the '@' with another '@': '@@content.json' will inject '@content.json' value.\n" +
                "\n" +
                "Global options:\n" +
                "\n" +
                "  All commands support the following additional options:\n" +
                "\n" +
                "  --cli-env: takes a properties file as parameter containing options merged with command line ones. It enables to save commands to reexecute them easily.\n" +
                "  --cli-response-dump: take a properties file path as value and triggers a dump of a successful command response as properties (useful to chain commands more easily for example in a script).\n" +
                "  --cli-response-dump-delete-on-exit: if set to true, the dump deleted - if created with success - when the CLI exits.\n" +
                "  --cli-silent: if set to true, the command will not output sucess response (useful in batches).\n" +
                "\n", stderr.asString());
    }
}
