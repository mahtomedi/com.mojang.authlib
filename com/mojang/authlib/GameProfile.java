package com.mojang.authlib;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class GameProfile {
   private final String id;
   private final String name;
   private final Map<String, ProfileProperty> properties = new HashMap();
   private boolean legacy;

   public GameProfile(String id, String name) {
      if (StringUtils.isBlank(id) && StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("Name and ID cannot both be blank");
      } else {
         this.id = id;
         this.name = name;
      }
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Map<String, ProfileProperty> getProperties() {
      return this.properties;
   }

   public boolean isComplete() {
      return StringUtils.isNotBlank(this.getId()) && StringUtils.isNotBlank(this.getName());
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         GameProfile that = (GameProfile)o;
         if (!this.id.equals(that.id)) {
            return false;
         } else {
            return this.name.equals(that.name);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.id.hashCode();
      return 31 * result + this.name.hashCode();
   }

   public String toString() {
      return "GameProfile{id='" + this.id + '\'' + ", name='" + this.name + '\'' + '}';
   }

   public boolean isLegacy() {
      return this.legacy;
   }
}
