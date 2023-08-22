package com.carbonorm.carbonjava.classes;

import com.carbonorm.carbonjava.interfaces.IColorCode;

public class ColorCode implements IColorCode {

    public static void colorCode(String message) {
        System.out.printf(GREEN, message);
    }

    public static void colorCode(String message, String color) {
        System.out.printf(color, message);
    }
}
