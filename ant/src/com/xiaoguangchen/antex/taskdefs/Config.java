package com.xiaoguangchen.antex.taskdefs;

import com.xiaoguangchen.antex.utils.PropertyLoader;
import com.xiaoguangchen.antex.utils.PropertyStore;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * <p>Title: Config</p>
 *
 * <p>Description: parse structured-hierachical configuration property files. Allow default and
 *    inheritace, and overwrite of the properties.
 *
 *    The basic idea is that many configuration files are following certain structures:
 *
 *       Parent
 *         |==child
 *              |== Grandchild
 *              ....
 *
 *       The configuration should allow
 *      a. Child's configuration overwrite Parent's configuration and union the rest of prperties with different names
 *      b. If child's configuration(configuration file or property) does not exist, then parent configuration will be the default.
 *      d. The configuration should allow multiple configuration files so that configurations can be
 *      grouped into logical file groups.
 *         for example
 *              Develoment
 *                 |-- config.properties
 *                 |-- db.properties
 *                 |-- test.properties
 *                 |-- developer-specific
 *                       |-- config.properties
 *                       |-- sever.properties
 *                       |-- test.properties
 *                 |-- test-specific
 *                       |-- config.properties
 *                       |-- sever.properties
 *                       |-- test.properties
 *                 |-- weblogic-specific
 *                       |-- config.properties
 *                       |-- sever.properties
 *                       |-- test.properties
 *                 |-- Jboss-specific
 *                       |-- config.properties
 *                       |-- sever.properties
 *                       |-- test.properties

 *      e. The configuration overwrite is at configuration level, not at file level.
 *         For example,
 *                at parent level:   foo="a" bar = "b" are defined in configue.properties
 *                at child level:    foo="A" are defined in child's config.properties
 *
 *         The configuration will return :
 *             foo = "A" and bar = "b"  as result
 *         where
 *             foo = "A" is from child configuration
 *             bar = "b" os from parent configuratio as inherited value
 *
  *
 *   The example configuration consists of the following:
 *   root config directory
 *      config.properties (default)
        db.properties (default)
 *      login (cchen)
 *         config.properties (default)
 *         db.propeties
 *         host (chester)
 *            config1.properties
              test.properties
 *         host (xgchen)
 *            config2.properties
 *
 *   user can app customized structure to fit their own needs.
 *   for example: the following represents application server structures
 *   which allows one to switch from one appserver configuration to another one.
 *      parent-directory (any of the above):
 *            weblogic
 *               appserver.properties
 *            jboss
 *               appserver.properties
 *            websphere
 *               appserver.properties
 *            glassfish
 *               appserver.properties
 *   The following represents database structures
 *   which allows one to switch from one database to next one
 *      parent-directory (any of the above):
 *            oracle
 *               db.properties
 *            mysql
 *              db.properties
 *            mssqlserver
 *              db.properties
 *            db2
 *              db.properties
 *            postgresql
 *              db.properties
 *   The following represents other delivery structure: one can delivery
 *   the finish build to remote distribution, the following structures can
 *   be used to setup different delivery destionation and authentication properties
 *      parent-directory (any of the above):
 *            QA
 *               delivery.properties
 *            test
 *               delivery.properties
 *            dev
 *              delivery.properties
 *
 *--------------------------------------------------------------------------------
 *  <!--
 *      configuration search along the specified path starting at root directory,
 *      with a list of defaullt properties file name(s). The first directory
 *  -->
 *  <config  root = "/opt/config" path ="develop/${user}/${host}" filename="config.properties, db.properties, test.properties" />
 *  <config  root = "/opt/config" path ="develop/test/staging1" filename="config.properties, db.properties, test.properties" />
 *  <config  root = "/opt/config" path ="develop/test/staging2" filename="config.properties, db.properties, test.properties" />
 *
 *  <!-- path with defaullt properties file name(s) -->
 *  <config root = "/opt/config" path = "cchen/host" filename="config.properties, test.properties">
 *      <!-- additional properties files names -->
 *      <fileset dir="/opt/config/developer1">
 *         <include filename="qa.properties"/>
        </fileset>
 *      <fileset dir="config/developer1/host1">
 *        <include filename="*.properties"/>
 *       </fileset>
 *   </config>
 * </p>
 *
 *
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 *
 * <p>Company: Managing Digital Content LLC. </p>
 *
 * @author Chester Chen
 * $Id: $
 */
public class Config extends Task
{
    private String m_path; // if not provided use default path name
    private String m_filenames; // if not provided use default file name
    private String m_root;     // root directory of the configuration

    /**
     * File path separator, when provided use this separator
     * instead of the system properties property (or File.separator).
     * use this separator to overcome the problem, the actually separator
     * is different from normal OS convention. For example, in Windows, the
     * file separator is "\", but if the user uses Cygwin shell, the file
     * path separator is "/".  In this case, the user need to manually set
     * the file path seperator to "/"
     */
    private String m_separator; // file path separator

