package fr.uge.project.ruge.dto;

public class UserStatsResponse {
    private int reviewCount;
    private int likeCount;

    public UserStatsResponse(int reviewCount, int likeCount) {
        this.reviewCount = reviewCount;
        this.likeCount = likeCount;
    }

    public int getReviewCount() { return reviewCount; }
    public int getLikeCount() { return likeCount; }
}