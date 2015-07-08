/*
 * Copyright 2009-2015, Acciente LLC
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
package com.acciente.oacc;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestAccessControl_revokeResourcePermissions extends TestAccessControlBase {
   @Test
   public void revokeResourcePermissions_validAsSystemResource() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(
            true));

      Set<ResourcePermission> permissions_pre = new HashSet<>();
      permissions_pre.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      permissions_pre.add(ResourcePermissions.getInstance(customPermissionName));
      
      accessControlContext.setResourcePermissions(accessorResource, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource),
                 is(permissions_pre));

      // revoke permissions and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                     ResourcePermissions.getInstance(customPermissionName));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource),
                 is(permissions_pre));

      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                           ResourcePermissions.getInstance(customPermissionName)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_ungrantedPermissions_shouldSucceed() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(
            true));

      // revoke permissions and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                     ResourcePermissions.getInstance(customPermissionName));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(
            accessorResource,
            accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                           ResourcePermissions.getInstance(customPermissionName)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_ungrantedPermissions_withAndWithoutGrant_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String grantablePermissionName = generateResourceClassPermission(resourceClassName);
      final String ungrantablePermissionName = ResourcePermissions.INHERIT;
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantablePermissionName, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource), is(
            grantorResourcePermissions));

      // authenticate grantor resource
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(grantablePermissionName),
                                                        ResourcePermissions.getInstance(ungrantablePermissionName));
         fail("revoking permissions without grant-authorization should have failed");
      }
      catch (NotAuthorizedException e) {
         assertThat(e.getMessage().toLowerCase(), containsString(ungrantablePermissionName.toLowerCase()));
         assertThat(e.getMessage().toLowerCase(), not(containsString(grantablePermissionName)));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions.getInstance(grantablePermissionName),
                                                              ResourcePermissions
                                                                    .getInstance(ungrantablePermissionName)));
         fail("revoking permissions without grant-authorization should have failed");
      }
      catch (NotAuthorizedException e) {
         assertThat(e.getMessage().toLowerCase(), containsString(ungrantablePermissionName.toLowerCase()));
         assertThat(e.getMessage().toLowerCase(), not(containsString(grantablePermissionName)));
      }
   }

   @Test
   public void revokeResourcePermissions_ungrantedResetCredentialsPermissionOnUnauthenticatables_shouldFail() {
      authenticateSystemResource();
      final Resource accessorResource = generateAuthenticatableResource(generateUniquePassword());
      final Resource accessedResource = generateUnauthenticatableResource();
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // attempt to revoke *RESET_CREDENTIALS system permission
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions
                                                              .getInstance(ResourcePermissions.RESET_CREDENTIALS));
         fail("revoking reset-credentials permission for unauthenticatable resource should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource"));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.RESET_CREDENTIALS)));
         fail("revoking reset-credentials permission for unauthenticatable resource should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource"));
      }
   }

   @Test
   public void revokeResourcePermissions_ungrantedImpersonatePermissionOnUnauthenticatables_shouldFail() {
      authenticateSystemResource();
      final Resource accessorResource = generateAuthenticatableResource(generateUniquePassword());
      final Resource accessedResource = generateUnauthenticatableResource();
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // attempt to revoke *RESET_CREDENTIALS system permission
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions
                                                              .getInstance(ResourcePermissions.IMPERSONATE));
         fail("revoking impersonate permission for unauthenticatable resource should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource"));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.IMPERSONATE)));
         fail("revoking impersonate permission for unauthenticatable resource should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource"));
      }
   }

   @Test
   public void revokeResourcePermissions_validAsAuthorized() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup accessor permissions
      Set<ResourcePermission> permissions_pre = new HashSet<>();
      permissions_pre.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      permissions_pre.add(ResourcePermissions.getInstance(customPermissionName));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource),
                 is(permissions_pre));

      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource),
                 is(permissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT, true));
      grantorResourcePermissions.add(ResourcePermissions.getInstance(customPermissionName, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource), is(
            grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      grantQueryPermission(grantorResource, accessorResource2);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                     ResourcePermissions.getInstance(customPermissionName));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                           ResourcePermissions.getInstance(customPermissionName)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_reRevokePermissions() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup accessor permissions
      Set<ResourcePermission> permissions_pre = new HashSet<>();
      permissions_pre.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      permissions_pre.add(ResourcePermissions.getInstance(customPermissionName));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(
            permissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT, true));
      grantorResourcePermissions.add(ResourcePermissions.getInstance(customPermissionName, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource), is(
            grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                     ResourcePermissions.getInstance(customPermissionName));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // revoke permissions again and check nothing changed
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                     ResourcePermissions.getInstance(customPermissionName));

      final Set<ResourcePermission> permissions_post2 = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post2.isEmpty(), is(true));

      // revoke permissions again via set-based version and check nothing changed
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                           ResourcePermissions.getInstance(customPermissionName)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_revokeSubsetOfPermissions() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup accessor permissions
      Set<ResourcePermission> permissions_pre = new HashSet<>();
      permissions_pre.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      permissions_pre.add(ResourcePermissions.getInstance(customPermissionName));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(
            permissions_pre));

      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource), is(
            permissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT, true));
      grantorResourcePermissions.add(ResourcePermissions.getInstance(customPermissionName, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource), is(
            grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      grantQueryPermission(grantorResource, accessorResource2);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(customPermissionName));

      Set<ResourcePermission> permissions_expected = new HashSet<>();
      permissions_expected.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post, is(permissions_expected));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(customPermissionName)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource),
                 is(permissions_expected));
   }

   @Test
   public void revokeResourcePermissions_withUnauthorizedPermissionsGrantedElsewhere_shouldFailAsAuthorized() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String grantablePermissionName = generateResourceClassPermission(resourceClassName);
      final String ungrantablePermissionName = ResourcePermissions.INHERIT;
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup accessor permissions
      Set<ResourcePermission> accessorPermissions_pre
            = setOf(ResourcePermissions.getInstance(grantablePermissionName),
                    ResourcePermissions.getInstance(ungrantablePermissionName));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(accessorPermissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantablePermissionName, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource), is(grantorResourcePermissions));

      // authenticate grantor resource
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(grantablePermissionName),
                                                        ResourcePermissions.getInstance(ungrantablePermissionName));
         fail("revoking existing permission granted elsewhere without authorization should have failed");
      }
      catch (NotAuthorizedException e) {
         assertThat(e.getMessage().toLowerCase(), containsString(ungrantablePermissionName.toLowerCase()));
         assertThat(e.getMessage().toLowerCase(), not(containsString(grantablePermissionName)));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions.getInstance(grantablePermissionName),
                                                              ResourcePermissions
                                                                    .getInstance(ungrantablePermissionName)));
         fail("revoking existing permission granted elsewhere without authorization should have failed");
      }
      catch (NotAuthorizedException e) {
         assertThat(e.getMessage().toLowerCase(), containsString(ungrantablePermissionName.toLowerCase()));
         assertThat(e.getMessage().toLowerCase(), not(containsString(grantablePermissionName)));
      }
   }

   @Test
   public void revokeResourcePermissions_identicalPermissions_shouldSucceedAsAuthorized() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String grantedPermissionName1 = generateResourceClassPermission(resourceClassName);
      final String grantedPermissionName2 = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // setup accessor permissions
      Set<ResourcePermission> accessorPermissions_pre = new HashSet<>();
      accessorPermissions_pre.add(ResourcePermissions.getInstance(grantedPermissionName1));
      accessorPermissions_pre.add(ResourcePermissions.getInstance(grantedPermissionName2, true));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(
            accessorPermissions_pre));

      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource), is(accessorPermissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantedPermissionName1, true));
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantedPermissionName2, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource),
                 is(grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      grantQueryPermission(grantorResource, accessorResource2);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(grantedPermissionName1),
                                                     ResourcePermissions.getInstance(grantedPermissionName2, true));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(grantedPermissionName1),
                                                           ResourcePermissions.getInstance(grantedPermissionName2,
                                                                                           true)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_lesserGrantingRightPermissions_shouldSucceedAsAuthorized() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String grantedPermissionName1 = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(
            true));

      // setup accessor permissions
      Set<ResourcePermission> accessorPermissions_pre = new HashSet<>();
      accessorPermissions_pre.add(ResourcePermissions.getInstance(grantedPermissionName1));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(
            accessorPermissions_pre));

      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource), is(accessorPermissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantedPermissionName1, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource),
                 is(grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      grantQueryPermission(grantorResource, accessorResource2);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(grantedPermissionName1, true));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(grantedPermissionName1, true)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_greaterGrantingRightPermissions_shouldSucceedAsAuthorized() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String grantedPermissionName1 = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource grantorResource = generateAuthenticatableResource(password);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(),
                 is(true));

      // setup accessor permissions
      Set<ResourcePermission> accessorPermissions_pre = new HashSet<>();
      accessorPermissions_pre.add(ResourcePermissions.getInstance(grantedPermissionName1, true));

      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource),
                 is(accessorPermissions_pre));

      final Resource accessorResource2 = generateUnauthenticatableResource();
      accessControlContext.setResourcePermissions(accessorResource2, accessedResource, accessorPermissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource),
                 is(accessorPermissions_pre));

      // setup grantor permissions
      Set<ResourcePermission> grantorResourcePermissions = new HashSet<>();
      grantorResourcePermissions.add(ResourcePermissions.getInstance(grantedPermissionName1, true));

      accessControlContext.setResourcePermissions(grantorResource, accessedResource, grantorResourcePermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(grantorResource, accessedResource),
                 is(grantorResourcePermissions));

      // authenticate grantor resource
      grantQueryPermission(grantorResource, accessorResource);
      grantQueryPermission(grantorResource, accessorResource2);
      accessControlContext.authenticate(grantorResource, PasswordCredentials.newInstance(password));

      // revoke permissions as grantor and verify
      accessControlContext.revokeResourcePermissions(accessorResource,
                                                     accessedResource,
                                                     ResourcePermissions.getInstance(grantedPermissionName1));

      final Set<ResourcePermission> permissions_post = accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource);
      assertThat(permissions_post.isEmpty(), is(true));

      // test set-based version
      accessControlContext.revokeResourcePermissions(accessorResource2,
                                                     accessedResource,
                                                     setOf(ResourcePermissions.getInstance(grantedPermissionName1)));

      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource2, accessedResource).isEmpty(),
                 is(true));
   }

   @Test
   public void revokeResourcePermissions_duplicatePermissionNames_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      final String permissionName = generateResourceClassPermission(resourceClassName);

      // attempt to revoke permissions with duplicate permission names
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                        ResourcePermissions.getInstance(permissionName, true),
                                                        ResourcePermissions.getInstance(permissionName, false));
         fail("revoking permissions that include the same permission - by name - but with different grant-options, should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("duplicate permission"));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                              ResourcePermissions.getInstance(permissionName, true),
                                                              ResourcePermissions.getInstance(permissionName, false)));
         fail("revoking permissions that include the same permission - by name - but with different grant-options, should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("duplicate permission"));
      }
   }

   @Test
   public void revokeResourcePermissions_duplicateIdenticalPermissions_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      final String permissionName = generateResourceClassPermission(resourceClassName);

      // setup accessor permissions
      Set<ResourcePermission> permissions_pre = setOf(ResourcePermissions.getInstance(permissionName));
      accessControlContext.setResourcePermissions(accessorResource, accessedResource, permissions_pre);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(permissions_pre));

      // attempt to revoke permissions with duplicate permission names
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(permissionName),
                                                        ResourcePermissions.getInstance(permissionName));
         fail("revoking resource permissions for duplicate (identical) permissions, should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("duplicate element"));
      }
   }

   @Test
   public void revokeResourcePermissions_nulls_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // attempt to revoke permissions with null references
      try {
         accessControlContext.revokeResourcePermissions(null,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
         fail("revoking permissions for null accessor resource should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        null,
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
         fail("revoking permissions for null accessed resource should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        accessedResource,
                                                        (ResourcePermission) null);
         fail("revoking permissions with null permission set should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("permission required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                        null);
         fail("revoking permissions with null permission element should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("array or a sequence"));
      }

      // test set-based version
      try {
         accessControlContext.revokeResourcePermissions(null,
                                                        accessedResource,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.INHERIT)));
         fail("revoking permissions for null accessor resource should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        null,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.INHERIT)));
         fail("revoking permissions for null accessed resource should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        accessedResource,
                                                        (Set<ResourcePermission>) null);
         fail("revoking permissions with null permission set should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("permissions required"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.INHERIT),
                                                              null));
         fail("revoking permissions with null permission element should have failed");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("contains null element"));
      }
   }

   @Test
   public void revokeResourcePermissions_emptyPermissionSet_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      // attempt to revoke permissions with empty permission set
      try {
         accessControlContext.revokeResourcePermissions(accessedResource,
                                                        accessedResource,
                                                        Collections.<ResourcePermission>emptySet());
         fail("revoking permissions with null permission set should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("permissions required"));
      }
   }

   @Test
   public void revokeResourcePermissions_nonExistentReferences_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      Set<ResourcePermission> accessorPermissions
            = setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT, true),
                    ResourcePermissions.getInstance(customPermissionName, true));

      // setup grantor permissions
      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(accessorPermissions));

      // attempt to revoke permissions with non-existent references
      try {
         accessControlContext.revokeResourcePermissions(Resources.getInstance(-999L),
                                                        accessedResource,
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                        ResourcePermissions.getInstance(customPermissionName));
         fail("revoking permissions with non-existent accessor resource reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not found"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        Resources.getInstance(-999L),
                                                        ResourcePermissions.getInstance(ResourcePermissions.INHERIT),
                                                        ResourcePermissions.getInstance(customPermissionName));
         fail("revoking permissions with non-existent accessed resource reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("could not determine resource class for resource"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        ResourcePermissions.getInstance("invalid_permission"));
         fail("revoking permissions with non-existent permission reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }

      // test set-based version
      try {
         accessControlContext.revokeResourcePermissions(Resources.getInstance(-999L),
                                                        accessedResource,
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.INHERIT),
                                                              ResourcePermissions.getInstance(customPermissionName)));
         fail("revoking permissions with non-existent accessor resource reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not found"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        Resources.getInstance(-999L),
                                                        setOf(ResourcePermissions
                                                                    .getInstance(ResourcePermissions.INHERIT),
                                                              ResourcePermissions.getInstance(customPermissionName)));
         fail("revoking permissions with non-existent accessed resource reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("could not determine resource class for resource"));
      }

      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(ResourcePermissions.getInstance("invalid_permission")));
         fail("revoking permissions with non-existent permission reference should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
   }

   @Test
   public void revokeResourcePermissions_mismatchedResourceClass_shouldFail() {
      authenticateSystemResource();
      final String resourceClassName = generateResourceClass(false, false);
      final ResourcePermission permission_valid
            = ResourcePermissions.getInstance(generateResourceClassPermission(resourceClassName), true);
      final ResourcePermission permission_invalid
            = ResourcePermissions.getInstance(generateResourceClassPermission(generateResourceClass(false, false)));
      final Resource accessorResource = generateUnauthenticatableResource();
      final Resource accessedResource = accessControlContext.createResource(resourceClassName, generateDomain());
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource).isEmpty(), is(true));

      Set<ResourcePermission> accessorPermissions
            = setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT, true), permission_valid);

      // setup grantor permissions
      accessControlContext.setResourcePermissions(accessorResource, accessedResource, accessorPermissions);
      assertThat(accessControlContext.getEffectiveResourcePermissions(accessorResource, accessedResource), is(accessorPermissions));

      // attempt to revoke permissions with non-existent references
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        permission_valid,
                                                        permission_invalid);
         fail("revoking permissions with mismatched resource class and permission should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
      try {
         accessControlContext.revokeResourcePermissions(accessorResource,
                                                        accessedResource,
                                                        setOf(permission_valid,
                                                              permission_invalid));
         fail("revoking permissions with mismatched resource class and permission should have failed");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
   }
}
