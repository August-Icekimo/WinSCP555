/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.urbancode.air

import java.text.SimpleDateFormat

public class DateParser {

    private final def DATE_FORMATS = [
              new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy"), // java.util.Date.toString()
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),      // ISO-like format
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z"),    // GIT-like

              new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy"),
              new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SZ"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
              new SimpleDateFormat("yyyy-MM-dd HH:mmZ"),
              new SimpleDateFormat("yyyy-MM-dd HH:mm"),

              // Old CVS formats
              new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
              new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z"),
              new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z"),

              // XML-RPC spec compliant iso8601 formats
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ"),
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S"),
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ"),
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
              new SimpleDateFormat("yyyy-MM-dd"),
              new SimpleDateFormat("yyyyMMdd"),
          ] as SimpleDateFormat[];

    public DateParser() {
    }

    public Date parseDate(String value) {
        if (!value) {
            return null
        }
        if (value.isLong()) {
            return new Date(value.toLong())
        }
        else {
            for (def format in DATE_FORMATS) {
                try {
                    // return the first result which works
                    return format.parse(value)
                }
                catch (java.text.ParseException e) {
                }
            }
            throw new IllegalArgumentException("Unrecognized date format $value")
        }
    }
}