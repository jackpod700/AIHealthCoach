package com.aihealthcoach.memory.mapper;

import com.aihealthcoach.memory.entity.UserMemory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMemoryMapper {
    UserMemory insertMemory(UserMemory userMemory);

    int deactivateMemory(
            @Param("userId") Long userId,
            @Param("memoryId") Long memoryId
    );

    List<UserMemory> findMemoriesByUserId(@Param("userId") Long userId);

    List<UserMemory> findActiveMemories(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );
}
