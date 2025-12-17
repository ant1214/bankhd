package com.zychen.bank.mapper;

import com.zychen.bank.dto.OperationLogQueryDTO;
import com.zychen.bank.model.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    // 插入操作日志
    @Insert("INSERT INTO operation_log (user_id, user_role, module, operation_type, " +
            "operation_detail, target_type, target_id, ip_address, user_agent, " +
            "status, error_message, execution_time, created_time) " +
            "VALUES (#{userId}, #{userRole}, #{module}, #{operationType}, " +
            "#{operationDetail}, #{targetType}, #{targetId}, #{ipAddress}, #{userAgent}, " +
            "#{status}, #{errorMessage}, #{executionTime}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    int insert(OperationLog operationLog);

    // 查询操作日志（带分页和条件）
    @Select("<script>" +
            "SELECT * FROM operation_log " +
            "<where>" +
            "   <if test='query.userId != null'>AND user_id = #{query.userId}</if>" +
            "   <if test='query.targetType != null'>AND target_type = #{query.targetType}</if>" +
            "   <if test='query.targetId != null'>AND target_id = #{query.targetId}</if>" +
            "   <if test='query.operationType != null'>AND operation_type = #{query.operationType}</if>" +
            "   <if test='query.startTime != null'>AND DATE(created_time) &gt;= #{query.startTime}</if>" +
            "   <if test='query.endTime != null'>AND DATE(created_time) &lt;= #{query.endTime}</if>" +
            "</where>" +
            "ORDER BY created_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<OperationLog> findLogs(@Param("query") OperationLogQueryDTO queryDTO,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    // 统计总数（用于分页）
    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log " +
            "<where>" +
            "   <if test='userId != null'>AND user_id = #{userId}</if>" +
            "   <if test='targetType != null'>AND target_type = #{targetType}</if>" +
            "   <if test='targetId != null'>AND target_id = #{targetId}</if>" +
            "   <if test='operationType != null'>AND operation_type = #{operationType}</if>" +
            "   <if test='startTime != null'>AND DATE(created_time) &gt;= #{startTime}</if>" +
            "   <if test='endTime != null'>AND DATE(created_time) &lt;= #{endTime}</if>" +
            "</where>" +
            "</script>")
    int countLogs(OperationLogQueryDTO queryDTO);

    // 根据ID查询
    @Select("SELECT * FROM operation_log WHERE log_id = #{logId}")
    OperationLog findById(@Param("logId") Long logId);
}