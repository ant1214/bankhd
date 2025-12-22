package com.zychen.bank.mapper;

import com.zychen.bank.model.BankCard;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BankCardMapper {

    // 插入银行卡
    @Insert("INSERT INTO bank_card (card_id, user_id, card_password, balance, available_balance, " +
            "frozen_amount, card_type, status, bind_time, daily_limit, monthly_limit) " +
            "VALUES (#{cardId}, #{userId}, #{cardPassword}, #{balance}, #{availableBalance}, " +
            "#{frozenAmount}, #{cardType}, #{status}, #{bindTime}, #{dailyLimit}, #{monthlyLimit})")
    int insert(BankCard bankCard);

    // 根据卡号查询
    @Select("SELECT * FROM bank_card WHERE card_id = #{cardId}")
    BankCard findByCardId(@Param("cardId") String cardId);

    // 根据用户ID查询所有银行卡
    @Select("SELECT * FROM bank_card WHERE user_id = #{userId} AND status != 3 ORDER BY bind_time DESC")
    java.util.List<BankCard> findByUserId(@Param("userId") String userId);

    // 检查卡号是否已存在（包括已解绑的）
    @Select("SELECT COUNT(*) FROM bank_card WHERE card_id = #{cardId}")
    int countByCardId(@Param("cardId") String cardId);

    // 更新余额
    @Update("UPDATE bank_card SET balance = #{balance}, " +
            "available_balance = #{availableBalance}, " +
            "last_transaction_time = #{lastTransactionTime} " +
            "WHERE card_id = #{cardId}")
    int updateBalance(@Param("cardId") String cardId,
                      @Param("balance") BigDecimal balance,
                      @Param("availableBalance") BigDecimal availableBalance,
                      @Param("lastTransactionTime") LocalDateTime lastTransactionTime);

    @Update("UPDATE bank_card SET status = #{status} WHERE card_id = #{cardId}")
    int updateStatus(@Param("cardId") String cardId, @Param("status") Integer status);


    @Select("SELECT COUNT(*) FROM bank_card")
    Long countTotalCards();

    @Select("SELECT COUNT(*) FROM bank_card WHERE status = #{status}")
    Long countCardsByStatus(@Param("status") Integer status);

    @Select("SELECT IFNULL(SUM(balance), 0) FROM bank_card WHERE status = 0")
    BigDecimal getTotalBalance();


    // 查询所有银行卡（带分页和筛选）
    @Select("<script>" +
            "SELECT bc.*, u.username as user_name, ui.name " +
            "FROM bank_card bc " +
            "LEFT JOIN user u ON bc.user_id = u.user_id " +
            "LEFT JOIN user_info ui ON bc.user_id = ui.user_id " +
            "WHERE 1=1 " +
            "<if test='search != null and search != \"\"'>" +
            "  AND (bc.card_id LIKE CONCAT('%', #{search}, '%') OR u.username LIKE CONCAT('%', #{search}, '%') OR ui.name LIKE CONCAT('%', #{search}, '%'))" +
            "</if>" +
            "<if test='status != null'>" +
            "  AND bc.status = #{status}" +
            "</if>" +
            "ORDER BY bc.bind_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Map<String, Object>> findAllCards(
            @Param("search") String search,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    // 统计所有银行卡数量（带筛选）
    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM bank_card bc " +
            "LEFT JOIN user u ON bc.user_id = u.user_id " +
            "LEFT JOIN user_info ui ON bc.user_id = ui.user_id " +
            "WHERE 1=1 " +
            "<if test='search != null and search != \"\"'>" +
            "  AND (bc.card_id LIKE CONCAT('%', #{search}, '%') OR u.username LIKE CONCAT('%', #{search}, '%') OR ui.name LIKE CONCAT('%', #{search}, '%'))" +
            "</if>" +
            "<if test='status != null'>" +
            "  AND bc.status = #{status}" +
            "</if>" +
            "</script>")
    int countAllCards(@Param("search") String search, @Param("status") Integer status);
}