/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.serena.air

import java.nio.charset.Charset

class AgentEncodingHelper {

    public static final String AGENT_FILE_ENCODING = "AGENT_FILE_ENCODING"
    public static final String AGENT_CONSOLE_ENCODING = "AGENT_CONSOLE_ENCODING"
    public static final String UTF_8 = "UTF-8"

    public static final HashMap<String, String> encodingAliases = new HashMap<>();
    static {
        encodingAliases.put("cp932", "MS932");
    }

    String agentFileEncoding
    String agentConsoleEncoding
    Boolean isDebug = false;

    public AgentEncodingHelper() {
        this(false)
    }

    public AgentEncodingHelper(Boolean isDebug) {
        this.isDebug = isDebug;
        agentFileEncoding = System.getenv().get(AGENT_FILE_ENCODING) ? System.getenv().get(AGENT_FILE_ENCODING) : UTF_8;
        agentConsoleEncoding = System.getenv().get(AGENT_CONSOLE_ENCODING) ? System.getenv().get(AGENT_CONSOLE_ENCODING) : agentFileEncoding;

        agentFileEncoding = prepareEncoding(agentFileEncoding);
        agentConsoleEncoding = prepareEncoding(agentConsoleEncoding);

        try {
            tryEncoding(agentFileEncoding);
        } catch (Exception e) {
            agentFileEncoding = UTF_8;
        }

        try {
            tryEncoding(agentConsoleEncoding);
        } catch (Exception e) {
            agentConsoleEncoding = agentFileEncoding;
        }

        if (isDebug) {
            println "agentFileEncoding = " + agentFileEncoding
            println "agentConsoleEncoding = " + agentConsoleEncoding
        }
    }

    String getAgentFileEncoding() {
        return agentFileEncoding
    }

    String getAgentConsoleEncoding() {
        return agentConsoleEncoding
    }

    private void tryEncoding(String encoding) throws Exception {
        try {
            Charset.forName(encoding)
        } catch (Exception e) {
            if (isDebug) {
                println "Couldn't get encoding for name = " + encoding;
            }
            throw e;
        }
    }

    private String prepareEncoding(String encoding) {
        return encodingAliases.get(encoding, encoding);
    }
}
