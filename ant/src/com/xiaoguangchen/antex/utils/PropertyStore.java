package com.xiaoguangchen.antex.utils;
 

import java.util.Set;

/**
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public interface PropertyStore{

    public String get(String s);

    //String property
    public void put(String key, String value);

    public Set<String> getKeySet();

}
