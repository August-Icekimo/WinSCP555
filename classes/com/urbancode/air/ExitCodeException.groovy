/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.urbancode.air

class ExitCodeException extends Exception {
    /**
    * Construct a new ExitCodeException.
    */
   public ExitCodeException() {
       super();
   }

   /**
    * Construct a new ExitCodeException with the provided message.
    *
    * @param message A brief description of this exception.
    */
   public ExitCodeException(String message) {
       super(message);
   }

   /**
    * Construct a new ExitCodeException instance with the provided cause.
    *
    * @param cause The underlying cause of this exception.
    */
   public ExitCodeException(Throwable cause) {
       super(cause);
   }

   /**
    * Construct a new ExitCodeException instance with the provided message and cause.
    *
    * @param message A brief description of the exception.
    * @param cause The underlying exception which caused this exception to be emitted.
    */
   public ExitCodeException(String message, Throwable cause) {
       super(message, cause);
   }
}