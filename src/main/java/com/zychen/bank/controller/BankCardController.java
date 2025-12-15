package com.zychen.bank.controller;

import com.zychen.bank.dto.BindCardDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.service.BankCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.zychen.bank.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cards")
public class BankCardController {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BankCardService bankCardService;
    @Autowired
    private BankCardMapper bankCardMapper;
    /**
     * 绑定银行卡
     */
    @PostMapping("/bind")
    public ResponseEntity<Map<String, Object>> bindCard(
            @Valid @RequestBody BindCardDTO bindCardDTO,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            BankCard bankCard = bankCardService.bindCard(userId, bindCardDTO);

            // 银行卡信息脱敏处理
            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("cardId", maskCardId(bankCard.getCardId()));  // 卡号脱敏
            cardInfo.put("balance", bankCard.getBalance());
            cardInfo.put("availableBalance", bankCard.getAvailableBalance());
            cardInfo.put("cardType", bankCard.getCardType() == 0 ? "储蓄卡" : "信用卡");
            cardInfo.put("status", getStatusText(bankCard.getStatus()));
            cardInfo.put("bindTime", bankCard.getBindTime());
            cardInfo.put("dailyLimit", bankCard.getDailyLimit());
            cardInfo.put("monthlyLimit", bankCard.getMonthlyLimit());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "银行卡绑定成功");
            response.put("data", cardInfo);

            log.info("银行卡绑定成功: 用户={}, 卡号={}", userId, bindCardDTO.getCardId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("银行卡绑定失败: {}", e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);

            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("银行卡绑定异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 查询我的所有银行卡
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyCards(HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            List<BankCard> cards = bankCardService.getUserCards(userId);

            // 处理返回数据（脱敏）
            List<Map<String, Object>> cardList = cards.stream()
                    .map(card -> {
                        Map<String, Object> cardInfo = new HashMap<>();
                        cardInfo.put("cardId", maskCardId(card.getCardId()));
                        cardInfo.put("balance", card.getBalance());
                        cardInfo.put("availableBalance", card.getAvailableBalance());
                        cardInfo.put("cardType", card.getCardType() == 0 ? "储蓄卡" : "信用卡");
                        cardInfo.put("status", getStatusText(card.getStatus()));
                        cardInfo.put("bindTime", card.getBindTime());
                        cardInfo.put("lastTransactionTime", card.getLastTransactionTime());
                        return cardInfo;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", Map.of(
                    "cards", cardList,
                    "total", cardList.size(),
                    "totalBalance", cards.stream()
                            .map(BankCard::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询银行卡列表异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 查询银行卡详情
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<Map<String, Object>> getCardDetail(
            @PathVariable String cardId,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            BankCard bankCard = bankCardService.getCardDetail(cardId, userId);

            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("cardId", maskCardId(bankCard.getCardId()));
            cardInfo.put("balance", bankCard.getBalance());
            cardInfo.put("availableBalance", bankCard.getAvailableBalance());
            cardInfo.put("frozenAmount", bankCard.getFrozenAmount());
            cardInfo.put("cardType", bankCard.getCardType() == 0 ? "储蓄卡" : "信用卡");
            cardInfo.put("status", getStatusText(bankCard.getStatus()));
            cardInfo.put("bindTime", bankCard.getBindTime());
            cardInfo.put("lastTransactionTime", bankCard.getLastTransactionTime());
            cardInfo.put("dailyLimit", bankCard.getDailyLimit());
            cardInfo.put("monthlyLimit", bankCard.getMonthlyLimit());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", cardInfo);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 银行卡号脱敏：622848******5678
    private String maskCardId(String cardId) {
        if (cardId == null || cardId.length() != 12) {
            return cardId;
        }
        return cardId.substring(0, 6) + "******" + cardId.substring(10);
    }

    // 状态码转文本
    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "正常";
            case 1: return "挂失";
            case 2: return "冻结";
            case 3: return "已注销";
            default: return "未知";
        }
    }


    // BankCardController.java - 余额查询接口
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<?> getCardBalance(
            @PathVariable String cardId,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            BankCard bankCard = bankCardMapper.findByCardId(cardId);
            if (bankCard == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 400);
                errorResponse.put("message", "银行卡不存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!bankCard.getUserId().equals(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 400);
                errorResponse.put("message", "无权查询此银行卡");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Map<String, Object> balanceInfo = new HashMap<>();
            balanceInfo.put("cardId", bankCard.getCardId());
            balanceInfo.put("balance", bankCard.getBalance());
            balanceInfo.put("availableBalance", bankCard.getAvailableBalance());
            balanceInfo.put("frozenAmount", bankCard.getFrozenAmount());
            balanceInfo.put("status", bankCard.getStatus());
            balanceInfo.put("statusText", getStatusText(bankCard.getStatus()));
            balanceInfo.put("lastTransactionTime", bankCard.getLastTransactionTime());

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("message", "查询成功");
            successResponse.put("data", balanceInfo);

            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }



}