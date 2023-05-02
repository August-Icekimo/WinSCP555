/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

/**
 *
 */
package com.urbancode.air.plugin.teamforge

import groovy.lang.Closure;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author jwa
 *
 */
public class FileSet {

    //**************************************************************************
    // CLASS
    //**************************************************************************

    //**************************************************************************
    // INSTANCE
    //**************************************************************************

    def isWindows = (System.getProperty('os.name') =~ /(?i)windows/).find()
    def base
    def includes = []
    def excludes = []

    public FileSet(base) {
        if (base instanceof File) {
            this.base = base
        }
        else if (base == null) {
            this.base = new File('.').absolutePath
        }
        else {
            this.base = new File(base)
        }
    }

    public def include(antPattern) {
        forNonEmptyLines(antPattern) {
            includes << convertToPattern(it)
        }
    }

    public def exclude(antPattern) {
        forNonEmptyLines(antPattern) {
            excludes << convertToPattern(it)
        }
    }

    public def each(Closure closure) {
        base.eachFileRecurse { file ->
            def path = file.path.replace('\\', '/').substring(base.path.length())
            def matches = false
            for (p in includes) {
                if (path =~ p) {
                    matches = true
                    break;
                }
            }
            if (matches) {
                for (p in excludes) {
                    if (path =~ p) {
                        matches = false;
                        break;
                    }
                }
            }
            if (matches) {
                closure(file)
            }
        }
    }

    public def files() {
        def list = []
        each { list << it }
        return list
    }

    private forNonEmptyLines(strings, Closure closure) {
        if (strings instanceof Collection) {
            strings.each {
                def trimmed = it?.trim()
                if (trimmed?.length() > 0) {
                    closure(it)
                }
            }
        }
        else if (strings != null) {
            forNonEmptyLines(strings.readLines(), closure)
        }
    }

    private convertToPattern(antPattern) {
        // normalize file separator in pattern
        antPattern = antPattern.replace('\\', '/');

        // ensure leading / character from pattern
        def pattern = antPattern.startsWith('/') ? antPattern : '/'+antPattern

        // deal with special regex-characters that should be interpreted as literals
        '\\.+[]^${}|()'.toCharArray().each{ c ->
            pattern = pattern.replace(''+c, '\\'+c)
        }
        pattern = pattern.replace('?', '.') // ? is a single-char wildcard

        // deal with ant-style wildcards
        StringBuffer result = new StringBuffer()
        result.append("^")
        def m = (pattern =~ '\\*\\*/|\\*\\*|\\*')
        while (m) {
            def token = m.group()
            def replacement;
            if (token == '**/') {
                replacement = '.*(?<=/)'
            }
            else if (token == '**') {
                replacement = '.*'
            }
            else {
                replacement = '[^/]*'
            }
            m.appendReplacement(result, Matcher.quoteReplacement(replacement))
        }
        m.appendTail(result)
        result.append("\$")
        def flags = 0
        if (isWindows) {
            flags |= Pattern.CASE_INSENSITIVE
        }
        return Pattern.compile(result.toString(), flags)
    }
}