    protected Vector<FileSet> m_filesets = new Vector<FileSet>();

    ///////////////////////////////////////////////////////////////////////////////

    public String getPath() {
        return m_path;
    }

    public void setPath(final String path) {
        this.m_path = getPropValue(path);
    }

    public String getSeparator() {
        return m_separator;
    }

    public void setSeparator(String separator) {
        this.m_separator = separator;
    }

    /**
     * add a comma "," sperated file names. file name doesn't contain path
     *
     * @param filenames
     */
     public void setFilename(final String filenames) {
        m_filenames = getPropValue(filenames);
     }

    /**
     * specify the root configuration directory
     *
     * @param root
     */
     public void setRoot (final String root) {
       m_root = getPropValue(root);
     }

    /**
     * return the root configuration directory
     *
     * @return String
     */
     public String getRoot(){
       return m_root;
     }

     /**
     * Adds a set of files to copy.
     * @param set a set of files to copy
     */
    public void addFileset(FileSet set) {
        m_filesets.addElement(set);
    }


    /**
     * replace <pre>${value}</pre> with real value, if the property value is not defind,
     * just return the original
     *
     * @param arg
     * @return String the value after the substitution
     */
    private String getPropValue(String arg)  {

       if (arg == null || arg.length() == 0)
         return arg;
       if (arg.startsWith("${") && arg.endsWith("}")) {
         arg = arg.substring(2, arg.length()-1);
       }

       String prop1 = getProject().replaceProperties(arg);
       String value1 = getProject().getProperty(prop1);
       return (value1 == null && arg != null) ? arg : value1;

     }

     private PropertyStore getPropertyStore(String[] commonFilenames,
                                                Map<String, Set<String>> pathSpecificFileNames) {

         //System.out.println(" m_root = " + m_root);
         return PropertyLoader.loadPath(m_root, m_path, commonFilenames, pathSpecificFileNames, m_separator);
     }

     private void initValidate() {
         if ((m_filenames == null || m_filenames.length() ==0) &&  m_filesets.isEmpty()) {
            throw new BuildException("no file name specified", getLocation());
         }

         if (m_root == null || m_root.length() == 0)
            throw new BuildException("root directory must not null.", getLocation());

         File rootDir = new File(m_root);
         if (!rootDir.exists())
            throw new BuildException("root directory:" + rootDir.getAbsolutePath() +" doesn't exist.", getLocation());
     }


    /**
     * common file names are specified by filename attribute and deliminated by ","
     * @return array of filenames.
     */
    private String[] getCommonFileNames() {
        if (m_filenames == null) return new String[0];

        String[] names= m_filenames.split(",");

        for (int i = 0; i < names.length; ++i) {
            if (names[i] == null) continue;
            names[i] = names[i].trim();
        }

        return names;
    }

    /**
     * filename provided by fileset
     * @return a map of directory path to a set of filenames
     */

    private Map<String, Set<String>> getPathSpecificFileNames() {

        Map<String, Set<String>> pathSpecificFileNames = new HashMap<String, Set<String>>();

        // deal with the m_filesets
        for (int i = 0; i < m_filesets.size(); i++) {

            FileSet fs = m_filesets.elementAt(i);

            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for ( String includedDir:  ds.getIncludedDirectories()) {
                log("Warning: " + includedDir + " is skipped without processing.");
            }

            File dir = fs.getDir(getProject());
            String[] fileNames = ds.getIncludedFiles();
            Set<String> fileNameSet = new HashSet<String>(fileNames == null ? 0: fileNames.length);
            if (fileNames != null) {
                for (String name : fileNames)
                    fileNameSet.add(name);
            }
           
            pathSpecificFileNames.put(dir.getAbsolutePath(),fileNameSet);
        }

        return pathSpecificFileNames;
    }

    private void loadPropertyToAntProject(PropertyStore store) {

        if (store == null) return;
        Set<String> keyset = store.getKeySet();
        if (keyset == null || keyset.isEmpty()) return;

        for (String key: keyset) {
            String value = store.get(key);

            Property propTask = (Property) getProject().createTask("property");
            propTask.setName(key);

            //fist check on the property map for any substitute value,
            // then check Ant Project for any replace value
            propTask.setValue(getProject().replaceProperties(PropertyLoader.checkForSubstituteValue(value, store)));
            propTask.execute();
        }
    }

     /**
      *
      * Run task.
      *
      * @exception BuildException if an error occurs
      */
     public void execute() throws BuildException {
         try {
             initValidate();
             String[]  commonFileNames = getCommonFileNames();
             PropertyStore store = getPropertyStore(commonFileNames, getPathSpecificFileNames());
             loadPropertyToAntProject(store);
         }
         catch(Exception ex) {
             throw new BuildException(ex.getMessage(), ex);
         }
     }

}
