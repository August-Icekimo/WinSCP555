/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.serena.air

import java.util.Properties;

class StepPropertiesHelper {
    Properties props
    
    public StepPropertiesHelper(Map aprops){
        props = new Properties()
        props.putAll(aprops)
    }
    
    public String notNull(String paramName) {
        String res = props.getProperty(paramName);
        if (res == null) {
            throw new RuntimeException("Parameter " + paramName + " is missing!");
        }
        return res;
    }

    public boolean notNullBoolean(String paramName) {
        String res = props.getProperty(paramName);
        if (res == null) {
            throw new RuntimeException("Parameter " + paramName + " is missing!");
        }
        return Boolean.parseBoolean(res);
    }

    public String optional(String paramName) {
        return optional(paramName, "");
    }
    
    public String optional(String paramName, String defValue) {
        String res = props.getProperty(paramName);

        return res != null ? res : defValue;
    }

    public int optionalInt(String paramName, int defValue) {
        String res = props.getProperty(paramName);
        if (res == null || res.isEmpty()) {
            return defValue;
        }
        return Integer.parseInt(res);
    }

    public boolean optionalBoolean(String paramName, boolean defValue) {
        String res = props.getProperty(paramName);
        if (res == null || res.isEmpty()) {
            return defValue
        }
        return Boolean.parseBoolean(res);
    }
        
    public def getAt(key){
        props[key]    
    }

    public def putAt(key, value){
        props[key] = value
    } 
    
    public static String[] splitTextArea(String text){
        return text.replaceAll("\\s*(\n|^|\$)\\s*", "\$1").replaceAll("\\s*\n\\s*\n*\\s*", "\n").split("\n")
    }
}
