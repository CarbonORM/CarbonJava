package com.carbonorm.carbonjava.interfaces;

import com.carbonorm.carbonjava.throwables.PublicAlert;

import java.util.Map;

public interface IRestSinglePrimaryKey {

    // Delete all data from a tables given its primary key
    boolean delete(Map<String, Object> remove, String primary, Map<String, Object> argv);

    // Get tables columns given in argv (usually an array) and place them into our array
    boolean get(Map<String, Object> returnData, String primary, Map<String, Object> argv);

    // Add an associative array Column => value
    Object post(Map<String, Object> post) throws PublicAlert;

    // true on success false on failure
    boolean put(Map<String, Object> returnUpdated, String primary, Map<String, Object> argv);
}
