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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class SchmoopieApp {
    private static final IsFodp IS_FODP =
            new IsFodp();
    private static final Fodp2Org FODP_2_ORG =
            new Fodp2Org();

    public void run(final InputStream in,
                    final PrintStream err,
                    final PrintStream out) {

        try {
            final String input = IOUtils.toString(in, StandardCharsets.UTF_8);
            final Outcome vo = validate(input);

            if (!vo.isValid()) {
                err.println(vo.getMessage());
                return;
            }

            out.println(FODP_2_ORG.apply(input, err));
        } catch (final IOException e) {
            err.println("An error occurred while reading the input");
            e.printStackTrace(err);
        }
    }

    private Outcome validate(final String input) {
        if (StringUtils.isBlank(input)) {
            return Outcome.builder()
                    .message("schmoopie: Empty input")
                    .valid(false)
                    .build();
        }

        if (!IS_FODP.apply(input)) {
            return Outcome.builder()
                    .message("schmoopie: Not a FODP file")
                    .valid(false)
                    .build();
        }

        return Outcome.builder()
                .valid(true)
                .build();
    }

    public static void main(final String[] args) {
        new SchmoopieApp().run(System.in, System.err, System.out);
    }
}
