package com.campus.activity.service.impl.v1;

import com.campus.activity.entity.StudentProfile;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.mapper.RecommendationMapper;
import com.campus.activity.mapper.StudentProfileMapper;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1RecommendationService;
import com.campus.activity.view.v1.CategoryAffinityRow;
import com.campus.activity.view.v1.OrganizerAffinityRow;
import com.campus.activity.view.v1.RecommendActivityView;
import com.campus.activity.view.v1.RecommendCandidateView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 推荐服务实现（v1）。
 * 基于加权多信号评分的个性化推荐算法。
 */
@Service
public class V1RecommendationServiceImpl implements V1RecommendationService {

    private static final double WEIGHT_CATEGORY = 0.40;
    private static final double WEIGHT_ORGANIZER = 0.25;
    private static final double WEIGHT_POPULARITY = 0.15;
    private static final double WEIGHT_RECENCY = 0.10;
    private static final double WEIGHT_FEATURED = 0.10;

    private final RecommendationMapper recommendationMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final OperatorPermissionService permissionService;
    private final ActivityPhaseResolver activityPhaseResolver;

    public V1RecommendationServiceImpl(
            RecommendationMapper recommendationMapper,
            StudentProfileMapper studentProfileMapper,
            OperatorPermissionService permissionService,
            ActivityPhaseResolver activityPhaseResolver
    ) {
        this.recommendationMapper = recommendationMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.permissionService = permissionService;
        this.activityPhaseResolver = activityPhaseResolver;
    }

