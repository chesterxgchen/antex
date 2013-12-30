package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * AssertEquals is used for Ant Task self testing
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class AssertEquals extends Equals {
 

	public void execute() throws BuildException {
        //todo: get location
         String msg;

         if (!equals())  {
             msg = "FAILED: ";
             String prop1 = getProject().replaceProperties(m_arg1);
             String value1 = getProject().getProperty(prop1);
             value1  = (value1 == null && m_arg1 != null) ? m_arg1 : value1;
             msg = msg + "arg1 value = " + value1;
         }
          else
             msg = "PASSED";

        getProject().log(this, msg, Project.MSG_ERR);
    }
}
