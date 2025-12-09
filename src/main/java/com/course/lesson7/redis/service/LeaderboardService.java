package com.course.lesson7.redis.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LEADERBOARD_PREFIX = "leaderboard:";

    public void setScore(String leaderboardName, String playerId, double score) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        redisTemplate.opsForZSet().add(key, playerId, score);
        log.debug("Score set for player {} in {}: {}", playerId, leaderboardName, score);
    }

    public Double incrementScore(String leaderboardName, String playerId, double delta) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        Double newScore = redisTemplate.opsForZSet().incrementScore(key, playerId, delta);
        log.debug("Score incremented for player {} in {}: +{} = {}",
                playerId, leaderboardName, delta, newScore);
        return newScore;
    }

    public Double getScore(String leaderboardName, String playerId) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        return redisTemplate.opsForZSet().score(key, playerId);
    }

    public Long getRank(String leaderboardName, String playerId) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        return redisTemplate.opsForZSet().reverseRank(key, playerId);
    }

    public List<LeaderboardEntry> getTopPlayers(String leaderboardName, int count) {
        String key = LEADERBOARD_PREFIX + leaderboardName;

        Set<ZSetOperations.TypedTuple<Object>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, count - 1);

        return convertToEntries(entries);
    }

    public List<LeaderboardEntry> getPlayersByRank(String leaderboardName, long startRank, long endRank) {
        String key = LEADERBOARD_PREFIX + leaderboardName;

        Set<ZSetOperations.TypedTuple<Object>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, startRank, endRank);

        return convertToEntries(entries);
    }

    public List<LeaderboardEntry> getPlayersByScore(String leaderboardName,
                                                     double minScore, double maxScore) {
        String key = LEADERBOARD_PREFIX + leaderboardName;

        Set<ZSetOperations.TypedTuple<Object>> entries =
                redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, minScore, maxScore);

        return convertToEntries(entries);
    }

    public Long getTotalPlayers(String leaderboardName) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        return redisTemplate.opsForZSet().zCard(key);
    }

    public Long getPlayersCountInRange(String leaderboardName, double minScore, double maxScore) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        return redisTemplate.opsForZSet().count(key, minScore, maxScore);
    }

    public void removePlayer(String leaderboardName, String playerId) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        redisTemplate.opsForZSet().remove(key, playerId);
        log.info("Player {} removed from leaderboard {}", playerId, leaderboardName);
    }

    public Long removePlayersBelowScore(String leaderboardName, double threshold) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, threshold);
        log.info("Removed {} players below score {} from leaderboard {}",
                removed, threshold, leaderboardName);
        return removed;
    }

    public void clearLeaderboard(String leaderboardName) {
        String key = LEADERBOARD_PREFIX + leaderboardName;
        redisTemplate.delete(key);
        log.info("Leaderboard cleared: {}", leaderboardName);
    }

    public List<LeaderboardEntry> getPlayersAroundPlayer(String leaderboardName,
                                                          String playerId, int range) {
        Long rank = getRank(leaderboardName, playerId);

        if (rank == null) {
            return new ArrayList<>();
        }

        long startRank = Math.max(0, rank - range);
        long endRank = rank + range;

        return getPlayersByRank(leaderboardName, startRank, endRank);
    }

    private List<LeaderboardEntry> convertToEntries(Set<ZSetOperations.TypedTuple<Object>> entries) {
        List<LeaderboardEntry> result = new ArrayList<>();

        if (entries != null) {
            int rank = 1;
            for (ZSetOperations.TypedTuple<Object> entry : entries) {
                result.add(new LeaderboardEntry(
                        rank++,
                        (String) entry.getValue(),
                        entry.getScore() != null ? entry.getScore() : 0.0
                ));
            }
        }

        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry implements Serializable {
        private int rank;
        private String playerId;
        private double score;
    }
}
