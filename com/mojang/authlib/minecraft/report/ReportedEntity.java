package com.mojang.authlib.minecraft.report;

import java.util.UUID;

public class ReportedEntity {
   public UUID profileId;

   public ReportedEntity(UUID profileId) {
      this.profileId = profileId;
   }
}
