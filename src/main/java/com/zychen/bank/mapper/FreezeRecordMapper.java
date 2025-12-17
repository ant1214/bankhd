package com.zychen.bank.mapper;

import com.zychen.bank.model.FreezeRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FreezeRecordMapper {

    // 插入冻结记录
    @Insert("INSERT INTO freeze_record(freeze_no, freeze_type, freeze_level, " +
            "target_id, user_id, card_id, reason_type, reason_detail, " +
            "freeze_time, operator_id, operator_role, status) " +
            "VALUES(#{freezeNo}, #{freezeType}, #{freezeLevel}, " +
            "#{targetId}, #{userId}, #{cardId}, #{reasonType}, #{reasonDetail}, " +
            "#{freezeTime}, #{operatorId}, #{operatorRole}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(FreezeRecord freezeRecord);

    // 根据用户ID查询冻结记录
    @Select("SELECT * FROM freeze_record WHERE user_id = #{userId} " +
            "ORDER BY freeze_time DESC")
    List<FreezeRecord> findByUserId(@Param("userId") String userId);

    // 根据银行卡号查询冻结记录
    @Select("SELECT * FROM freeze_record WHERE card_id = #{cardId} " +
            "ORDER BY freeze_time DESC")
    List<FreezeRecord> findByCardId(@Param("cardId") String cardId);

    // 查询冻结中的记录
    @Select("SELECT * FROM freeze_record WHERE card_id = #{cardId} AND status = 1")
    FreezeRecord findActiveFreezeByCardId(@Param("cardId") String cardId);

    // 更新冻结状态
    @Update("UPDATE freeze_record SET status = #{status}, unfreeze_time = #{unfreezeTime} " +
            "WHERE record_id = #{recordId}")
    int updateStatus(@Param("recordId") Long recordId,
                     @Param("status") Integer status,
                     @Param("unfreezeTime") LocalDateTime unfreezeTime);

    // 根据记录ID查询
    @Select("SELECT * FROM freeze_record WHERE record_id = #{recordId}")
    FreezeRecord findById(@Param("recordId") Long recordId);

    // 根据用户ID查询活跃的账户冻结记录
    @Select("SELECT * FROM freeze_record WHERE user_id = #{userId} AND freeze_type = #{freezeType} AND status = 1")
    List<FreezeRecord> findActiveByUserId(@Param("userId") String userId, @Param("freezeType") Integer freezeType);

    // 根据卡号查询所有活跃的冻结记录（可能有多条）
    @Select("SELECT * FROM freeze_record WHERE card_id = #{cardId} AND status = 1")
    List<FreezeRecord> findActiveByCardId(@Param("cardId") String cardId);

    // 根据冻结编号查询
    @Select("SELECT * FROM freeze_record WHERE freeze_no = #{freezeNo}")
    FreezeRecord findByFreezeNo(@Param("freezeNo") String freezeNo);

    // 更新整个冻结记录对象
    @Update("UPDATE freeze_record SET unfreeze_time = #{unfreezeTime}, status = #{status}, " +
            "planned_unfreeze_time = #{plannedUnfreezeTime} WHERE record_id = #{recordId}")
    int update(FreezeRecord record);

    // 查询用户自己冻结的活跃记录
    @Select("SELECT * FROM freeze_record WHERE card_id = #{cardId} AND user_id = #{userId} AND freeze_level = 1 AND status = 1")
    FreezeRecord findActiveUserFreezeByCardId(@Param("cardId") String cardId, @Param("userId") String userId);

}
