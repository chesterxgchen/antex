package com.xiaoguangchen.antex.taskdefs;

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
public class NotEquals extends Equals
{
    public NotEquals()
    {
        super();
    }

    protected boolean shouldExec()
    {
        return !equals();
    }


}
