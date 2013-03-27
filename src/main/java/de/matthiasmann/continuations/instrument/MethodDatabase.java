/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.continuations.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.ClassReader;

/**
 * <p>Collects information about classes and their suspendable methods.</p>
 * <p>Provides access to configuration parameters and to logging</p>
 * 
 * @author Matthias Mann
 */
public class MethodDatabase implements Log {
    
    private final ClassLoader cl;
    private final HashMap<String, ClassEntry> classes;
    private final HashMap<String, String> superClasses;
    private final ArrayList<File> workList;
    
    private Log log;
    private boolean verbose;
    private boolean debug;
    private boolean allowMonitors;
    private boolean allowBlocking;
    private int logLevelMask;
    
    public MethodDatabase(ClassLoader classloader) {
        if(classloader == null) {
            throw new NullPointerException("classloader");
        }
        
        this.cl = classloader;
        
        classes = new HashMap<String, ClassEntry>();
        superClasses = new HashMap<String, String>();
        workList = new ArrayList<File>();
        
        setLogLevelMask();
    }

    public boolean isAllowMonitors() {
        return allowMonitors;
    }

    public void setAllowMonitors(boolean allowMonitors) {
        this.allowMonitors = allowMonitors;
    }

    public boolean isAllowBlocking() {
        return allowBlocking;
    }

    public void setAllowBlocking(boolean allowBlocking) {
        this.allowBlocking = allowBlocking;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        setLogLevelMask();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        setLogLevelMask();
    }

    private void setLogLevelMask() {
        logLevelMask = (1 << LogLevel.WARNING.ordinal());
        if(verbose || debug) {
            logLevelMask |= (1 << LogLevel.INFO.ordinal());
        }
        if(debug) {
            logLevelMask |= (1 << LogLevel.DEBUG.ordinal());
        }
    }
    
    public void log(LogLevel level, String msg, Object ... args) {
        if(log != null && (logLevelMask & (1 << level.ordinal())) != 0) {
            log.log(level, msg, args);
        }
    }
    
    public void error(String msg, Exception ex) {
        if(log != null) {
            log.error(msg, ex);
        }
    }
    
