/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.urbancode.air

import java.security.Provider;
import java.security.Security;

public class XTrustProvider extends Provider {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    final static private long serialVersionUID = 1L;

    //----------------------------------------------------------------------------------------------
    static public void install() {
        if (Security.getProvider(XTrustProvider.class.getSimpleName()) == null) {
            Security.insertProviderAt(new XTrustProvider(), 2);
            Security.setProperty("ssl.TrustManagerFactory.algorithm", "XTrust509");
        }
    }

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************

    //----------------------------------------------------------------------------------------------
    public XTrustProvider() {
        super(XTrustProvider.class.getSimpleName(), 1D,
                "Basic XTrustProvider ignoring invalid certificates");

        put("TrustManagerFactory.XTrust509", XTrustManagerFactory.class.getName());
    }
}
