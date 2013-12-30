package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.BuildException;


/**
 *
 * <p>Title: IsNotSet</p>
 *
 * <p>Description: tests whether a given property has NOT been set.</p>
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 *
 * <p>Company: Managing Digital Content LLC</p>
 *
 * @author Chester Chen
 * @version 0.1
 */

public class IsNotSet extends IsSet {

    /**
     * @return true if the property exists
     * @exception BuildException if the property attribute is not set
     */
    public boolean eval() throws BuildException {
        return !super.eval();
    }

}
