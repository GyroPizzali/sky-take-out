package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        while(!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //select sum(amount) from orders where status = ? and order_time > ? and order_time < ?
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status",Orders.COMPLETED);
            Double dayTurnover = orderMapper.sumByMap(map);
            dayTurnover = dayTurnover == null ? 0.0 : dayTurnover;
            turnoverList.add(dayTurnover);
        }
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(",")));
        turnoverReportVO.setTurnoverList(turnoverList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        return turnoverReportVO;
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        while(!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .newUserList(newUserList.stream().map(Object::toString).collect(Collectors.joining(",")))
                .totalUserList(totalUserList.stream().map(Object::toString).collect(Collectors.joining(",")))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        while(!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer dayOrderCount = getOrderCount(beginTime,endTime,null);
            Integer dayValidOrderCount = getOrderCount(beginTime,endTime,Orders.COMPLETED);
            orderCountList.add(dayOrderCount);
            validOrderCountList.add(dayValidOrderCount);
            totalOrderCount += dayOrderCount;
            validOrderCount += dayValidOrderCount;
        }
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0)
            orderCompletionRate = 1.0 * validOrderCount / totalOrderCount;
        return OrderReportVO.builder()
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .orderCountList(orderCountList.stream().map(Object::toString).collect(Collectors.joining(",")))
                .validOrderCountList(validOrderCountList.stream().map(Object::toString).collect(Collectors.joining(",")))
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> list = orderMapper.getSalesTop10(begin,end);
        String nameList = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.joining(","));
        String numberList = list.stream().map(o -> o.getNumber().toString()).collect(Collectors.joining(","));
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        Integer count = orderMapper.countByMap(map);
        return count;
    }
}
