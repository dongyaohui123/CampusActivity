package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.CreateActivityCommentRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityComment;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityCommentMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1ActivityCommentServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.ActivityCommentView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1ActivityCommentServiceImplTest {
    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityCommentMapper commentMapper;

    @Mock
    private OperatorPermissionService permissionService;

    private V1ActivityCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new V1ActivityCommentServiceImpl(
                activityMapper,
                reviewMapper,
                commentMapper,
                permissionService
        );
    }

    @Test
    void listActivityComments_shouldReturnMapperRowsForPublicApprovedActivity() {
        ActivityCommentView view = new ActivityCommentView();
        view.setCommentId(8L);
        view.setActivityId(9L);
        view.setContent("nice");
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));
        when(commentMapper.selectActivityComments(9L)).thenReturn(List.of(view));

        List<ActivityCommentView> rows = commentService.listActivityComments(9L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCommentId()).isEqualTo(8L);
        assertThat(rows.get(0).getContent()).isEqualTo("nice");
    }

    @Test
    void listActivityComments_shouldRejectUnavailableActivity() {
        Activity activity = buildPublicActivity(9L);
        activity.setStatus(ActivityStatus.DRAFT);
        when(activityMapper.selectById(9L)).thenReturn(activity);

        assertThatThrownBy(() -> commentService.listActivityComments(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void createComment_shouldInsertTrimmedComment() {
        User operator = buildUser(2L, UserRole.STUDENT);
        operator.setNickname("Tester");
        CreateActivityCommentRequest request = new CreateActivityCommentRequest();
        request.setContent("  hello campus  ");

        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));

        ActivityCommentView result = commentService.createComment(9L, request, 2L, UserRole.STUDENT);

        verify(commentMapper).insert((ActivityComment) any(ActivityComment.class));
        assertThat(result.getActivityId()).isEqualTo(9L);
        assertThat(result.getAuthorUserId()).isEqualTo(2L);
        assertThat(result.getAuthorNickname()).isEqualTo("Tester");
        assertThat(result.getContent()).isEqualTo("hello campus");
    }

    @Test
    void createComment_shouldRejectBlankContent() {
        User operator = buildUser(2L, UserRole.STUDENT);
        CreateActivityCommentRequest request = new CreateActivityCommentRequest();
        request.setContent("   ");

        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));

        assertThatThrownBy(() -> commentService.createComment(9L, request, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void createComment_shouldRejectTooLongContent() {
        User operator = buildUser(2L, UserRole.STUDENT);
        CreateActivityCommentRequest request = new CreateActivityCommentRequest();
        request.setContent("a".repeat(501));

        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));

        assertThatThrownBy(() -> commentService.createComment(9L, request, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void deleteComment_shouldRejectNonAuthor() {
        User operator = buildUser(2L, UserRole.STUDENT);
        ActivityComment comment = new ActivityComment();
        comment.setId(33L);
        comment.setActivityId(9L);
        comment.setUserId(5L);

        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));
        when(commentMapper.selectById(33L)).thenReturn(comment);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self can access this resource"))
                .when(permissionService).requireSelf(operator, 5L);

        assertThatThrownBy(() -> commentService.deleteComment(9L, 33L, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void deleteComment_shouldDeleteWhenAuthorMatches() {
        User operator = buildUser(2L, UserRole.STUDENT);
        ActivityComment comment = new ActivityComment();
        comment.setId(33L);
        comment.setActivityId(9L);
        comment.setUserId(2L);

        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));
        when(commentMapper.selectById(33L)).thenReturn(comment);

        commentService.deleteComment(9L, 33L, 2L, UserRole.STUDENT);

        verify(commentMapper).delete(any());
    }

    private User buildUser(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setUsername("user" + id);
        return user;
    }

    private Activity buildPublicActivity(Long id) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setStatus(ActivityStatus.PUBLISHED);
        return activity;
    }

    private ActivityReview buildApprovedReview(Long activityId) {
        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setReviewStatus(ReviewStatus.APPROVED);
        return review;
    }
}
