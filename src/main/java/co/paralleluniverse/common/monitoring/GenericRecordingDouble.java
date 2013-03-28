/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.monitoring;

/**
 *
 * @author pron
 */
public class GenericRecordingDouble {
    protected final Object clazz;
    protected final int hashCode;

    public GenericRecordingDouble(Class clazz, int hashCode) {
        this.clazz = clazz;
        this.hashCode = hashCode;
    }

    public GenericRecordingDouble(String clazz, int hashCode) {
        this.clazz = clazz;
        this.hashCode = hashCode;
    }

    public GenericRecordingDouble(Object object, String name) {
        if (object == null) {
            this.clazz = null;
            this.hashCode = -1;
        } else {
            this.clazz = name;
            this.hashCode = System.identityHashCode(object);
        }
    }

    public GenericRecordingDouble(Object object) {
        if (object == null) {
            this.clazz = null;
            this.hashCode = -1;
        } else {
            this.clazz = object.getClass();
            this.hashCode = System.identityHashCode(object);
        }
    }

    @Override
    public String toString() {
        if (clazz == null)
            return "null";
        final String name = (clazz instanceof Class ? ((Class)clazz).getSimpleName() : (String)clazz);
        return name + "@" + Integer.toHexString(hashCode);
    }
}
