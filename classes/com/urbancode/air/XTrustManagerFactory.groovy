/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.urbancode.air

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;

public class XTrustManagerFactory extends TrustManagerFactorySpi {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************

    //------------------------------------------------------------------------------------------
    public XTrustManagerFactory() {
    }

    //------------------------------------------------------------------------------------------
    @Override
    protected void engineInit(KeyStore keystore) {
    }

    //------------------------------------------------------------------------------------------
    @Override
    protected void engineInit(ManagerFactoryParameters managerParams)
    throws InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException(
                "XTrustManagerFactory cannot be initialized using ManagerFactoryParameters");
    }

    //------------------------------------------------------------------------------------------
    @Override
    protected TrustManager[] engineGetTrustManagers() {
        List<TrustManager> managerList = new ArrayList<TrustManager>();
        managerList.add(new OpenX509TrustManager());

        return managerList.toArray(new TrustManager[managerList.size()]);
    }
}
