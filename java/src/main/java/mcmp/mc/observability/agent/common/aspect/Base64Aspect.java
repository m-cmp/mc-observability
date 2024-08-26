package mcmp.mc.observability.agent.common.aspect;

import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Component
public class Base64Aspect {
    @Before(value = "@annotation(mcmp.mc.observability.agent.common.annotation.Base64Decode)")
    public void base64DecodeString(JoinPoint jp) throws Exception {
        List<Object> args  = Arrays.stream(jp.getArgs()).collect(Collectors.toList());

        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        List<Class> targetClassList = Arrays.stream(methodSignature.getMethod().getAnnotation(Base64Decode.class).value()).collect(Collectors.toList());
        List<Object> filterArgs = args.stream().filter(f -> targetClassList.contains(f.getClass())).collect(Collectors.toList());

        for( Object arg : filterArgs ) {
            List<Field> filterFields = Arrays.stream(arg.getClass().getDeclaredFields()).filter(f -> Arrays.stream(f.getDeclaredAnnotations())
                    .anyMatch(annotation -> annotation.annotationType().equals(Base64DecodeField.class))).collect(Collectors.toList());

            for( Field field : filterFields ) {
                String targetMethodName = toUppercase(field.getName());

                List<Method> methods = Arrays.stream(arg.getClass().getDeclaredMethods()).collect(Collectors.toList());
                Optional<Method> targetGetMethod = methods.stream().filter(method -> method.getName().equals("get" + targetMethodName)).findFirst();
                Optional<Method> targetSetMethod = methods.stream().filter(method -> method.getName().equals("set" + targetMethodName)).findFirst();

                if (targetGetMethod.isPresent() && targetSetMethod.isPresent()) {
                    String plainText = (String) targetGetMethod.get().invoke(arg);
                    if (!StringUtils.isBlank(plainText)) {
                        String decodedText = new String(Base64.getDecoder().decode(plainText.getBytes()));
                        targetSetMethod.get().invoke(arg, decodedText);
                    }
                }
            }
        }
    }

    @AfterReturning(value = "@annotation(mcmp.mc.observability.agent.common.annotation.Base64Encode)", returning = "response")
    public void base64EncodeString(JoinPoint jp, Object response) throws Exception {
        List<Field> fields = Arrays.stream(response.getClass().getDeclaredFields()).collect(Collectors.toList());

        for( Field field : fields ) {
            if( Arrays.stream(field.getAnnotations()).anyMatch(annotation -> annotation.annotationType().equals(Base64EncodeField.class)) ) {
                String targetMethodName = toUppercase(field.getName());

                List<Method> methods = Arrays.stream(response.getClass().getDeclaredMethods()).collect(Collectors.toList());
                Optional<Method> targetGetMethod = methods.stream().filter(method -> method.getName().equals("get" + targetMethodName)).findFirst();
                Optional<Method> targetSetMethod = methods.stream().filter(method -> method.getName().equals("set" + targetMethodName)).findFirst();

                Object getInvoke = targetGetMethod.get().invoke(response);

                if( targetGetMethod.isPresent() && targetSetMethod.isPresent() && getInvoke != null && targetGetMethod.get().invoke(response).getClass().equals(String.class) ) {
                    String plainText = (String) targetGetMethod.get().invoke(response);
                    if (!StringUtils.isBlank(plainText)) {
                        String encodedText = new String(Base64.getEncoder().encode(plainText.getBytes(StandardCharsets.UTF_8)));
                        targetSetMethod.get().invoke(response, encodedText);
                    }
                }
                else if( targetGetMethod.isPresent() && getInvoke != null && targetGetMethod.get().invoke(response).getClass().equals(ArrayList.class) ) {
                    List<Object> list = (List)targetGetMethod.get().invoke(response);
                    for( Object o : list ) base64EncodeString(jp, o);
                }
                else if( targetGetMethod.isPresent() && getInvoke != null ) {
                    base64EncodeString(jp, getInvoke);
                }
            }
        }
    }

    private static String toUppercase(String targetStr) {
        return targetStr.length() > 1 ? targetStr.substring(0, 1).toUpperCase() + targetStr.substring(1, targetStr.length()) : targetStr.substring(0, 1);
    }
}