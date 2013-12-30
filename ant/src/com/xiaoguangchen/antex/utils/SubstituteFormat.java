package com.xiaoguangchen.antex.utils;
            

/**
 * SubstituteFormat defines a format of variable substituion.
 *
 * The main assumption of the format is that a substitution format is consistent of
 * prefix and postfix. Example of the format are:
 * <P>
 * ANT_FORMAT: ${v}
 * ANT_TOKEN_FORAMT: @v@
  * </P>
 *
 * One can define additional format to suite ones need.
 *
 *
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class SubstituteFormat {

       public String m_format;
       public String m_starts;
       public String m_ends;

       public SubstituteFormat(String format, String starts, String ends) {
           this.m_format = format;
           this.m_starts = starts;
           this.m_ends = ends;
       }

    public String getFormat() {
        return m_format;
    }

    public void setFormat(String m_format) {
        this.m_format = m_format;
    }

    public String getStarts() {
        return m_starts;
    }

    public void setStarts(String m_starts) {
        this.m_starts = m_starts;
    }

    public String getEnds() {
        return m_ends;
    }

    public void setEnds(String m_ends) {
        this.m_ends = m_ends;
    }

    public static final SubstituteFormat ANT_FORMAT   = new SubstituteFormat("ant_foramt", "${", "}");
    public static final SubstituteFormat TOKEN_FORMAT = new SubstituteFormat("token_foramt", "@", "@");
}
