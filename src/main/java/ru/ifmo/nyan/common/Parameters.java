package ru.ifmo.nyan.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Parameters {
    private static Parameters instance;

    private final Properties properties;

    public Parameters() throws IOException {
        properties = new Properties();
        try (InputStream in = new FileInputStream("game_params.properties")) {
            properties.load(in);
        }
    }

    public int get(String prop) {
        String value = properties.getProperty(prop);
        if (value == null)
            throw new IllegalArgumentException(String.format("Property %s is not defined", prop));

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Property is not int: \"%s\"", value));
        }
    }

    public static Parameters getInstance() throws IOException {
        if (instance == null) {
            synchronized (Parameters.class) {
                if (instance == null) {
                    instance = new Parameters();
                }
            }
        }
        return instance;
    }

    public static int getProperty(String prop) throws IOException {
        return getInstance().get(prop);
    }

    public static int getSilently(String prop) {
        try {
            return getProperty(prop);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
