package com.zychen.bank.mapper;

import com.zychen.bank.model.UserInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserInfoMapper {

    // 插入用户信息
    @Insert("INSERT INTO user_info (user_id, name, id_number, gender, email, address, updated_time) " +
            "VALUES (#{userId}, #{name}, #{idNumber}, #{gender}, #{email}, #{address}, #{updatedTime})")
    int insert(UserInfo userInfo);

    // 根据身份证号查询
    @Select("SELECT * FROM user_info WHERE id_number = #{idNumber}")
    UserInfo findByIdNumber(@Param("idNumber") String idNumber);

    // 根据用户ID查询用户信息
    @Select("SELECT * FROM user_info WHERE user_id = #{userId}")
    UserInfo findByUserId(@Param("userId") String userId);

    // 更新用户信息
    @Update("UPDATE user_info SET name = #{name}, gender = #{gender}, " +
            "birth_date = #{birthDate}, email = #{email}, address = #{address}, " +
            "updated_time = #{updatedTime} WHERE user_id = #{userId}")
    int update(UserInfo userInfo);
}