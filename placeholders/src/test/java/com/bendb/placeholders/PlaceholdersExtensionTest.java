/*
 * Copyright 2017 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.placeholders;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PlaceholdersExtensionTest {
    @Test
    public void placeholdersIsNonNullByDefault() {
        PlaceholdersExtension ext = new PlaceholdersExtension();
        assertThat(ext.getPlaceholders(), not(nullValue()));
    }

    @Test
    public void replaceAddsPlaceholderToMap() {
        PlaceholdersExtension ext = new PlaceholdersExtension();
        ext.replace("foo", "bar");

        assertThat(ext.getPlaceholders(), equalTo(Collections.singletonMap("foo", "bar")));
    }

    @Test
    public void placeholdersReturnValueCanBeMutatedWithoutModifyingExtension() {
        PlaceholdersExtension ext = new PlaceholdersExtension();
        ext.replace("foo", "bar");

        Map<String, String> x = ext.getPlaceholders();
        x.put("foo", "quux");

        assertThat(x, not(equalTo(ext.getPlaceholders())));
    }
}