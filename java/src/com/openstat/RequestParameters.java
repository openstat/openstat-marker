package com.openstat;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parses an URI query string, decodes request parameters, and creates a
 * parameter map. This version is based on original Apache Cocoon
 * implementation (org.apache.cocoon.servletservice.util.RequestParameters),
 * with a few changes:
 * <ul>
 * <li>It works with byte arrays internally, not strings; helper methods
 * return strings from these byte arrays.</li>
 * <li>It uses proper generics, so it ensures stricter type checking.</li>
 * </ul>
 */
public final class RequestParameters {

    /**
     * The parameter names are the keys and the value is a String array object
     */
    private final Map<String, byte[]> values;

    /**
     * Construct a new object from a queryString
     *
     * @param queryString request query string
     */
    public RequestParameters(String queryString) {
        this.values = new HashMap<String, byte[]>(5);

        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String pair = st.nextToken();
                int pos = pair.indexOf('=');
                if (pos != -1) {
                    setParameter(new String(decode(pair.substring(0, pos)), CHARSET_UTF8),
                                 decode(pair.substring(pos + 1, pair.length())));
                } else {
                    setParameter(new String(decode(pair), CHARSET_UTF8), new byte[] {});
                }
            }
        }
    }

    /**
     * Add a parameter.
     * The parameter is added with the given value.
     *
     * @param name   The name of the parameter.
     * @param value  The value of the parameter.
     */
    private void setParameter(String name, byte[] value) {
        this.values.put(name, value);
    }

    /**
     * Get the value of a parameter.
     *
     * @param name   The name of the parameter.
     * @return       The value of the first parameter with the name
     *               or <CODE>null</CODE>
     */
    public String getParameter(String name) {
        return getParameter(name, null);
    }

    /**
     * Get the value of a parameter.
     *
     * @param name   The name of the parameter.
     * @param defaultValue The default value if the parameter does not exist.
     * @return       The value of the first parameter with the name
     *               or <CODE>defaultValue</CODE>
     */
    public String getParameter(String name, String defaultValue) {
        byte[] value = this.values.get(name);
        return (value != null) ? new String(value, CHARSET_UTF8) : defaultValue;
    }

    /**
     * Get map of parameter names to String array values
     *
     * @return map of parameter values
     */
    public Map<String, byte[]> getParameterMap() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * Decodes URL encoded string. Supports decoding of '+', '%XX' encoding,
     * and '%uXXXX' encoding.
     *
     * @param s URL encoded string
     * @return decoded string
     */
    private static byte[] decode(String s) {
        ByteBuffer bb = ByteBuffer.allocate(s.length() * 4);

        final int length = s.length();
        for (int i = 0; i < length; ) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    bb.put((byte) ' ');
                    i++;
                    break;

                case '%':
                    // Check if this is a non-standard %u9999 encoding
                    // (see COCOON-1950)
                    try {
                        if (s.charAt(i + 1) == 'u') {
                            bb.put(charToUtf8Bytes((char) Integer.parseInt(s.substring(i + 2, i + 6), 16)));
                            i += 6;
                        } else {
                            bb.put((byte) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                            i += 3;
                        }
                    } catch (NumberFormatException e) {
                        bb.put(charToUtf8Bytes(c));
                        i++;
                    } catch (StringIndexOutOfBoundsException e) {
                        bb.put(charToUtf8Bytes(c));
                        i++;
                    }
                    break;

                default:
                    bb.put(charToUtf8Bytes(c));
                    i++;
                    break;
            }
        }
        return Arrays.copyOfRange(bb.array(), 0, bb.position());
    }

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    private static byte[] charToUtf8Bytes(char x) {
        return new String(new char[] { x }).getBytes(CHARSET_UTF8);
    }

    /**
     * Get the value of a parameter as long.
     *
     * @param name The name of the parameter.
     * @param defaultValue Default value, if parameter is not found or is not numeric.
     */
    public long getLong(String name, long defaultValue) {
        byte[] value = this.values.get(name);
        if (value == null)
            return defaultValue;
        try {
            return Long.parseLong(new String(value, CHARSET_UTF8));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
