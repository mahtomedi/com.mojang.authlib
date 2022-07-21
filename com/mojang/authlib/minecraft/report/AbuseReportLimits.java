package com.mojang.authlib.minecraft.report;

public record AbuseReportLimits(int maxOpinionCommentsLength, int maxReportedMessageCount, int maxEvidenceMessageCount) {
   public static final AbuseReportLimits DEFAULTS = new AbuseReportLimits(1000, 10, 100);
}