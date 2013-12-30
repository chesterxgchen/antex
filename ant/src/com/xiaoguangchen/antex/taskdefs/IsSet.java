package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Target;


/**
 *
 * <p>Title: IsSet</p>
 *
 * <p>Description: tests whether a given property has been set.</p>
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 *
 * <p>Company: Managing Digital Content LLC</p>
 *
 * @author Chester Chen
 * @version 0.1
 */

public class IsSet extends Target {

    protected String property;

    /**
     * Set the property attribute
     * @param p the property name
     */
    public void setProperty(String p) {
        property = p;
    }

    /**
     * @return true if the property exists
     * @exception BuildException if the property attribute is not set
     */
    public boolean eval() throws BuildException {
        if (property == null) {
            throw new BuildException("No property specified for isset condition");
        }
        return getProject().getProperty(property) != null;
    }

    public void execute() throws BuildException {
        if (eval()) {
            super.execute();
        }
    }


}
