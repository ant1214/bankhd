package com.zychen.bank.mapper;


import com.zychen.bank.model.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    // 插入新用户
    @Insert("INSERT INTO user (user_id, username, phone, password, role, account_status, created_time) " +
            "VALUES (#{userId}, #{username}, #{phone}, #{password}, #{role}, #{accountStatus}, #{createdTime})")
    int insert(User user);

    // 根据用户名查询
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    // 根据手机号查询
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    // 根据用户ID查询
    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    User findByUserId(@Param("userId") String userId);

    // 更新最后登录时间
    @Update("UPDATE user SET last_login_time = #{lastLoginTime} WHERE user_id = #{userId}")
    int updateLastLoginTime(@Param("userId") String userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    // 更新密码
    @Update("UPDATE user SET password = #{password} WHERE user_id = #{userId}")
    int updatePassword(@Param("userId") String userId, @Param("password") String password);

    @Select("SELECT MAX(user_id) FROM user WHERE user_id LIKE #{prefix}")
    String findMaxUserId(@Param("prefix") String prefix);

    // 更新手机号
    @Update("UPDATE user SET phone = #{phone} WHERE user_id = #{userId}")
    int updatePhone(@Param("userId") String userId, @Param("phone") String phone);


}
