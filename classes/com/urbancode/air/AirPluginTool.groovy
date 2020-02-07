package com.urbancode.air;

import org.apache.commons.codec.binary.Base64;

public class AirPluginTool {

    //**************************************************************************
    // CLASS
    //**************************************************************************

    //**************************************************************************
    // INSTANCE
    //**************************************************************************
    
    final public def isWindows = (System.getProperty('os.name') =~ /(?i)windows/).find()

    def out = System.out;
    def err = System.err;

    private def inPropsFile;
    private def outPropsFile;

    def outProps;

    public AirPluginTool(def inFile, def outFile){
        inPropsFile = inFile;
        outPropsFile = outFile;
        outProps = new Properties();
    }

    public Properties getStepProperties() {
        def props = new Properties();
        final def inputPropsFile = this.inPropsFile;
        final def inputPropsStream = null;
        try {
            inputPropsStream = new FileInputStream(inputPropsFile);
            props.load(inputPropsStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            inputPropsStream.close();
        }
        return props;
    }

    public HashMap getStepPropertiesMap() {
        def map = new HashMap<String, Object>()

        getStepProperties().each() { key, value ->
            map.put(key, deSerialize(value))
        };
        return map
    }

    public void setOutputProperty(String name, Object value) {
        this.outProps.setProperty(name, serialize(value));
    }

    public void setOutputProperties(Properties properties) {
        for (Object key : properties.keySet()) {
            String propName = (String) key;
            String propValue = properties.getProperty(propName);
            setOutputProperty(propName, propValue);
        }
    }

    public void setOutputProperties() {
        final OutputStream outputPropsStream = null;
        try {
            outputPropsStream = new FileOutputStream(this.outPropsFile);
            outProps.store(outputPropsStream, "");
        }
        finally {
            if (outputPropsStream != null) {
                outputPropsStream.close();
            }   
        }
    }

    public String getAuthToken() {
        String authToken = System.getenv("AUTH_TOKEN");
        return "{\"token\" : \"" + authToken + "\"}";
    }

    public String getAuthTokenUsername() {
        return "PasswordIsAuthToken";
    }

    public void storeOutputProperties() {
        setOutputProperties();
    }

    private String serialize(Object object) throws IOException {
        String retval;
        if (object instanceof String || object instanceof Number) {
            retval = String.valueOf(object);
        } else {
            ByteArrayOutputStream aout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(aout);
            out.writeObject(object);
            out.close();
            retval = new String(Base64.encodeBase64(aout.toByteArray()));
        }
        return retval;
    }

    public Object deSerialize(String value) throws IOException {
        Object retval = value;
        try {
            ByteArrayInputStream ain = new ByteArrayInputStream(Base64.decodeBase64(value));
            ObjectInputStream inputStream = new ObjectInputStream(ain);
            retval = inputStream.readObject();
        }
        catch (Exception e) {
        }
        return retval;
    }

}
