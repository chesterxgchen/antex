package com.xiaoguangchen.antex.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Properties-based PropertyStore
 * 
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class PropertyStoreImpl implements PropertyStore {

    private Properties  m_properties;

    public PropertyStoreImpl(Properties hashtable) {
        this.m_properties = hashtable;
    }

    public String get(String s) {
        if (m_properties == null) return null;

        return m_properties.getProperty(s);
    }

    //String property
    public void put(String key, String value) {
        if (m_properties == null) {
            synchronized (this) {
                Properties store = new Properties();
                if (m_properties == null)
                    m_properties = store;
            }
        }

        m_properties.put(key, value);
    }

    public Set<String> getKeySet() {
        if (m_properties == null) return Collections.emptySet();

        Set<Object> keyObjSet = m_properties.keySet();
        if (keyObjSet == null)  return Collections.emptySet();

        Set<String> keyset = new HashSet<String>(keyObjSet.size());
        for (Object o : keyObjSet) {
              keyset.add(o.toString());  //the key must be a string
        }

        return keyset;

    }

    public Properties getStore() {
        return m_properties;
    }
}
