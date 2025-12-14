package com.zychen.bank.mapper;

import com.zychen.bank.model.Transaction;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TransactionMapper {

    @Insert("INSERT INTO transaction (trans_no, card_id, user_id, trans_type, trans_subtype, " +
            "amount, balance_before, balance_after, fee, currency, status, remark, " +
            "operator_id, operator_type, trans_time, completed_time) " +
            "VALUES (#{transNo}, #{cardId}, #{userId}, #{transType}, #{transSubtype}, " +
            "#{amount}, #{balanceBefore}, #{balanceAfter}, #{fee}, #{currency}, #{status}, #{remark}, " +
            "#{operatorId}, #{operatorType}, #{transTime}, #{completedTime})")
    int insert(Transaction transaction);
}