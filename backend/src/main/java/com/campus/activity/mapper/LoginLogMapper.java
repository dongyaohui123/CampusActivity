package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.LoginLog;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for login_logs.
 */
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    /**
     * Read recent logs by user id, sorted by latest first.
     */
    List<LoginLog> selectRecentByUserId(@Param("userId") Long userId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("limit") Integer limit);
}
