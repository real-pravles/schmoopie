/*
 * Copyright 2026 Pravles Redneckoff
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pravles.schmoopie;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

class SchmoopieAppTest {
    static Stream<Arguments> scenarios() throws IOException {
        return Stream.of(
                of(
                        "schmoopie: Not a FODP file",
                        new ByteArrayInputStream("foo".getBytes(UTF_8)),
                        "schmoopie: Not a FODP file",
                        ""),
                of(
                        "schmoopie: Empty input",
                        new ByteArrayInputStream("".getBytes(UTF_8)),
                        "schmoopie: Empty input",
                        ""),
                of(
                        "2026_05_24_map.fodp",
                        new FileInputStream("src/test/resources/2026_05_24_map.fodp"),
                        "",
                        FileUtils.readFileToString(new File("src/test/resources/2026_05_24_map.fodp.out.org"), UTF_8) )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void run(final String name,
             final InputStream input,
             final String expectedErr,
             final String expectedOut) {
        // Given

        // When
        final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        try (final PrintStream out = new PrintStream(outBuffer,
                true, UTF_8);
             final PrintStream err = new PrintStream(errBuffer,
                     true, UTF_8)) {
            new SchmoopieApp().run(input, err, out);
        }

        // Then
        final String actualOut = outBuffer.toString(UTF_8);
        final String actualErr = errBuffer.toString(UTF_8);

        assertThat(actualOut)
                .isEqualToIgnoringWhitespace(expectedOut);
        assertThat(actualErr)
                .isEqualToIgnoringWhitespace(expectedErr);
    }
}