package com.zychen.bank.mapper;

import com.zychen.bank.model.UserInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoMapper {

    // 插入用户信息
    @Insert("INSERT INTO user_info (user_id, name, id_number, gender, email, address, updated_time) " +
            "VALUES (#{userId}, #{name}, #{idNumber}, #{gender}, #{email}, #{address}, #{updatedTime})")
    int insert(UserInfo userInfo);

    // 根据身份证号查询
    @Select("SELECT * FROM user_info WHERE id_number = #{idNumber}")
    UserInfo findByIdNumber(@Param("idNumber") String idNumber);
}