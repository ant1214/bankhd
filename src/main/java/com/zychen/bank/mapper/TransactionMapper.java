package com.zychen.bank.mapper;

import com.zychen.bank.model.Transaction;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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


    @Select("SELECT COUNT(*) FROM transaction WHERE status = 1")
    Long countTotalTransactions();

    @Select("SELECT COUNT(*) FROM transaction WHERE DATE(trans_time) = CURDATE() AND status = 1")
    Long countTodayTransactions();

    @Select("SELECT COUNT(*) FROM transaction WHERE status = 2")
    Long countPendingTransactions();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM transaction WHERE DATE(trans_time) = CURDATE() " +
            "AND trans_type = 'DEPOSIT' AND status = 1")
    BigDecimal getTodayIncome();

    @Select("SELECT IFNULL(SUM(CASE " +
            "WHEN trans_type = 'WITHDRAW' AND status = 1 THEN amount " +
            "WHEN trans_type = 'TRANSFER' AND amount < 0 AND status = 1 THEN ABS(amount) " +
            "ELSE 0 " +
            "END), 0) FROM transaction WHERE DATE(trans_time) = CURDATE()")
    BigDecimal getTodayOutcome();


    // TransactionMapper.java - 修复版本
    /**
     * 管理员查询交易记录（模糊搜索版）
     */
    @Select("<script>" +
            "SELECT t.*, u.username, ui.name as real_name " +
            "FROM transaction t " +
            "LEFT JOIN user u ON t.user_id = u.user_id " +
            "LEFT JOIN user_info ui ON t.user_id = ui.user_id " +
            "WHERE 1=1 " +
            "<if test='userId != null and userId.length() > 0'>" +
            "  AND t.user_id LIKE CONCAT('%', #{userId}, '%') " +  // 改为模糊搜索
            "</if> " +
            "<if test='userName != null and userName.length() > 0'>" +
            "  AND (u.username LIKE CONCAT('%', #{userName}, '%') OR ui.name LIKE CONCAT('%', #{userName}, '%')) " +
            "</if> " +
            "<if test='cardId != null and cardId.length() > 0'>" +
            "  AND t.card_id LIKE CONCAT('%', #{cardId}, '%') " +  // 改为模糊搜索
            "</if> " +
            "<if test='transType != null and transType.length() > 0'>" +
            "  AND t.trans_type = #{transType} " +  // 交易类型保持精确匹配
            "</if> " +
            "<if test='transNo != null and transNo.length() > 0'>" +
            "  AND t.trans_no LIKE CONCAT('%', #{transNo}, '%') " +  // 流水号也改为模糊搜索
            "</if> " +
            "<if test='status != null'>" +
            "  AND t.status = #{status} " +  // 状态保持精确匹配
            "</if> " +
            "<if test='startDate != null'>" +
            "  AND DATE(t.trans_time) &gt;= #{startDate} " +
            "</if> " +
            "<if test='endDate != null'>" +
            "  AND DATE(t.trans_time) &lt;= #{endDate} " +
            "</if> " +
            "ORDER BY t.trans_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    @Results({
            // 这里保持你的@Results注解不变
    })
    List<Map<String, Object>> findAdminTransactions(
            @Param("userId") String userId,
            @Param("userName") String userName,
            @Param("cardId") String cardId,
            @Param("transType") String transType,
            @Param("transNo") String transNo,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    /**
     * 统计管理员查询的总数（模糊搜索版）
     */
    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM transaction t " +
            "LEFT JOIN user u ON t.user_id = u.user_id " +
            "LEFT JOIN user_info ui ON t.user_id = ui.user_id " +
            "WHERE 1=1 " +
            "<if test='userId != null and userId.length() > 0'>" +
            "  AND t.user_id LIKE CONCAT('%', #{userId}, '%') " +  // 改为模糊搜索
            "</if> " +
            "<if test='userName != null and userName.length() > 0'>" +
            "  AND (u.username LIKE CONCAT('%', #{userName}, '%') OR ui.name LIKE CONCAT('%', #{userName}, '%')) " +
            "</if> " +
            "<if test='cardId != null and cardId.length() > 0'>" +
            "  AND t.card_id LIKE CONCAT('%', #{cardId}, '%') " +  // 改为模糊搜索
            "</if> " +
            "<if test='transType != null and transType.length() > 0'>" +
            "  AND t.trans_type = #{transType} " +  // 交易类型保持精确匹配
            "</if> " +
            "<if test='transNo != null and transNo.length() > 0'>" +
            "  AND t.trans_no LIKE CONCAT('%', #{transNo}, '%') " +  // 流水号也改为模糊搜索
            "</if> " +
            "<if test='status != null'>" +
            "  AND t.status = #{status} " +  // 状态保持精确匹配
            "</if> " +
            "<if test='startDate != null'>" +
            "  AND DATE(t.trans_time) &gt;= #{startDate} " +
            "</if> " +
            "<if test='endDate != null'>" +
            "  AND DATE(t.trans_time) &lt;= #{endDate} " +
            "</if>" +
            "</script>")
    int countAdminTransactions(
            @Param("userId") String userId,
            @Param("userName") String userName,
            @Param("cardId") String cardId,
            @Param("transType") String transType,
            @Param("transNo") String transNo,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
//    /**
//     * 管理员查询交易记录（修复NPE问题）
//     */
//    @Select("<script>" +
//            "SELECT t.*, u.username, ui.name as real_name " +
//            "FROM transaction t " +
//            "LEFT JOIN user u ON t.user_id = u.user_id " +
//            "LEFT JOIN user_info ui ON t.user_id = ui.user_id " +
//            "WHERE 1=1 " +
//            "<if test='userId != null and userId.length() > 0'>" +
//            "  AND t.user_id = #{userId} " +
//            "</if> " +
//            "<if test='userName != null and userName.length() > 0'>" +
//            "  AND (u.username LIKE CONCAT('%', #{userName}, '%') OR ui.name LIKE CONCAT('%', #{userName}, '%')) " +
//            "</if> " +
//            "<if test='cardId != null and cardId.length() > 0'>" +
//            "  AND t.card_id = #{cardId} " +
//            "</if> " +
//            "<if test='transType != null and transType.length() > 0'>" +
//            "  AND t.trans_type = #{transType} " +
//            "</if> " +
//            "<if test='transNo != null and transNo.length() > 0'>" +
//            "  AND t.trans_no = #{transNo} " +
//            "</if> " +
//            "<if test='status != null'>" +
//            "  AND t.status = #{status} " +
//            "</if> " +
//            "<if test='startDate != null'>" +
//            "  AND DATE(t.trans_time) &gt;= #{startDate} " +
//            "</if> " +
//            "<if test='endDate != null'>" +
//            "  AND DATE(t.trans_time) &lt;= #{endDate} " +
//            "</if> " +
//            "ORDER BY t.trans_time DESC " +
//            "LIMIT #{offset}, #{pageSize}" +
//            "</script>")
//    @Results({
//            // 这里保持你的@Results注解不变
//    })
//    List<Map<String, Object>> findAdminTransactions(
//            @Param("userId") String userId,
//            @Param("userName") String userName,
//            @Param("cardId") String cardId,
//            @Param("transType") String transType,
//            @Param("transNo") String transNo,
//            @Param("status") Integer status,
//            @Param("startDate") LocalDate startDate,
//            @Param("endDate") LocalDate endDate,
//            @Param("offset") int offset,
//            @Param("pageSize") int pageSize);
//
//    /**
//     * 统计管理员查询的总数（修复版）
//     */
//    @Select("<script>" +
//            "SELECT COUNT(*) " +
//            "FROM transaction t " +
//            "LEFT JOIN user u ON t.user_id = u.user_id " +
//            "LEFT JOIN user_info ui ON t.user_id = ui.user_id " +
//            "WHERE 1=1 " +
//            "<if test='userId != null and userId.length() > 0'>" +
//            "  AND t.user_id = #{userId} " +
//            "</if> " +
//            "<if test='userName != null and userName.length() > 0'>" +
//            "  AND (u.username LIKE CONCAT('%', #{userName}, '%') OR ui.name LIKE CONCAT('%', #{userName}, '%')) " +
//            "</if> " +
//            "<if test='cardId != null and cardId.length() > 0'>" +
//            "  AND t.card_id = #{cardId} " +
//            "</if> " +
//            "<if test='transType != null and transType.length() > 0'>" +
//            "  AND t.trans_type = #{transType} " +
//            "</if> " +
//            "<if test='transNo != null and transNo.length() > 0'>" +
//            "  AND t.trans_no = #{transNo} " +
//            "</if> " +
//            "<if test='status != null'>" +
//            "  AND t.status = #{status} " +
//            "</if> " +
//            "<if test='startDate != null'>" +
//            "  AND DATE(t.trans_time) &gt;= #{startDate} " +
//            "</if> " +
//            "<if test='endDate != null'>" +
//            "  AND DATE(t.trans_time) &lt;= #{endDate} " +
//            "</if>" +
//            "</script>")
//    int countAdminTransactions(
//            @Param("userId") String userId,
//            @Param("userName") String userName,
//            @Param("cardId") String cardId,
//            @Param("transType") String transType,
//            @Param("transNo") String transNo,
//            @Param("status") Integer status,
//            @Param("startDate") LocalDate startDate,
//            @Param("endDate") LocalDate endDate);
    /**
     * 获取交易类型统计（用于图表）
     */
    @Select("<script>" +
            "SELECT trans_type, COUNT(*) as count, SUM(amount) as total_amount " +
            "FROM transaction " +
            "WHERE 1=1 " +
            "<if test='startDate != null'>AND DATE(trans_time) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND DATE(trans_time) &lt;= #{endDate}</if> " +
            "AND status = 1 " +  // 只统计成功的交易
            "GROUP BY trans_type" +
            "</script>")
    List<Map<String, Object>> getTransactionTypeStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 获取每日交易统计（用于折线图）
     */
    @Select("<script>" +
            "SELECT DATE(trans_time) as date, " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN trans_type = 'DEPOSIT' THEN 1 ELSE 0 END) as deposit_count, " +
            "SUM(CASE WHEN trans_type = 'WITHDRAW' THEN 1 ELSE 0 END) as withdraw_count, " +
            "SUM(CASE WHEN trans_type = 'DEPOSIT' AND status = 1 THEN amount ELSE 0 END) as deposit_amount, " +
            "SUM(CASE WHEN trans_type = 'WITHDRAW' AND status = 1 THEN amount ELSE 0 END) as withdraw_amount " +
            "FROM transaction " +
            "WHERE 1=1 " +
            "<if test='startDate != null'>AND DATE(trans_time) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND DATE(trans_time) &lt;= #{endDate}</if> " +
            "GROUP BY DATE(trans_time) " +
            "ORDER BY date DESC" +
            "</script>")
    List<Map<String, Object>> getDailyTransactionStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 统计指定日期范围内的转账转出金额
     */
    @Select("SELECT COALESCE(SUM(ABS(amount)), 0) FROM transaction " +
            "WHERE trans_type = 'TRANSFER' " +
            "AND amount < 0 " +  // 添加：只统计转出（金额为负）
            "AND status = 1 " +  // 添加：只统计成功的
            "AND DATE(trans_time) >= #{startDate} " +
            "AND DATE(trans_time) <= #{endDate}")
    BigDecimal sumTransferOutAmount(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}