    public void checkClass(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            CheckInstrumentationVisitor civ = checkFileAndClose(fis, f.getPath());
            
            if(civ != null) {
                recordSuspendableMethods(civ.getName(), civ.getClassEntry());

                if(civ.needsInstrumentation()) {
                    if(civ.isAlreadyInstrumented()) {
                        log(LogLevel.INFO, "Found instrumented class: %s", f.getPath());
                    } else {
                        log(LogLevel.INFO, "Found class: %s", f.getPath());
                        workList.add(f);
                    }
                }
            }
        } catch (UnableToInstrumentException ex) {
            throw ex;
        } catch (Exception ex) {
            error(f.getPath(), ex);
        }
    }
    
    public boolean isMethodSuspendable(String className, String methodName, String methodDesc, boolean searchSuperClass) {
        if(methodName.charAt(0) == '<') {
            return false;   // special methods are never suspendable
        }
        
        if(isJavaCore(className)) {
            return false;
        }

        String curClassName = className;
        do {
            ClassEntry entry = getClassEntry(curClassName);
            if(entry == null) {
                entry = CLASS_NOT_FOUND;
                
                if(cl != null) {
                    log(LogLevel.INFO, "Trying to read class: %s", curClassName);

                    CheckInstrumentationVisitor civ = checkClass(curClassName);
                    if(civ == null) {
                        log(LogLevel.WARNING, "Class not found assuming suspendable: %s", curClassName);
                    } else {
                        entry = civ.getClassEntry();
                    }
                } else {
                    log(LogLevel.WARNING, "Can't check class - assuming suspendable: %s", curClassName);
                }
                
                recordSuspendableMethods(curClassName, entry);
            }
            
            if(entry == CLASS_NOT_FOUND) {
                return true;
            }

            Boolean suspendable = entry.check(methodName, methodDesc);
            if(suspendable != null) {
                return suspendable;
            }
            
            curClassName = entry.superName;
        } while(searchSuperClass && curClassName != null);
        
        log(LogLevel.WARNING, "Method not found in class - assuming suspendable: %s#%s%s", className, methodName, methodDesc);
        return true;
    }

    private synchronized ClassEntry getClassEntry(String className) {
        return classes.get(className);
    }

    void recordSuspendableMethods(String className, ClassEntry entry) {
        ClassEntry oldEntry;
        synchronized(this) {
            oldEntry = classes.put(className, entry);
        }
        if(oldEntry != null) {
            if(!oldEntry.equals(entry)) {
                log(LogLevel.WARNING, "Duplicate class entries with different data for class: %s", className);
            }
        }
    }
    
    public String getCommonSuperClass(String classA, String classB) {
        ArrayList<String> listA = getSuperClasses(classA);
        ArrayList<String> listB = getSuperClasses(classB);
        if(listA == null || listB == null) {
            return null;
        }
        int idx = 0;
        int num = Math.min(listA.size(), listB.size());
        for(; idx<num ; idx++) {
            String superClassA = listA.get(idx);
            String superClassB = listB.get(idx);
            if(!superClassA.equals(superClassB)) {
                break;
            }
        }
        if(idx > 0) {
            return listA.get(idx-1);
        }
        return null;
    }

    public boolean isException(String className) {
        for(;;) {
            if("java/lang/Throwable".equals(className)) {
                return true;
            }
            if("java/lang/Object".equals(className)) {
                return false;
            }

            String superClass = getDirectSuperClass(className);
            if(superClass == null) {
                log(LogLevel.WARNING, "Can't determine super class of %s", className);
                return false;
            }
            className = superClass;
        }
    }

    public ArrayList<File> getWorkList() {
        return workList;
    }

    /**
     * <p>Overwrite this function if Coroutines is used in a transformation chain.</p>
     * <p>This method must create a new CheckInstrumentationVisitor and visit the
     * specified class with it.</p>
     * @param className the class the needs to be analysed
     * @return a new CheckInstrumentationVisitor that has visited the specified
     * class or null if the class was not found
     */
    protected CheckInstrumentationVisitor checkClass(String className) {
        InputStream is = cl.getResourceAsStream(className + ".class");
        if(is != null) {
            return checkFileAndClose(is, className);
        }
        return null;
    }
    
    private CheckInstrumentationVisitor checkFileAndClose(InputStream is, String name) {
        try {
            try {
                ClassReader r = new ClassReader(is);

                CheckInstrumentationVisitor civ = new CheckInstrumentationVisitor();
                r.accept(civ, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES|ClassReader.SKIP_CODE);
                
                return civ;
            } finally {
                is.close();
            }
        } catch (UnableToInstrumentException ex) {
            throw ex;
        } catch (Exception ex) {
            error(name, ex);
        }
        return null;
    }

    private String extractSuperClass(String className) {
        InputStream is = cl.getResourceAsStream(className + ".class");
        if(is != null) {
            try {
                try {
                    ClassReader r = new ClassReader(is);
                    ExtractSuperClass esc = new ExtractSuperClass();
                    r.accept(esc, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                    return esc.superClass;
                } finally {
                    is.close();
                }
            } catch(IOException ex) {
                error(className, ex);
            }
        }
        return null;
    }

    private ArrayList<String> getSuperClasses(String className) {
        ArrayList<String> result = new ArrayList<String>();
        for(;;) {
            result.add(0, className);
            if("java/lang/Object".equals(className)) {
                return result;
            }

            String superClass = getDirectSuperClass(className);
            if(superClass == null) {
                log(LogLevel.WARNING, "Can't determine super class of %s", className);
                return null;
            }
            className = superClass;
        }
    }

    protected String getDirectSuperClass(String className) {
        ClassEntry entry = getClassEntry(className);
        if(entry != null && entry != CLASS_NOT_FOUND) {
            return entry.superName;
        }
        
        String superClass;
        synchronized(this) {
            superClass = superClasses.get(className);
        }
        if(superClass == null) {
            superClass = extractSuperClass(className);
            if(superClass != null) {
                String oldSuperClass;
                synchronized (this) {
                    oldSuperClass = superClasses.put(className, superClass);
                }
                if(oldSuperClass != null) {
                    if(!oldSuperClass.equals(superClass)) {
                        log(LogLevel.WARNING, "Duplicate super class entry with different value: %s vs %s", oldSuperClass, superClass);
                    }
                }
            }
        }
        return superClass;
    }
    
    public static boolean isJavaCore(String className) {
        return className.startsWith("java/") || className.startsWith("javax/") ||
                className.startsWith("sun/") || className.startsWith("com/sun/");
    }
    
    private static final ClassEntry CLASS_NOT_FOUND = new ClassEntry("<class not found>");

    static final class ClassEntry {
        private final HashMap<String, Boolean> methods;
        final String superName;

        public ClassEntry(String superName) {
            this.superName = superName;
            this.methods = new HashMap<String, Boolean>();
        }
        
        public void set(String name, String desc, boolean suspendable) {
            String nameAndDesc = key(name, desc);
            methods.put(nameAndDesc, suspendable);
        }
        
        public Boolean check(String name, String desc) {
            return methods.get(key(name, desc));
        }

        @Override
        public int hashCode() {
            return superName.hashCode() * 67 + methods.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof ClassEntry)) {
                return false;
            }
            final ClassEntry other = (ClassEntry)obj;
            return superName.equals(other.superName) && methods.equals(other.methods);
        }
        
        private static String key(String methodName, String methodDesc) {
            return methodName.concat(methodDesc);
        }
    }
}
