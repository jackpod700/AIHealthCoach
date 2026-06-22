package com.aihealthcoach.admin.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper {
    long countUsers();

    long countTodaySignups();
}
