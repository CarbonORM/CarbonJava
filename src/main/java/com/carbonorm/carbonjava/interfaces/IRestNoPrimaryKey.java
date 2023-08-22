package com.carbonorm.carbonjava.interfaces;

import com.carbonorm.carbonjava.throwables.PublicAlert;

import java.util.Map;

public interface IRestNoPrimaryKey {
    // Delete all data from a tables given its primary key
    boolean delete(Map<String, Object> remove, Map<String, Object> argv);

    // Get tables columns given in argv (usually an array) and place them into our array
    boolean get(Map<String, Object> returnData, Map<String, Object> argv);

    // Add an associative array Column => value
    boolean post(Map<String, Object> post) throws PublicAlert;

    // true on success false on failure
    boolean put(Map<String, Object> returnUpdated, Map<String, Object> argv);
}
