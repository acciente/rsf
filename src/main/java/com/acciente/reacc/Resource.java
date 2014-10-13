/*
 * Copyright 2009-2014, Acciente LLC
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
package com.acciente.reacc;

import java.io.Serializable;

public class Resource implements Serializable {
   private final Long resourceId;

   public static Resource getInstance(long resourceId) {
      return new Resource(resourceId);
   }

   private Resource(long resourceId) {
      this.resourceId = resourceId;
   }

   public long getId() {
      return resourceId;
   }

   public String toString() {
      return "R(" + Long.toString(resourceId) + ")";
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      }
      if (other == null || getClass() != other.getClass()) {
         return false;
      }

      Resource otherResource = (Resource) other;

      if (!resourceId.equals(otherResource.resourceId)) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      return resourceId.hashCode();
   }
}