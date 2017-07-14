package com.github.ddth.mappings;

import java.util.Date;

import com.github.ddth.dao.BaseBo;

/**
 * Mapping {@code object <--> target}.
 * <p>
 * <p> Fields: <ol> <li>{@link #ATTR_NAMESPACE}: mapping namespace, so that multiple mappings can
 * share a same storage.</li> <li>{@link #ATTR_OBJECT}: mapping object.</li> <li>{@link
 * #ATTR_TARGET}: mapping target.</li> <li>{@link #ATTR_TIMESTAMP}: mapping timestamp.</li>
 * <li>{@link #ATTR_INFO}: extra info.</li> </ol> </p>
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class MappingBo extends BaseBo {
    public final static MappingBo[] EMPTY_ARRAY = new MappingBo[0];

    public static MappingBo newInstance(String namespace, String obj, String target) {
        return newInstance(namespace, obj, target, new Date(), null);
    }

    public static MappingBo newInstance(String namespace, String obj, String target, String info) {
        return newInstance(namespace, obj, target, new Date(), info);
    }

    public static MappingBo newInstance(String namespace, String obj, String target, long
            timestamp) {
        return newInstance(namespace, obj, target, new Date(timestamp), null);
    }

    public static MappingBo newInstance(String namespace, String obj, String target,
            Date timestamp) {
        return newInstance(namespace, obj, target, timestamp, null);
    }

    public static MappingBo newInstance(String namespace, String obj, String target, long timestamp,
            String info) {
        return newInstance(namespace, obj, target, new Date(timestamp), info);
    }

    public static MappingBo newInstance(String namespace, String obj, String target, Date timestamp,
            String info) {
        MappingBo bo = new MappingBo();
        bo.setNamespace(namespace).setObject(obj).setTarget(target).setTimestamp(timestamp)
                .setInfo(info);
        return bo;
    }

    private final static String ATTR_NAMESPACE = "ns";
    private final static String ATTR_OBJECT = "obj";
    private final static String ATTR_TARGET = "target";
    private final static String ATTR_TIMESTAMP = "t";
    private final static String ATTR_INFO = "info";

    public String getNamespace() {
        return getAttribute(ATTR_NAMESPACE, String.class);
    }

    public MappingBo setNamespace(String value) {
        return (MappingBo) setAttribute(ATTR_NAMESPACE,
                value != null ? value.trim().toLowerCase() : null);
    }

    public String getObject() {
        return getAttribute(ATTR_OBJECT, String.class);
    }

    public MappingBo setObject(String value) {
        return (MappingBo) setAttribute(ATTR_OBJECT, value != null ? value.trim() : null);
    }

    public String getTarget() {
        return getAttribute(ATTR_TARGET, String.class);
    }

    public MappingBo setTarget(String value) {
        return (MappingBo) setAttribute(ATTR_TARGET, value != null ? value.trim() : null);
    }

    public Date getTimestamp() {
        return getAttribute(ATTR_TIMESTAMP, Date.class);
    }

    public MappingBo setTimestamp(Date value) {
        setAttribute(ATTR_TIMESTAMP, value);
        return this;
    }

    public long getTimestampAsLong() {
        Date value = getTimestamp();
        return value != null ? value.getTime() : 0;
    }

    public MappingBo setTimestamp(long value) {
        setAttribute(ATTR_TIMESTAMP, value);
        return this;
    }

    public String getInfo() {
        return getAttribute(ATTR_INFO, String.class);
    }

    public MappingBo setInfo(String value) {
        return (MappingBo) setAttribute(ATTR_INFO, value != null ? value.trim() : null);
    }

}
