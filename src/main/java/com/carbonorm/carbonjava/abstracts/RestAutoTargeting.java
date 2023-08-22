package com.carbonorm.carbonjava.abstracts;

import com.carbonorm.carbonjava.CarbonJAVA;
import com.carbonorm.carbonjava.throwables.ThrowableHandler;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;

public abstract class RestAutoTargeting extends RestSettings {

    public Object getFieldValue(String fieldName) {

        Class<?> self = getClass();
        Field primaryFieldField = null; // get the field
        Object primaryField = null; // get the value

        try {
            primaryFieldField = self.getDeclaredField(fieldName);

            primaryFieldField.setAccessible(true); // make it accessible if it's private

            primaryField = primaryFieldField.get(null);

        } catch (NoSuchFieldException | IllegalAccessException e) {

            ThrowableHandler.generateLog(e);

        }

        return primaryField;
    }

    public Object getPrimaryFieldValue() {
        return getFieldValue("PRIMARY");
    }

    public static String autoTargetTableDirectory() throws IOException, JSONException {
        return CarbonJAVA.app_root + CarbonJAVA.app_tables;
    }

}
