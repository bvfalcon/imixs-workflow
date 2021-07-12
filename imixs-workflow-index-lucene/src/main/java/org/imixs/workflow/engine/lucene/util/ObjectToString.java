package org.imixs.workflow.engine.lucene.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.json.JsonObject;

import org.imixs.workflow.util.JSONParser;

public class ObjectToString {
    public static String convert(Object value) {
        String convertedValue = "";

        if (value instanceof Calendar || value instanceof Date) {
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

            // convert calendar to lucene string representation
            String sDateValue;
            if (value instanceof Calendar) {
                sDateValue = dateformat.format(((Calendar) value).getTime());
            } else {
                sDateValue = dateformat.format((Date) value);
            }
            convertedValue = sDateValue;
        } else if (value instanceof JsonObject && ((JsonObject) value).containsKey("prettyValue")) {
            convertedValue = ((JsonObject) value).getString("prettyValue");
        } else if (value instanceof String && ((String) value).startsWith("{")) {
            String prettyValue = JSONParser.getKey("prettyValue", (String) value);
            convertedValue = prettyValue != null ? prettyValue : (String) value;
        } else {
            // default
            convertedValue = value.toString();
        }
        return convertedValue;
    }
}
