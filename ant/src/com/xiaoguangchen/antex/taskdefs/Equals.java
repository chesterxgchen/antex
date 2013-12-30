package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

/**
 *
 * <p>Title: Equals</p>
 *
 * <p>Description: if two arguments equals then proceed to generate the elements
 *  if casesenstive is true, perform a case sensitive comparision. Default is true.
 *  if trim is true,trim whitespace from arguments before comparing them. Default is false.
 * </p>
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 *
 * <p>Company: Managing Digital Content LLC</p>
 *
 * @author Chester Chen
 *
 * @version 0.1
 */
public class Equals extends Target
{
    protected String m_arg1  = "";
    protected String m_arg2  = "";

    //Perform a case sensitive comparision. Default is true.
    protected boolean m_isCaseSensitive=true;

    //Trim whitespace from arguments before comparing them. Default is false.
    protected boolean m_trim=false;


    public Equals()
    {
        super();
	}

    public void setArg1(String property1) {
        m_arg1 = property1;
    }

    public void setArg2(String property2) {
        m_arg2 = property2;
    }

    public void execute() throws BuildException {
        if (shouldExec()) {
            super.execute();
        } else if (!shouldExec()) {
            getProject().log(this, "task skipped based on specified condition ", Project.MSG_VERBOSE);
        }
    }

    protected boolean equals() {

         // if this is not a property, then use the value instead.
          String prop1 = getProject().replaceProperties(m_arg1);
          String value1 = getProject().getProperty(prop1);
          value1  = (value1 == null && m_arg1 != null) ? m_arg1 : value1;

          String prop2 = getProject().replaceProperties(m_arg2);
          String value2 = getProject().getProperty(prop2);
          value2  = (value2 == null && m_arg2 != null) ? m_arg2 : value2;

          if (m_trim)  {
            value1 = (value1 == null) ? value1 : value1.trim();
            value2 = (value1 == null) ? value1 : value2.trim();
          }

          //test if arg1 and arg2 are equal
          if (m_isCaseSensitive)
            return ( (value1 == null) ? (value2 == null) : value1.equals(value2));
          else
            return ( (value1 == null) ? (value2 == null) : value1.equalsIgnoreCase(value2));

  }

  protected boolean shouldExec()
  {
      return equals();
  }


}
