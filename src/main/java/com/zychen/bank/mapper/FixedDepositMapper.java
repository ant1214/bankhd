package com.zychen.bank.mapper;

import com.zychen.bank.model.FixedDeposit;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FixedDepositMapper {

    // 插入定期存款 - 修正字段映射
    @Insert("INSERT INTO fixed_deposit(fd_no, card_id, user_id, principal, " +
            "annual_rate, term_months, start_date, end_date, " +
            "auto_renew, status, created_time) " +
            "VALUES(#{fdNo}, #{cardId}, #{userId}, #{principal}, #{rate}, " +
            "#{term}, #{startTime}, #{endTime}, #{autoRenew}, " +
            "#{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "fdId")
    int insert(FixedDeposit fixedDeposit);

    // 根据用户ID查询 - 使用字段别名映射
    @Select("SELECT fd_id as fdId, fd_no as fdNo, card_id as cardId, user_id as userId, " +
            "principal, annual_rate as rate, term_months as term, " +
            "start_date as startTime, end_date as endTime, " +
            "auto_renew, status, created_time as createdTime " +
            "FROM fixed_deposit WHERE user_id = #{userId} " +
            "ORDER BY created_time DESC")
    List<FixedDeposit> findByUserId(@Param("userId") String userId);

    // 根据卡号查询 - 使用字段别名映射
    @Select("SELECT fd_id as fdId, fd_no as fdNo, card_id as cardId, user_id as userId, " +
            "principal, annual_rate as rate, term_months as term, " +
            "start_date as startTime, end_date as endTime, " +
            "auto_renew, status, created_time as createdTime " +
            "FROM fixed_deposit WHERE card_id = #{cardId} " +
            "ORDER BY created_time DESC")
    List<FixedDeposit> findByCardId(@Param("cardId") String cardId);

    // 根据ID查询 - 使用字段别名映射
    @Select("SELECT fd_id as fdId, fd_no as fdNo, card_id as cardId, user_id as userId, " +
            "principal, annual_rate as rate, term_months as term, " +
            "start_date as startTime, end_date as endTime, " +
            "auto_renew, status, created_time as createdTime " +
            "FROM fixed_deposit WHERE fd_id = #{fdId}")
    FixedDeposit findById(@Param("fdId") Integer fdId);

    // 更新状态
    @Update("UPDATE fixed_deposit SET status = #{status} WHERE fd_id = #{fdId}")
    int updateStatus(@Param("fdId") Integer fdId, @Param("status") Integer status);


    @Select("SELECT COUNT(*) FROM fixed_deposit WHERE status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    @Select("SELECT IFNULL(SUM(principal), 0) FROM fixed_deposit WHERE status IN (0, 1)")
    BigDecimal getTotalFixedDepositAmount();

}