    @Override
    public List<RecommendActivityView> getRecommendedActivities(Long operatorUserId, UserRole operatorRole, int limit) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);

        Map<Long, Double> categoryAffinity = new HashMap<>();
        Map<Long, Double> organizerAffinity = new HashMap<>();
        boolean coldStart = false;

        List<CategoryAffinityRow> categoryRows = recommendationMapper.selectUserCategoryAffinity(operator.getId());
        List<OrganizerAffinityRow> organizerRows = recommendationMapper.selectUserOrganizerAffinity(operator.getId());

        if (categoryRows.isEmpty() && organizerRows.isEmpty()) {
            coldStart = true;
            StudentProfile profile = studentProfileMapper.selectById(operator.getId());
            if (profile != null && profile.getCollege() != null) {
                List<CategoryAffinityRow> peerRows = recommendationMapper.selectPeerCategoryAffinity(
                        profile.getCollege(), profile.getMajor(), operator.getId()
                );
                for (CategoryAffinityRow row : peerRows) {
                    categoryAffinity.put(row.getCategoryId(), row.getScore());
                }
            }
        } else {
            for (CategoryAffinityRow row : categoryRows) {
                categoryAffinity.put(row.getCategoryId(), row.getScore());
            }
            for (OrganizerAffinityRow row : organizerRows) {
                organizerAffinity.put(row.getOrganizerId(), row.getScore());
            }
        }

        List<RecommendCandidateView> candidates = recommendationMapper.selectRecommendationCandidates();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        double maxCategoryAffinity = categoryAffinity.values().stream().max(Double::compareTo).orElse(1.0);
        double maxOrganizerAffinity = organizerAffinity.values().stream().max(Double::compareTo).orElse(1.0);
        int maxRegisteredCount = candidates.stream()
                .mapToInt(c -> c.getRegisteredCount() != null ? c.getRegisteredCount() : 0)
                .max().orElse(1);

        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (RecommendCandidateView candidate : candidates) {
            double categoryScore = computeCategoryScore(candidate, categoryAffinity, maxCategoryAffinity);
            double organizerScore = computeOrganizerScore(candidate, organizerAffinity, maxOrganizerAffinity);
            double popularityScore = computePopularityScore(candidate, maxRegisteredCount);
            double recencyScore = computeRecencyScore(candidate);
            double featuredScore = Boolean.TRUE.equals(candidate.getFeatured()) ? 1.0 : 0.0;

            double totalScore = WEIGHT_CATEGORY * categoryScore
                    + WEIGHT_ORGANIZER * organizerScore
                    + WEIGHT_POPULARITY * popularityScore
                    + WEIGHT_RECENCY * recencyScore
                    + WEIGHT_FEATURED * featuredScore;

            String reason = generateReason(categoryScore, organizerScore, popularityScore, featuredScore,
                    coldStart, candidate, categoryAffinity);

            scoredCandidates.add(new ScoredCandidate(candidate, totalScore, reason));
        }

        scoredCandidates.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());

        return scoredCandidates.stream()
                .limit(limit)
                .map(sc -> toRecommendView(sc.candidate(), sc.score(), sc.reason()))
                .collect(Collectors.toList());
    }

    private double computeCategoryScore(RecommendCandidateView candidate, Map<Long, Double> affinity, double maxAffinity) {
        if (affinity.isEmpty() || maxAffinity <= 0) {
            return 0.0;
        }
        String categoryIdList = candidate.getCategoryIdList();
        if (categoryIdList == null || categoryIdList.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (String idStr : categoryIdList.split(",")) {
            try {
                Long categoryId = Long.parseLong(idStr.trim());
                total += affinity.getOrDefault(categoryId, 0.0);
            } catch (NumberFormatException ignored) {
            }
        }
        return Math.min(total / maxAffinity, 1.0);
    }

    private double computeOrganizerScore(RecommendCandidateView candidate, Map<Long, Double> affinity, double maxAffinity) {
        if (affinity.isEmpty() || maxAffinity <= 0 || candidate.getOrganizerId() == null) {
            return 0.0;
        }
        double score = affinity.getOrDefault(candidate.getOrganizerId(), 0.0);
        return Math.min(score / maxAffinity, 1.0);
    }

    private double computePopularityScore(RecommendCandidateView candidate, int maxCount) {
        if (maxCount <= 0) {
            return 0.0;
        }
        int count = candidate.getRegisteredCount() != null ? candidate.getRegisteredCount() : 0;
        return (double) count / maxCount;
    }

    private double computeRecencyScore(RecommendCandidateView candidate) {
        if (candidate.getStartTime() == null) {
            return 0.0;
        }
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDateTime.now(), candidate.getStartTime());
        return Math.max(0.0, 1.0 - Math.abs(daysUntilStart) / 30.0);
    }

    private String generateReason(double categoryScore, double organizerScore, double popularityScore,
                                   double featuredScore, boolean coldStart, RecommendCandidateView candidate,
                                   Map<Long, Double> categoryAffinity) {
        if (coldStart && !categoryAffinity.isEmpty()) {
            return "与你同专业的同学也在关注";
        }

        double maxScore = Math.max(categoryScore, Math.max(organizerScore, Math.max(popularityScore, featuredScore)));

        if (maxScore == organizerScore && organizerScore > 0.3) {
            return "你关注的" + candidate.getOrganizerName() + "发布了新活动";
        }
        if (maxScore == categoryScore && categoryScore > 0.3) {
            String primaryCategory = getPrimaryCategory(candidate.getCategoryNames());
            if (primaryCategory != null) {
                return "你常参加" + primaryCategory + "类活动";
            }
        }
        if (Boolean.TRUE.equals(candidate.getFeatured())) {
            return "精选推荐";
        }
        if (maxScore == popularityScore && popularityScore > 0.5) {
            return "校园热门活动";
        }

        return "为你推荐";
    }

    private String getPrimaryCategory(String categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return null;
        }
        String[] parts = categoryNames.split(",");
        return parts.length > 0 ? parts[0].trim() : null;
    }

    private RecommendActivityView toRecommendView(RecommendCandidateView candidate, double score, String reason) {
        RecommendActivityView view = new RecommendActivityView();
        view.setId(candidate.getId());
        view.setTitle(candidate.getTitle());
        view.setSummary(candidate.getSummary());
        view.setCoverUrl(candidate.getCoverUrl());
        view.setLocation(candidate.getLocation());
        view.setStartTime(candidate.getStartTime());
        view.setEndTime(candidate.getEndTime());
        view.setRegistrationDeadline(candidate.getRegistrationDeadline());
        view.setRegisteredCount(candidate.getRegisteredCount());
        view.setOrganizerName(candidate.getOrganizerName());
        view.setCategoryNames(candidate.getCategoryNames());
        view.setLocationCampus(candidate.getLocationCampus());
        view.setRecommendScore(Math.round(score * 100.0) / 100.0);
        view.setRecommendReason(reason);
        view.setStatus(activityPhaseResolver.resolve(
                candidate.getStatus(),
                candidate.getStartTime(),
                candidate.getEndTime(),
                candidate.getRegistrationDeadline()
        ));
        return view;
    }

    private record ScoredCandidate(RecommendCandidateView candidate, double score, String reason) {
    }
}
