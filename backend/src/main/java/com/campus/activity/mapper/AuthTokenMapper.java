package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.AuthToken;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for auth_tokens.
 */
public interface AuthTokenMapper extends BaseMapper<AuthToken> {

    /**
     * Select active and unexpired tokens for one user.
     */
    List<AuthToken> selectValidTokensByUserId(@Param("userId") Long userId,
                                              @Param("now") LocalDateTime now);
}
