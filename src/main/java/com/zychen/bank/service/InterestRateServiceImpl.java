package com.zychen.bank.service;


import com.zychen.bank.mapper.InterestRateMapper;
import com.zychen.bank.model.InterestRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class InterestRateServiceImpl implements InterestRateService {

    @Autowired
    private InterestRateMapper interestRateMapper;

    @Override
    public List<InterestRate> getAllInterestRates() {
        try {
            return interestRateMapper.findAllActiveRates();
        } catch (Exception e) {
            log.error("获取利率配置失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getFormattedInterestRates() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取活期利率
            InterestRate currentRate = interestRateMapper.findCurrentDepositRate();
            if (currentRate != null) {
                Map<String, Object> current = new HashMap<>();
                current.put("rate", currentRate.getAnnualRate());
                current.put("name", "活期存款");
                current.put("description", "按日计息，按季结息");
                current.put("effective_date", currentRate.getEffectiveDate());
                result.put("current", current);
            }

            // 获取定期利率
            List<InterestRate> fixedRates = interestRateMapper.findAllActiveRates();
            List<Map<String, Object>> fixedRateList = new ArrayList<>();

            for (InterestRate rate : fixedRates) {
                if (rate.getAccountType().startsWith("FIXED_")) {
                    Map<String, Object> fixedRate = new HashMap<>();
                    fixedRate.put("term_months", rate.getTermMonths());
                    fixedRate.put("rate", rate.getAnnualRate());

                    // 根据期限设置名称
                    String termName = getTermName(rate.getTermMonths());
                    fixedRate.put("name", termName);
                    fixedRate.put("effective_date", rate.getEffectiveDate());

                    fixedRateList.add(fixedRate);
                }
            }

            // 按期限排序
            fixedRateList.sort(Comparator.comparing(m -> (Integer) m.get("term_months")));
            result.put("fixed_rates", fixedRateList);
            result.put("last_updated", new Date());

        } catch (Exception e) {
            log.error("格式化利率信息失败", e);
            // 返回默认值
            result = getDefaultRates();
        }

        return result;
    }

    @Override
    public BigDecimal getRateByTerm(Integer termMonths) {
        try {
            InterestRate rate = interestRateMapper.findFixedRateByTerm(termMonths);
            return rate != null ? rate.getAnnualRate() : new BigDecimal("0.0150");
        } catch (Exception e) {
            log.error("获取定期利率失败，期限：{}", termMonths, e);
            // 返回默认利率
            return getDefaultRateByTerm(termMonths);
        }
    }

    @Override
    public BigDecimal getCurrentRate() {
        try {
            InterestRate rate = interestRateMapper.findCurrentDepositRate();
            return rate != null ? rate.getAnnualRate() : new BigDecimal("0.0035");
        } catch (Exception e) {
            log.error("获取活期利率失败", e);
            return new BigDecimal("0.0035");
        }
    }

    private String getTermName(Integer termMonths) {
        if (termMonths == null) return "未知期限";

        switch (termMonths) {
            case 3: return "3个月定期";
            case 6: return "6个月定期";
            case 12: return "1年定期";
            case 24: return "2年定期";
            case 36: return "3年定期";
            default: return termMonths + "个月定期";
        }
    }

    private Map<String, Object> getDefaultRates() {
        Map<String, Object> result = new HashMap<>();

        // 默认活期利率
        Map<String, Object> current = new HashMap<>();
        current.put("rate", new BigDecimal("0.0035"));
        current.put("name", "活期存款");
        current.put("description", "按日计息，按季结息");
        result.put("current", current);

        // 默认定期利率
        List<Map<String, Object>> fixedRates = new ArrayList<>();
        int[] terms = {3, 6, 12, 24, 36};
        double[] rates = {0.0100, 0.0130, 0.0150, 0.0200, 0.0250};
        String[] names = {"3个月定期", "6个月定期", "1年定期", "2年定期", "3年定期"};

        for (int i = 0; i < terms.length; i++) {
            Map<String, Object> fixedRate = new HashMap<>();
            fixedRate.put("term_months", terms[i]);
            fixedRate.put("rate", new BigDecimal(String.valueOf(rates[i])));
            fixedRate.put("name", names[i]);
            fixedRates.add(fixedRate);
        }

        result.put("fixed_rates", fixedRates);
        result.put("last_updated", new Date());
        result.put("note", "使用默认利率配置");

        return result;
    }

    private BigDecimal getDefaultRateByTerm(Integer termMonths) {
        if (termMonths == null) return new BigDecimal("0.0150");

        switch (termMonths) {
            case 3: return new BigDecimal("0.0100");
            case 6: return new BigDecimal("0.0130");
            case 12: return new BigDecimal("0.0150");
            case 24: return new BigDecimal("0.0200");
            case 36: return new BigDecimal("0.0250");
            default: return new BigDecimal("0.0150");
        }
    }
}