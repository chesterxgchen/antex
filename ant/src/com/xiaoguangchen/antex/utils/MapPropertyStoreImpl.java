package com.xiaoguangchen.antex.utils;
 

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Use a common interface to encapsulate Map, Hashtable and other commons stores
 * with only get() method
 *
 * Map-based PropertyStore
 *
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class MapPropertyStoreImpl implements PropertyStore{
    Map<String, String> m_map;

    public MapPropertyStoreImpl(Map<String, String> mapstore) {
        m_map = mapstore;
    }
    public String get(String s) {
        if (m_map == null) return null;

        return m_map.get(s);
    }

    //String property
    public void put(String key, String value) {
        if (m_map == null) {
            synchronized(this) {
                Map<String, String> store = new HashMap<String, String>();
                if (m_map == null)
                    m_map = store;
            }
        }

        m_map.put(key, value);
    }


    public Map<String, String> getStore() {
        return m_map;
    }

    public Set<String> getKeySet() {
        if (m_map == null) return Collections.emptySet();
        return m_map.keySet();
    }
}
