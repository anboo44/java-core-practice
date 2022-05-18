package com.learn.java;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldUtil {

    private static final String POSTFIX = "Entity";
    private static final String ID_KEY = "Id";

    public static <T>Boolean isFieldMatching(T t, String fieldTarget) {
        var fieldNames = Arrays.stream(
                t.getClass().getDeclaredFields()
        ).map(Field::getName).collect(Collectors.toList());

        return fieldNames.contains(fieldTarget);
    }

    public static <T>String convertIdField(T t, String field) {
        if (field.equalsIgnoreCase(ID_KEY)) {
            var tmp = t.getClass().getName().split(".");
            String className = tmp[tmp.length - 1];
            String preName = className.substring(0, className.length() - POSTFIX.length());

            return preName.toLowerCase() + ID_KEY;
        } else {
            return field;
        }
    }

    public static void main(String[] args) {
        var x = Optional.ofNullable(null);
        System.out.println(x.orElse("sdsd"));
    }
}
