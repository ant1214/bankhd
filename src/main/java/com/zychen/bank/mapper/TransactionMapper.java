package com.zychen.bank.mapper;

import com.zychen.bank.model.Transaction;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransactionMapper {

    @Insert("INSERT INTO transaction (trans_no, card_id, user_id, trans_type, trans_subtype, " +
            "amount, balance_before, balance_after, fee, currency, status, remark, " +
            "operator_id, operator_type, trans_time, completed_time) " +
            "VALUES (#{transNo}, #{cardId}, #{userId}, #{transType}, #{transSubtype}, " +
            "#{amount}, #{balanceBefore}, #{balanceAfter}, #{fee}, #{currency}, #{status}, #{remark}, " +
            "#{operatorId}, #{operatorType}, #{transTime}, #{completedTime})")
    int insert(Transaction transaction);


    // 查询交易记录（分页）
    @Select("<script>" +
            "SELECT * FROM transaction WHERE user_id = #{userId} " +
            "<if test='cardId != null'>AND card_id = #{cardId}</if> " +
            "<if test='transType != null and transType != \"ALL\"'>AND trans_type = #{transType}</if> " +
            "<if test='startDate != null'>AND DATE(trans_time) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND DATE(trans_time) &lt;= #{endDate}</if> " +
            "ORDER BY trans_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Transaction> findByConditions(@Param("userId") String userId,
                                       @Param("cardId") String cardId,
                                       @Param("transType") String transType,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    // 查询交易记录总数
    @Select("<script>" +
            "SELECT COUNT(*) FROM transaction WHERE user_id = #{userId} " +
            "<if test='cardId != null'>AND card_id = #{cardId}</if> " +
            "<if test='transType != null and transType != \"ALL\"'>AND trans_type = #{transType}</if> " +
            "<if test='startDate != null'>AND DATE(trans_time) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND DATE(trans_time) &lt;= #{endDate}</if>" +
            "</script>")
    int countByConditions(@Param("userId") String userId,
                          @Param("cardId") String cardId,
                          @Param("transType") String transType,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * 根据用户ID和年月查询交易记录
     */
    @Select("SELECT * FROM transaction " +
            "WHERE user_id = #{userId} " +
            "AND YEAR(trans_time) = #{year} " +
            "AND MONTH(trans_time) = #{month} " +
            "AND status = 1 " +  // 只统计成功的交易
            "ORDER BY trans_time DESC")
    List<Transaction> findByUserIdAndMonth(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * 查询指定日期前的最后一笔交易
     */
    @Select("<script>" +
            "SELECT * FROM transaction " +
            "WHERE user_id = #{userId} " +
            "<if test='cardId != null'>AND card_id = #{cardId}</if> " +
            "AND DATE(trans_time) &lt;= DATE(#{date}) " +  // 两边都用DATE()函数
            "AND status = 1 " +
            "ORDER BY trans_time DESC " +
            "LIMIT 1" +
            "</script>")
    Transaction findLastTransactionBeforeDate(
            @Param("userId") String userId,
            @Param("cardId") String cardId,
            @Param("date") java.sql.Date date);
}