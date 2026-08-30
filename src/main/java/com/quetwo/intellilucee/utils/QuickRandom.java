package com.quetwo.intellilucee.utils;

import java.util.Random;
import java.util.stream.Collectors;

public class QuickRandom
{
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-=+.,~";
    private static final Random RANDOM = new Random();

    public static String generateString(int length)
    {
        return RANDOM.ints(length, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

}
