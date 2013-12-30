package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class AssertNotEquals extends Equals {

    public void execute() throws BuildException {
        if (equals()) {
            getProject().log(this, "FAILED", Project.MSG_ERR);
        }
        else
            getProject().log(this, "PASSED", Project.MSG_ERR);
    }

}
