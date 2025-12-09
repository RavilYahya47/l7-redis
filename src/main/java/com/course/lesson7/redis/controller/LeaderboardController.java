package com.course.lesson7.redis.controller;

import com.course.lesson7.redis.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping("/{leaderboardName}/score")
    public ResponseEntity<Void> setScore(
            @PathVariable String leaderboardName,
            @RequestParam String playerId,
            @RequestParam double score) {
        leaderboardService.setScore(leaderboardName, playerId, score);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{leaderboardName}/increment")
    public ResponseEntity<Double> incrementScore(
            @PathVariable String leaderboardName,
            @RequestParam String playerId,
            @RequestParam double delta) {
        Double newScore = leaderboardService.incrementScore(leaderboardName, playerId, delta);
        return ResponseEntity.ok(newScore);
    }

    @GetMapping("/{leaderboardName}/score/{playerId}")
    public ResponseEntity<Double> getScore(
            @PathVariable String leaderboardName,
            @PathVariable String playerId) {
        Double score = leaderboardService.getScore(leaderboardName, playerId);
        return ResponseEntity.ok(score);
    }

    @GetMapping("/{leaderboardName}/rank/{playerId}")
    public ResponseEntity<Long> getRank(
            @PathVariable String leaderboardName,
            @PathVariable String playerId) {
        Long rank = leaderboardService.getRank(leaderboardName, playerId);
        return ResponseEntity.ok(rank);
    }

    @GetMapping("/{leaderboardName}/top")
    public ResponseEntity<List<LeaderboardService.LeaderboardEntry>> getTopPlayers(
            @PathVariable String leaderboardName,
            @RequestParam(defaultValue = "10") int count) {
        List<LeaderboardService.LeaderboardEntry> entries =
                leaderboardService.getTopPlayers(leaderboardName, count);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{leaderboardName}/range")
    public ResponseEntity<List<LeaderboardService.LeaderboardEntry>> getPlayersByRank(
            @PathVariable String leaderboardName,
            @RequestParam long startRank,
            @RequestParam long endRank) {
        List<LeaderboardService.LeaderboardEntry> entries =
                leaderboardService.getPlayersByRank(leaderboardName, startRank, endRank);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{leaderboardName}/around/{playerId}")
    public ResponseEntity<List<LeaderboardService.LeaderboardEntry>> getPlayersAroundPlayer(
            @PathVariable String leaderboardName,
            @PathVariable String playerId,
            @RequestParam(defaultValue = "5") int range) {
        List<LeaderboardService.LeaderboardEntry> entries =
                leaderboardService.getPlayersAroundPlayer(leaderboardName, playerId, range);
        return ResponseEntity.ok(entries);
    }

    @DeleteMapping("/{leaderboardName}/player/{playerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable String leaderboardName,
            @PathVariable String playerId) {
        leaderboardService.removePlayer(leaderboardName, playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{leaderboardName}")
    public ResponseEntity<Void> clearLeaderboard(@PathVariable String leaderboardName) {
        leaderboardService.clearLeaderboard(leaderboardName);
        return ResponseEntity.ok().build();
    }
}
