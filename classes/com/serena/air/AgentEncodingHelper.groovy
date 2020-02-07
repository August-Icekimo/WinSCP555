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
