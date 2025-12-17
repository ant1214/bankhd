package com.zychen.bank.service;


import com.zychen.bank.dto.OperationLogQueryDTO;
import com.zychen.bank.mapper.OperationLogMapper;
import com.zychen.bank.mapper.UserMapper;
import com.zychen.bank.model.OperationLog;
import com.zychen.bank.model.User;
import com.zychen.bank.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void logOperation(String userId, Integer userRole, String module,
                             String operationType, String operationDetail,
                             String targetType, String targetId,
                             String ipAddress, String userAgent,
                             Integer status, String errorMessage, Integer executionTime) {

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserRole(userRole);
        log.setModule(module);
        log.setOperationType(operationType);
        log.setOperationDetail(operationDetail);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setExecutionTime(executionTime);
        log.setCreatedTime(LocalDateTime.now());

        operationLogMapper.insert(log);
    }

    @Override
    public Map<String, Object> getOperationLogs(OperationLogQueryDTO queryDTO) {
        Map<String, Object> result = new HashMap<>();

        // 计算分页偏移量
        int offset = (queryDTO.getPage() - 1) * queryDTO.getPageSize();

        // 查询日志列表
        List<OperationLog> logs = operationLogMapper.findLogs(queryDTO, offset, queryDTO.getPageSize());

        // 查询总数
        int total = operationLogMapper.countLogs(queryDTO);

        // 转换日志数据，添加用户姓名
        List<Map<String, Object>> logList = logs.stream().map(log -> {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("logId", log.getLogId());
            logMap.put("userId", log.getUserId());
            logMap.put("userRole", log.getUserRole());
            logMap.put("userRoleText", getRoleText(log.getUserRole()));

            // 尝试获取用户姓名
            String userName = "未知用户";
            if (log.getUserId() != null) {
                User user = userMapper.findByUserId(log.getUserId());
                if (user != null) {
                    // 需要根据你的实际结构获取姓名
                    // 这里假设User实体有getName()方法，如果没有需要调整
                    userName = user.getUsername(); // 或从userInfo获取真实姓名
                }
            }
            logMap.put("userName", userName);

            logMap.put("module", log.getModule());
            logMap.put("moduleText", getModuleText(log.getModule()));
            logMap.put("operationType", log.getOperationType());
            logMap.put("operationTypeText", getOperationTypeText(log.getOperationType()));
            logMap.put("operationDetail", log.getOperationDetail());
            logMap.put("targetType", log.getTargetType());
            logMap.put("targetId", log.getTargetId());
            logMap.put("ipAddress", log.getIpAddress());
            logMap.put("status", log.getStatus());
            logMap.put("statusText", getStatusText(log.getStatus()));
            logMap.put("createdTime", log.getCreatedTime());
            logMap.put("executionTime", log.getExecutionTime());
            logMap.put("errorMessage", log.getErrorMessage());
            return logMap;
        }).toList();

        result.put("logs", logList);
        result.put("pagination", Map.of(
                "page", queryDTO.getPage(),
                "pageSize", queryDTO.getPageSize(),
                "total", total,
                "totalPages", (int) Math.ceil((double) total / queryDTO.getPageSize())
        ));

        return result;
    }

    // 辅助方法：角色转文本
    private String getRoleText(Integer role) {
        if (role == null) return "未知";
        return role == 1 ? "管理员" : "用户";
    }

    // 辅助方法：模块转文本
    private String getModuleText(String module) {
        if (module == null) return "未知";
        switch (module) {
            case "AUTH": return "认证";
            case "CARD": return "银行卡";
            case "TRANSACTION": return "交易";
            case "SECURITY": return "安全控制";
            case "USER": return "用户管理";
            case "FIXED_DEPOSIT": return "定期存款";
            default: return module;
        }
    }

    // 辅助方法：操作类型转文本
    private String getOperationTypeText(String operationType) {
        if (operationType == null) return "未知";
        switch (operationType) {
            case "LOGIN": return "登录";
            case "LOGOUT": return "退出";
            case "REGISTER": return "注册";
            case "BIND_CARD": return "绑定银行卡";
            case "UNBIND_CARD": return "解绑银行卡";
            case "DEPOSIT": return "存款";
            case "WITHDRAW": return "取款";
            case "FREEZE_CARD": return "冻结银行卡";
            case "UNFREEZE_CARD": return "解冻银行卡";
            case "CREATE_FD": return "创建定期存款";
            case "EARLY_WITHDRAW_FD": return "提前支取定期存款";
            case "MATURE_FD": return "到期转出定期存款";
            case "CHANGE_PASSWORD": return "修改密码";
            case "UPDATE_INFO": return "更新信息";
            case "ADD_ADMIN": return "添加管理员";
            case "ADMIN_FREEZE": return "管理员冻结";
            case "ADMIN_UNFREEZE": return "管理员解冻";
            case "LOST_REPORT": return "挂失";
            case "CANCEL_LOST": return "解挂";
            default: return operationType;
        }
    }

    // 辅助方法：状态转文本
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "成功" : "失败";
    }
}