package com.mcmp.o11ymanager.manager.global.aspect;

import com.mcmp.o11ymanager.manager.global.annotation.Base64Decode;
import com.mcmp.o11ymanager.manager.global.annotation.Base64DecodeField;
import com.mcmp.o11ymanager.manager.global.annotation.Base64EncodeField;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class Base64Aspect {

    @Before("@annotation(com.mcmp.o11ymanager.manager.global.annotation.Base64Decode)")
    public void base64DecodeString(JoinPoint jp) throws Exception {
        var args = Arrays.stream(jp.getArgs()).toList();

        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        var targetClassList =
                Arrays.stream(methodSignature.getMethod().getAnnotation(Base64Decode.class).value())
                        .toList();

        var filterArgs =
                args.stream()
                        .filter(f -> f != null && targetClassList.contains(f.getClass()))
                        .toList();

        for (var arg : filterArgs) {
            var filterFields =
                    Arrays.stream(arg.getClass().getDeclaredFields())
                            .filter(
                                    f ->
                                            Arrays.stream(f.getDeclaredAnnotations())
                                                    .anyMatch(
                                                            annotation ->
                                                                    annotation
                                                                            .annotationType()
                                                                            .equals(
                                                                                    Base64DecodeField
                                                                                            .class)))
                            .toList();

            processFields(arg, filterFields);
        }
    }

    @AfterReturning(
            value = "@annotation(com.mcmp.o11ymanager.manager.global.annotation.Base64Encode)",
            returning = "response")
    public void base64EncodeString(JoinPoint jp, Object response) throws Exception {
        if (response == null) {
            return;
        }

        var fields =
                Arrays.stream(response.getClass().getDeclaredFields())
                        .filter(
                                field ->
                                        Arrays.stream(field.getAnnotations())
                                                .anyMatch(
                                                        annotation ->
                                                                annotation
                                                                        .annotationType()
                                                                        .equals(
                                                                                Base64EncodeField
                                                                                        .class)))
                        .toList();

        for (var field : fields) {
            var vmMethodName = toUppercase(field.getName());
            var methods = Arrays.stream(response.getClass().getDeclaredMethods()).toList();

            var vmGetMethod = findMethod(methods, "get" + vmMethodName);
            var vmSetMethod = findMethod(methods, "set" + vmMethodName);

            if (vmGetMethod.isEmpty()) {
                continue;
            }

            var getInvoke = vmGetMethod.get().invoke(response);
            if (getInvoke == null) {
                continue;
            }

            if (vmSetMethod.isPresent() && String.class.equals(getInvoke.getClass())) {
                var plainText = (String) getInvoke;
                if (!StringUtils.isBlank(plainText)) {
                    var encodedText =
                            Base64.getEncoder()
                                    .encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
                    vmSetMethod.get().invoke(response, encodedText);
                }
            } else if (ArrayList.class.equals(getInvoke.getClass())) {
                var list = (List<?>) getInvoke;
                for (var o : list) {
                    base64EncodeString(jp, o);
                }
            } else {
                base64EncodeString(jp, getInvoke);
            }
        }
    }

    private void processFields(Object obj, List<Field> fields) throws Exception {
        for (var field : fields) {
            var vmMethodName = toUppercase(field.getName());
            var methods = Arrays.stream(obj.getClass().getDeclaredMethods()).toList();

            var vmGetMethod = findMethod(methods, "get" + vmMethodName);
            var vmSetMethod = findMethod(methods, "set" + vmMethodName);

            if (vmGetMethod.isPresent() && vmSetMethod.isPresent()) {
                var plainText = (String) vmGetMethod.get().invoke(obj);
                if (!StringUtils.isBlank(plainText)) {
                    var decodedText =
                            new String(
                                    Base64.getDecoder()
                                            .decode(plainText.getBytes(StandardCharsets.UTF_8)),
                                    StandardCharsets.UTF_8);
                    vmSetMethod.get().invoke(obj, decodedText);
                }
            }
        }
    }

    private Optional<Method> findMethod(List<Method> methods, String methodName) {
        return methods.stream().filter(method -> method.getName().equals(methodName)).findFirst();
    }

    private static String toUppercase(String vmStr) {
        return vmStr.length() > 1
                ? vmStr.substring(0, 1).toUpperCase() + vmStr.substring(1)
                : vmStr.substring(0, 1).toUpperCase();
    }
}
