package com.xiaoguangchen.antex.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**

 * load propeties from given path for given array of files
 * propPath is a directory path. For example
 * <p/>
 * rootDir = /home/mylogin/project
 * propPath = etc/login/host
 * <p/>
 * The loadPath will load the properties from the path of directories /home/mylogin/project/etc/login/host
 * looking property files specified in the array.
 * <p/>
 * The properties will be loaded at rootDir the of the directory tree, starting  at etc -->login-->host
 * At each directory, the properties defined in the file arary are loaded and stored in a Map. The property
 * defined at parent level will be overwriten by the same property defined in the child level directory (last come wins). The other
 * properties will be the union of the parent/child properties at all levels.
 *
 * This class is used by ant "config" last
 *
 *
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class PropertyLoader {

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final SubstituteFormat DEFAULT_SUBSTITUTE_FORMAT = SubstituteFormat.ANT_FORMAT;


    static {
        updateDriveLetterForWindows();
    }

    /**
     * In Windows environment, Dirve always
     * starts with a Drive letter. The drive letter is always capitalized.
     *
     * But if the user use cgwins or other system in windows, the drive
     * letter in cygwin will show as LOWER CASE, in System properties
     *
     * We need to change it back
     */
    private static void updateDriveLetterForWindows() {
        if (!NetUtils.isWindows()) return;
        String userdir = System.getProperty("user.dir");
        userdir = userdir.substring(0, 1).toUpperCase() + userdir.substring(1);
        System.setProperty("user.dir", userdir);
    }

    /**
     * load properties from a url
     *
     * @param url url to load from
     * @return a non-null map
     */
    public static PropertyStore loadUrl(URL url) {
        return loadUrl(null, url);
    }

    /**
     * load properties from a url to a given property map. If the property map is null. it will be initialized first
     * before returned.
     *
     * @param store  - property store
     * @param url    - url to load from
     * @return a non-null Map
     */
    public static PropertyStore  loadUrl(PropertyStore store, URL url) {
        if (url == null) {
            if (store == null)
                return new MapPropertyStoreImpl(new HashMap<String, String>());
            else
                return store;
        }

        if (store == null)
            store = new MapPropertyStoreImpl(new HashMap<String, String>());

        Properties props = loadPropetiesFromUrl(url);
        return putPropertiesIntoStore(store, props);
    }



    /**
     * load properties from a url to a given property map. If the property map is null. it will be initialized first
     * before returned.
     *
     * @param url     url to load from
     * @return  Properties
     */
    public static Properties  loadPropetiesFromUrl(URL url) {
        Properties props = new Properties();
        if (url == null) {
            return props;
        }

        try {
            InputStream is = url.openStream();
            try {
                props.load(is);
                return props;
            } finally {
                if (is != null) {
                    is.close();
                }
            }

        } catch (IOException ex) {
            System.out.println("error loading properties from URI" + url + ", " + ex.getMessage());
        }

        return props;
    }


    /**
     * load properties from a file
     *
     * @param file file to load
     * @return a non-null map
     */
    public static PropertyStore loadFile(File file) {
        return loadFile(null, file);
    }


    /**
     * load properties from a file to a given property store.
     * If the property map is null, it will be initialized first
     *
     * @param store given property Store
     * @param file    file to load
     * @return a non-null PropertyStore
     */

    public static PropertyStore loadFile(PropertyStore store, File file) {

        if (file == null) {
            if (store == null)
                return new MapPropertyStoreImpl( new HashMap<String, String>());
            else
                return store;
        }
        if (store == null)
            store = new MapPropertyStoreImpl( new HashMap<String, String>());

        Properties props = loadPropertiesFromFile(file);
        return putPropertiesIntoStore(store, props);
    }

    /**
     * load properties from a file
     *
     *
     * @param file
     * @return  Properties
     */
    public static Properties loadPropertiesFromFile(File file) {
        Properties props = new Properties();

        if (file == null) {
            return props;
        }
        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                try {
                    props.load(fis);
                } finally {
                    fis.close();
                }
            } else {
                System.out.println("Unable to find property file: " + file.getAbsolutePath());
            }
        } catch (IOException ex) {
            System.out.println("error loading properties from file" + file.getPath() + ", " + ex.getMessage());
        }

        return props;
    }


    /**
     * load properties from a resource
     *
     * @param name name of resource to load
     * @return non-null property Map
     */
    public static PropertyStore loadResource(String name) {
        return loadResource(null, name);
    }

    /**
     * load properties from a resource
     *
     * @param store  - property store
     * @param name    name of resource to load
     * @return non-null Map
     */
    public static PropertyStore loadResource(PropertyStore store, String name) {

        if (name == null || name.length() == 0)
            return new MapPropertyStoreImpl(new HashMap<String, String>());

        if (store == null) {
            store = new MapPropertyStoreImpl(new HashMap<String, String> ());
        }

        Properties props = loadPropertiesFromResource(name);
        //  System.out.println("Resource Loading " + name);
       return putPropertiesIntoStore(store, props);
    }

    public static Properties loadPropertiesFromResource(String name) {
        Properties props = new Properties();

        InputStream is = null;
        try {
            ClassLoader cL = Thread.currentThread().getContextClassLoader();
            if (cL == null) {
                is = ClassLoader.getSystemResourceAsStream(name);
            } else {
                is = cL.getResourceAsStream(name);
            }

            if (is != null) {
                props.load(is);
                return props;
            } else {
                System.out.println("Unable to find resource " + name);
            }
        } catch (IOException ex) {
            System.out.println("error loading properties from resource name = " + name + ", " + ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) { // ignore
                }
            }
        }

      return props;
    }

    /**
     * copy Propertries into given Map or Properties with proper substitution
     *
     * @param store     -given property store
     * @param props     -Properties
     * @return non-null -PropertyStore
     */
    public static PropertyStore putPropertiesIntoStore(PropertyStore store, Properties props) {

        if (props == null) {
            if (store != null)
                return store;
            else
                return new MapPropertyStoreImpl(new HashMap<String, String>());
        }

        if (store == null) store = new MapPropertyStoreImpl(new HashMap<String, String>());

        //due to the sequence of the property is loaded
        //some property has not been loaded into the memory but
        // not into the store yet. Therefore the first pass
        // of the value substitution may not totally work.
        // we need to make two-passes.

        int count = 2;
        do {
            Enumeration e = props.keys();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String value = props.getProperty(name);
                value = checkForSubstituteValue(value, store);
                store.put(name, value);
            }
            count--;
        } while (count > 0);

        return store;
    }


