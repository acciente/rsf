/*
 * Copyright 2009-2017, Acciente LLC
 *
 * Acciente LLC licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.acciente.oacc.encryptor;

import com.acciente.oacc.sql.SQLAccessControlContextFactory;
import com.acciente.oacc.encryptor.jasypt.JasyptPasswordEncryptor;

/**
 * This interface is used to configure the password encryption scheme used by the built-in authentication provider
 * ({@link com.acciente.oacc.sql.internal.SQLPasswordAuthenticationProvider}). In OACC v2.0.0-rc.7 and prior the
 * built-in authentication provider used Jasypt internally and did not allow other options. The factory methods in
 * {@link SQLAccessControlContextFactory} which use the built-in authentication provider now take an explicit parameter
 * that specifies an implementation of this interface.
 * <p><p>
 * The following password encryptor implementations are built-in:
 * <p>
 * {@link JasyptPasswordEncryptor} -- uses Jasypt use {@link JasyptPasswordEncryptor#getPasswordEncryptor()} to get
 * an instance.
 * <p><p>
 * Compatibility notes:
 * <p>
 * The {@link JasyptPasswordEncryptor} is designed to be fully compatible with existing OACC v2.0.0-rc.7 deployments.
 * This new Jasypt implementation writes a header prefix to new hashes that it creates, but supports checking passwords
 * on hashes that do not contain the header prefix.
 */
public interface PasswordEncryptor {
   /**
    * Encrypts a password.
    *
    * @param password the plaintext password as a cleanable char[]
    * @return the BASE-64 digest of encrypting the specified password
    */
   String encryptPassword(char[] password);


   /**
    * Checks an unencrypted password against an encrypted one to see if they match.
    *
    * @param plainPassword     the plaintext password as a cleanable char[]
    * @param encryptedPassword the (BASE-64) digest from an earlier encryption against which to check the plaintext password
    * @return true if passwords match, false otherwise
    */
   boolean checkPassword(char[] plainPassword,
                         String encryptedPassword);

}