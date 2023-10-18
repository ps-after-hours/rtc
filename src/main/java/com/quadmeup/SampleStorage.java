package com.quadmeup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quadmeup.enums.SourceType;

public class SampleStorage {
    
    private List<String> stringList = Collections.synchronizedList(new ArrayList<String>());
    private Map<SourceType, Boolean> lockMap = Collections.synchronizedMap(new HashMap<SourceType, Boolean>());
    private Map<SourceType, Boolean> doneMap = Collections.synchronizedMap(new HashMap<SourceType, Boolean>());
    
    private List<String> duplicateList = Collections.synchronizedList(new ArrayList<String>());

    /**
     * Add a string to the list
     * Return true is the new string was added. Returns false if list already contains the string
     * @param str
     * @return
     */
    public synchronized boolean add(String str) {
        if (stringList.contains(str)) {
            return false;
        } else {
            stringList.add(str);
            return true;
        }
    }
    
    public synchronized String get() {
        if (stringList.size() > 0) {
            return stringList.remove(0);
        } else {
            return null;
        }
    }

    public synchronized void delete(String str) {
        //As we allow to add only one string to the list, we can safely remove the first occurence
        stringList.remove(str);
    }
    
    public synchronized int size() {
        return stringList.size();
    }

    public synchronized void lock(SourceType sourceType) {
        lockMap.put(sourceType, true);
    }

    public synchronized void unlock(SourceType sourceType) {
        lockMap.put(sourceType, false);
    }

    public synchronized boolean isLocked(SourceType sourceType) {
        return lockMap.getOrDefault(sourceType, false);
    }

    public synchronized void markAsDone(SourceType sourceType) {
        doneMap.put(sourceType, true);
    }

    public synchronized boolean isDone(SourceType sourceType) {
        return doneMap.getOrDefault(sourceType, false);
    }

    public synchronized void addDuplicate(String str) {
        duplicateList.add(str);
    }

    public synchronized String getDuplicate() {
        if (duplicateList.size() > 0) {
            return duplicateList.remove(0);
        } else {
            return null;
        }
    }

    public synchronized int duplicateSize() {
        return duplicateList.size();
    }

}