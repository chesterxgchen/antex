package com.xiaoguangchen.antex.taskdefs;

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import java.util.Vector;
import java.io.File;

import com.xiaoguangchen.antex.taskdefs.compilers.Javac6ExternalCompilerAdapter;

/**
 * <p>Description:
 *    Javac ant tasks for Java 1.6 or later. Add additional attributes
 *    not included in the Javac ant tasks. This class is adopted from Apt
 *    ant task
 * 
 * </p>
 * Copyright (c) 2009 Xiaoguang Chen.</p>
 *
 * @author Chester Chen (Xiaoguang Chen) chesterxgchen@yahoo.com
 */
public class Javac6 extends Javac {

  //  private static final String JAVAC16 = "javac1.6";

    private boolean compile = true;
    private String processingOption;
    private String processor;
    private String factory;
    private Path   processorpath;
    private Vector options    = new Vector();
    private Vector processors = new Vector();
    private Vector jvmargs    = new Vector();

    private File   sourcegendir;

    public static final String PROC_ONLY = "only";
    public static final String PROC_NONE = "none";

    /** The name of the javac. */
    public static final String EXECUTABLE_NAME = "javac";

    /** A warning message if used with java < 1.6. */
    public static final String ERROR_WRONG_JAVA_VERSION = "javac6 task requires Java 1.6+";

    /** An warning message when ignoring compiler attribute. */
    public static final String ERROR_IGNORING_COMPILER_OPTION = "Ignoring compiler attribute for the Javac6 task, as it is fixed";

    public static final String WARNING_IGNORING_FORK =  "Javac6 only runs in its own JVM; fork=false option ignored";

    /** An warning error when invalid processing option  */
    public static final String ERROR_INVALID_PROCESSING_OPTION = "Invalid processing option: must be: none or only.";
    

    /**
     * The nested option element.
     */
    public static final class Option {
        private String name;
        private String value;

        /** Constructor for Option */
        public Option() {
            //default
        }

        /**
         * Get the name attribute.
         * @return the name attribute.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name attribute.
         * @param name the name of the option.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the value attribute.
         * @return the value attribute.
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the value attribute.
         * @param value the value of the option.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }


    /**
     * The nested processor element.
     */
    public static final class Processor {
        private String className;

        /** Constructor for Processor */
        public Processor() {
            //default
        }

        /**
         * Get the name attribute.
         * @return the name attribute.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Set the name attribute.
         * @param name the name of the option.
         */
        public void setClassName(String name) {
            this.className = name;
        }
    }

    /**
     * The nested processor jvmargs.
     */
    public static final class Jvmarg {
        private String value;

        /** Constructor for Jvmarg */
        public Jvmarg() {
            //default
        }

        /**
         * Get the value attribute.
         * @return the value attribute.
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the name attribute.
         * @param value the value of the args
         */
        public void setValue(String value) {
            this.value = value;
        }
    }


    /**
     * Construtor for Javac6 task.
     */
    public Javac6() {
        super();
        super.setCompiler(Javac6ExternalCompilerAdapter.class.getName());
        setFork(true);
    }

    public String getJavac6Executable() {
        return EXECUTABLE_NAME;
    }


    /**
     * Set the compiler.
     * This is not allowed and a warning log message is made.
     * @param compiler not used.
     */
    public void setCompiler(String compiler) {
        log(ERROR_IGNORING_COMPILER_OPTION, Project.MSG_WARN);
    }

    /**
     * Set the fork attribute.
     * @param fork if false; warn the option is ignored.
     */
    public void setFork(boolean fork) {
        if (!fork) {
            log(WARNING_IGNORING_FORK, Project.MSG_WARN);
        }
    }
//
//    /**
//     * Get the compiler class name.
//     * @return the compiler class name.
//     */
//    public String getCompiler() {
//       return JAVAC16;
//    }

    /**
     * get processing option, should be either none or only
     * if this is not null, -proc:{none,only} will be used. 
     *
     * @return  processing option.
     */
    public String getProcessingOption() {
        return this.processingOption;
    }




    /**
     * get processing option, should be either none or only
     * if this is not null, -proc:{none,only} will be used.
     
     * @param processingOption
     */
    public void setProcessingOption(String processingOption) {
        this.processingOption = processingOption;

        if (!processingOption.equals(PROC_NONE) &&
            !processingOption.equals(PROC_ONLY)) {
            log(ERROR_INVALID_PROCESSING_OPTION, Project.MSG_ERR);            
        }
    }

    /**
     * Add a reference to a path to the processorpath attribute.
     * @param ref a reference to a path.
     */
    public void setProcessorpathRef(Reference ref) {
        createProcessorpath().setRefid(ref);
    }

    /**
     * Add a path to the processorpath attribute.
     * @return a path to be configured.
     */
    public Path createProcessorpath() {
        if (processorpath == null) {
            processorpath = new Path(getProject());
        }
        return processorpath.createPath();
    }

    /**
     * Get the factory path attribute.
     * If this is not null, the "-factorypath" argument will be used.
     * The default value is null.
     * @return the factory path attribute.
     */
    public Path getProcessorpath() {
        return processorpath;
    }

    /**
     * Create a nested option.
     * @return an option to be configured.
     */
    public Option createOption() {
        Option opt = new Option();
        options.add(opt);
        return opt;
    }


    /**
     * Create a nested Processors.
     * @return an Processor to be configured.
     */
    public Processor createProcessor() {
        Processor p = new Processor();
        processors.add(p);
        return p;
    }
    

    /**
     * Create a nested option.
     * @return an option to be configured.
     */
    public Jvmarg createJvmargs() {
        Jvmarg args = new Jvmarg();
        jvmargs.add(args);
        return args;
    }
    /**
     * Get the options to the compiler.
     * Each option will use '"-A" name ["=" value]' argument.
     * @return the options.
     */
    public Vector getOptions() {
        return options;
    }


    /**
     * Get the processor
       -processor <class1>[,<class2>,<class3>...]Names of the annotation processors to run; 
      bypasses default discovery process
     * @return the options.
     */
    public Vector getProcessors() {
        return processors;
    }

    /**
     * get runtime arguments.
     * this is corresponding to -J switch
     * @return
     */
    public Vector getJvmargs() {
        return jvmargs;
    }

    /**
     * Get the sourcegendir attribute.
     * This corresponds to the "-s" argument.
     *    -s <directory>             Specify where to place generated source files
     * The default value is null.
     * @return the preprocessdir attribute.
     */
    public File getSourcegendir() {
        return sourcegendir;
    }

    /**
     * Set the preprocessdir attribute.
     * @param sourcegenDir where to place processor generated source files.
     */
    public void setSourcegendir(File sourcegenDir) {
        this.sourcegendir = sourcegenDir;
    }
 
    /**
     * Do the compilation.
     * @throws org.apache.tools.ant.BuildException on error.
     */
    public void execute()
            throws BuildException {
        super.execute();
    }

}
