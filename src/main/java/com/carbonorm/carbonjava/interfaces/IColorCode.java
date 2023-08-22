package com.carbonorm.carbonjava.interfaces;

public interface IColorCode {
    String BOLD = "\033[1m%s\033[0m";
    String DARK = "\033[2m%s\033[0m";
    String ITALIC = "\033[3m%s\033[0m";
    String UNDERLINE = "\033[4m%s\033[0m";
    String BLINK = "\033[5m%s\033[0m";
    String REVERSE = "\033[7m%s\033[0m";
    String CONCEALED = "\033[8m%s\033[0m";
    String BLACK = "\033[30m%s\033[0m";
    String RED = "\033[31m%s\033[0m";
    String GREEN = "\033[32m%s\033[0m";
    String YELLOW = "\033[33m%s\033[0m";
    String BLUE = "\033[34m%s\033[0m";
    String MAGENTA = "\033[35m%s\033[0m";
    String CYAN = "\033[36m%s\033[0m";
    String WHITE = "\033[37m%s\033[0m";
    String BACKGROUND_BLACK = "\033[40m%s\033[0m";
    String BACKGROUND_RED = "\033[41m%s\033[0m";
    String BACKGROUND_GREEN = "\033[42m%s\033[0m";
    String BACKGROUND_YELLOW = "\033[43m%s\033[0m";
    String BACKGROUND_BLUE = "\033[44m%s\033[0m";
    String BACKGROUND_MAGENTA = "\033[45m%s\033[0m";
    String BACKGROUND_CYAN = "\033[46m%s\033[0m";
    String BACKGROUND_WHITE = "\033[47m%s\033[0m";
}