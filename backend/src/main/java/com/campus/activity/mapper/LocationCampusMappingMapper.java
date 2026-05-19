package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.LocationCampusMapping;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for location_campus_mapping.
 */
public interface LocationCampusMappingMapper extends BaseMapper<LocationCampusMapping> {

    /**
     * 查询启用映射中的校区代码（去重）。
     */
    List<String> selectEnabledCampusCodes();

    /**
     * 按地点名称查询启用映射。
     */
    LocationCampusMapping selectEnabledByLocationName(@Param("locationName") String locationName);

    /**
     * 新增或覆盖地点映射并启用。
     */
    int upsertMapping(@Param("locationName") String locationName, @Param("campusCode") String campusCode);
}