//    /**
//     * turn java.util.property into a Map
//     *
//     * @param props property
//     * @return Map map of properties
//     */
//    public static Map<String, String> getPropertyMap(property props) {
//        return putPropertiesIntoMap(null, props);
//    }

    /**
     * load propeties from given path for given array of files
     * propPath is a directory path. For example
     * <p/>
     * rootDir = /home/mylogin/project
     * propPath = etc/login/host
     * <p/>
     * The loadPath will load the properties from the path of directories /home/mylogin/project/etc/login/host
     * looking property files specified in the array.
     * <p/>
     * The properties will be loaded at rootDir the of the directory tree, starting  at etc -->login-->host
     * At each directory, the properties defined in the file arary are loaded and stored in a Map. The property
     * defined at parent level will be overwriten by the same property defined in the child level directory. The other
     * properties will be the union of the parent/child properties at all levels.
     *
     * @param rootDir   root directory of the test properties
     * @param propPath  path separated by the file separator under rootDir (not including rootDir)
     * @param fileName  Name of the file without path
     *
     * @return PropertyStore containing either Map<String, String> or Properties as store
     */
    public static PropertyStore loadPath(String rootDir, String propPath, String fileName, String filepathSeparator) {
        Set<String> filenameSet = new HashSet<String>(1);
        filenameSet.add(fileName);
        return loadPath(rootDir, propPath, filenameSet, filepathSeparator);
    }

    /**
     * load propeties from given path for given array of files
     * propPath is a directory path. For example
     * <p/>
     * rootDir = /home/mylogin/project
     * propPath = etc/login/host
     * <p/>
     * The loadPath will load the properties from the path of directories /home/mylogin/project/etc/login/host
     * looking property files specified in the array.
     * <p/>
     * The properties will be loaded at rootDir the of the directory tree, starting  at etc -->login-->host
     * At each directory, the properties defined in the file arary are loaded and stored in a Map. The property
     * defined at parent level will be overwriten by the same property defined in the child level directory. The other
     * properties will be the union of the parent/child properties at all levels.
     *
     * @param rootDir   root directory of the test properties
     * @param propPath  path separated by the file separator under rootDir (not including rootDir)
     * @param fileNames Names of the file without path
     * @return PropertyStore containing either Map<String, String> or Properties as store
     */
    public static PropertyStore loadPath(String rootDir, String propPath, Set<String> fileNames, String filepathSeparator) {
        if (fileNames == null || fileNames.isEmpty())
            return new MapPropertyStoreImpl( new HashMap<String, String>());

        return loadPath(rootDir, propPath, fileNames, null, filepathSeparator);
    }

    /**
    *  load propeties from given path for given Set of files. There are also additional path specific files
     * listed in a Map. Each Map key is the directory name, and value is a corresponding set of the file names
     * (without path) in the directory.
     *
     * <p/>
     * rootDir = /home/mylogin/project
     * propPath = etc/login/host
     *
     * etc ---> there are common files: test.properties, db.properties, config.properties, appserver.properties
     * etc/login ---> there are additional files spceific to the this directory: testdata.properties
     * etc/host  ---> there are additional files spceific to the this directory: hostspecific_testdata.properties
     * <p/>
     * <p>
     * When loading properites, the common files are given lower priority and is loaded first, the path specific
     * files are loaded second. So if there is common named (keyed) property, the path specific property will over
     * write the property list on the common files.
     *
     * </p>


     *
     * @see <pre> loadPath(String rootDir, String propPath, Set<String> fileNames) </pre> method for additional information.
     *
     * @param rootDir
     * @param propPath
     * @param commonfileNames
     * @param pathspecificFileNames
     * @return  Map<String, String>
     */
    public static PropertyStore loadPath(String rootDir,
                                         String propPath,
                                         Set<String> commonfileNames,
                                         Map<String, Set<String>> pathspecificFileNames,
                                         String filepathSeparator) {


        if ( (commonfileNames == null || commonfileNames.isEmpty()) &&
             (pathspecificFileNames == null || pathspecificFileNames.isEmpty())) {
            return new MapPropertyStoreImpl(new HashMap<String, String>());
        }

        List<File> dirs = buildDirectoryPath(rootDir, propPath, filepathSeparator);
        return loadPropertyFiles(dirs, commonfileNames, pathspecificFileNames);
    }

    public static PropertyStore loadPath(String rootDir,
                                                 String propPath,
                                                 String[] commonfileNames,
                                                 Map<String, Set<String>> pathspecificFileNames,
                                                 String filepathSeparator) {

        return loadPath(rootDir, propPath, convertArrayToSet(commonfileNames), pathspecificFileNames, filepathSeparator);
     }

	/**
	  * Check for substitute value: Check to see if there are
	  * variables in ${v} format. If variable ${v} has a value already
	  * defined in previous configuration file loaded and in the memory map.
	  * If there is one, the substitude value is then used before further
	  * processing.
	  *
	  * @return  String
	  */
	 public static String checkForSubstituteValue(String srcstr, SubstituteFormat format,  PropertyStore store) {
		 if (srcstr == null || srcstr.length() == 0) return srcstr;

		 StringBuffer sb = null;
		 String       key;
		 String       value;
		 while (0 < srcstr.length()) {
			 int prefixIndex = srcstr.indexOf(format.getStarts());

			 if (prefixIndex < 0) break;

			 int postfixIndex = srcstr.indexOf(format.getEnds(), prefixIndex);
			 if (postfixIndex <= prefixIndex) break;

			 if (postfixIndex > prefixIndex) {

				 if (sb == null) sb = new StringBuffer();

				 //variable without the special ${x} will become -->x
				 key = srcstr.substring(prefixIndex+format.getStarts().length(), postfixIndex );
				 value = null;
				 if (store != null)	value = store.get(key);

				 if (value == null) //no replace value
					 sb.append(srcstr.substring(0, postfixIndex+1));
				 else {
					  sb.append(srcstr.substring(0, prefixIndex)).append(value);
				 }

				 srcstr = srcstr.substring(postfixIndex+1);
			 }
		 }

		 if (sb == null) return srcstr;

		 sb.append(srcstr);

		 return sb.toString();
	 }


	 public static File checkForSubstituteValue(File dir, SubstituteFormat format, PropertyStore store) {

		 String arg = dir.getName();
		 String value = checkForSubstituteValue(arg, format, store);
		 if (!arg.equals(value)) {
			return new File(dir.getParent(), value);
		 }

		 return dir;
	 }

	 public static File checkForSubstituteValue(File dir,  PropertyStore store) {
		 return checkForSubstituteValue(dir, DEFAULT_SUBSTITUTE_FORMAT, store);
	 }

	 public static String checkForSubstituteValue(String srcstr,  PropertyStore store) {
		 return checkForSubstituteValue(srcstr, DEFAULT_SUBSTITUTE_FORMAT, store);
	 }

	//////////////////////////////////////////////////////////////////////////////////////////////
	//private methods

	private static PropertyStore loadPropertyFile(PropertyStore store,File dir, String fileName) {

        store = (store == null) ? new MapPropertyStoreImpl(new HashMap<String, String>()) : store;
        if (fileName == null) return new MapPropertyStoreImpl(new HashMap<String, String>());

        File propFile = new File(dir, fileName);
        if (propFile.exists())
            store= loadFile(store, propFile);

        return store;
    }

    /**
     * load property files into PropertyStore.
     * First load common files properties, then load path specific Files
     *
     * @param dirs
     * @param commonFileNames
     * @param pathFileNameMap
     * @return  Map<String, String>
     */
    private static PropertyStore loadPropertyFiles(List<File> dirs,
                                                   Set<String> commonFileNames,
                                                   Map<String, Set<String>> pathFileNameMap) {
//
//         * parent directories are loaded first, child and grandchild
//         * directories are loaded after. This sequence ensure that
//         * the children's properties overwrite the parent propeties with
//         * same property names.
//         *
//         * Also, for common file names, the ones are listed first will be
//         * loaded first, so the properties with same names will be overwritten
//         * by the last properties files
//         *
//
        Map<String, String> propMap = new HashMap<String, String>();
		PropertyStore store = new MapPropertyStoreImpl(propMap);
		int dirsize = dirs.size();
        for (int i = 0; i < dirsize; ++i) {
            File dir = dirs.get(i);
            dir = checkForSubstituteValue(dir, store);
            if (!dir.exists() || dir.isFile()) {
                throw new IllegalArgumentException("directory: " + dir.getAbsolutePath() + " doesn't exist.");
             }


            store = loadPropertyFiles(store, dir, commonFileNames, pathFileNameMap);
        }

        return store;
    }


  /**
     * load property files into Map. First load common files properties, then load path specific Files
     *
     * @param parentDir
     * @param commonFileNames
     * @param pathFileNameMap
     * @return  Map<String, String>
     */
    private static PropertyStore loadPropertyFiles(PropertyStore    store,
                                                   File             parentDir,
                                                   Set<String>      commonFileNames,
                                                   Map<String, Set<String>> pathFileNameMap) {

//         * For common file names, the ones are listed first will be
//         * loaded first, so the properties with same names will be overwritten
//         * by the last properties files
//         *
            for (String fileName : commonFileNames) {
               fileName = checkForSubstituteValue(fileName,store);
               store = loadPropertyFile(store, parentDir, fileName);
            }

            if (pathFileNameMap == null)  return store;

            Set<String> pathspecificFileNameSet = pathFileNameMap.get(parentDir.getAbsolutePath());
            if (pathspecificFileNameSet == null)  return store;

            for (String fileName: pathspecificFileNameSet) {
                fileName = checkForSubstituteValue(fileName,store);
                store = loadPropertyFile(store, parentDir, fileName);
            }

            return store;
    }



      private static Set<String> convertArrayToSet(String[] commonFileNames) {
        Set<String> commonFileNameSet = new HashSet<String>();
        for (String filename : commonFileNames) {
            if (filename != null && filename.length() > 0)
                commonFileNameSet.add(filename);
        }
       return commonFileNameSet;
    }


    private static List<File> buildDirectoryPath(String rootDir, String propPath, String filepathSeparator) {
        List<File> result = new LinkedList<File>();

        //System.out.println(" working dir = " + System.getProperty("user.dir"));
        File dir = new File(rootDir);
        if (dir.isFile() || !dir.exists())
            new IllegalArgumentException("invalid root directory, the directory must be a directory and exists");
        result.add(dir);
        if (propPath == null || propPath.length() == 0)
            return result;

        File parentDir = dir;

        /**
         * Use a getFilepathSeparator to get the path separator instead of
         * using File.separator or System property. This is because one special case,
         * in Windows, one uses cygwin, the file separator is "/" but windows
         * file separator is "\"
         */
        String[] paths = propPath.split(getFilepathSeparator(filepathSeparator));
        for (String path1 : paths) {
            File path = new File(parentDir, path1);
            result.add(path);
            parentDir = path;
        }

        return result;
    }

    private static String getFilepathSeparator(String filepathSeparator) {
        if (filepathSeparator == null || filepathSeparator.trim().length() == 0)
            return FILE_SEPARATOR;
        else if (!"/".equals(filepathSeparator) && !"\\".equals(filepathSeparator)) {
            System.err.println(" invalid file path separator = " + filepathSeparator);
            return FILE_SEPARATOR;
        }
        else
            return filepathSeparator;
    }



}
