package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    //前置通知
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("进行公共自动自动填充");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill annotation = method.getAnnotation(AutoFill.class);
        OperationType operationType = annotation.value();
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0)
            return;
        //获取填入的对象
        Object entity = args[0];
        Class<?> clazz = entity.getClass();
        //自动填入参数
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        try {
            Method setCreateTime = clazz.getMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
            Method setCreateUser = clazz.getMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
            Method setUpdateTime = clazz.getMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
            Method setUpdateUser = clazz.getMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
            //反射修改属性
            if(operationType == OperationType.INSERT){
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            }else if(operationType == OperationType.UPDATE){
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
