package com.campus.activity.service.v1;

import com.campus.activity.entity.Activity;
import com.campus.activity.enums.ActivityStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 基于活动时间窗口推导面向学生侧的有效活动阶段。
 * 不回写数据库，仅用于接口返回与资格校验。
 */
@Component
public class ActivityPhaseResolver {

    /**
     * 根据活动实体推导有效状态。
     */
    public ActivityStatus resolve(Activity activity) {
        if (activity == null) {
            return null;
        }
        return resolve(
                activity.getStatus(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getRegistrationDeadline()
        );
    }

    /**
     * 使用当前时间推导活动有效状态。
     */
    public ActivityStatus resolve(
            ActivityStatus rawStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime registrationDeadline
    ) {
        return resolve(rawStatus, startTime, endTime, registrationDeadline, LocalDateTime.now());
    }

    /**
     * 指定时间点下推导活动有效状态，便于测试。
     */
    public ActivityStatus resolve(
            ActivityStatus rawStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime registrationDeadline,
            LocalDateTime now
    ) {
        if (rawStatus == null) {
            return null;
        }
        if (ActivityStatus.DRAFT.equals(rawStatus) || ActivityStatus.CANCELLED.equals(rawStatus)) {
            return rawStatus;
        }

        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();
        if (endTime != null && !endTime.isAfter(effectiveNow)) {
            return ActivityStatus.FINISHED;
        }
        if (startTime != null && !startTime.isAfter(effectiveNow)) {
            return ActivityStatus.ONGOING;
        }
        if (registrationDeadline != null && registrationDeadline.isBefore(effectiveNow)) {
            return ActivityStatus.REGISTRATION_CLOSED;
        }
        return ActivityStatus.REGISTRATION_OPEN;
    }
}
