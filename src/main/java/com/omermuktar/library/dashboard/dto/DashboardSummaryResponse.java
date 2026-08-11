package com.omermuktar.library.dashboard.dto;

public record DashboardSummaryResponse(
    long totalBookTitles,
    long availableBookTitles,
    long totalCopies,
    long availableCopies,
    long activeMembers,
    long activeLoans,
    long overdueLoans) {}
