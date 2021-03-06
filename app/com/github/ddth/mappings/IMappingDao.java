package com.github.ddth.mappings;

import com.github.ddth.mappings.utils.MappingsUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Mappings API.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public interface IMappingDao {
    /**
     * Map an object to target.
     *
     * @param namespace
     * @param obj
     * @param target
     * @return existing mappings {@code object -> targets} if any
     */
    public MappingsUtils.DaoResult map(String namespace, String obj, String target);

    /**
     * Unmap an object from target.
     *
     * @param namespace
     * @param obj
     * @param target
     * @return existing mappings {@code object -> targets} if any
     */
    public MappingsUtils.DaoResult unmap(String namespace, String obj, String target);

    /**
     * Get all mappings (i.e targets) for an object.
     *
     * @param namespace
     * @param obj
     * @return
     */
    public Collection<MappingBo> getMappingsForObject(String namespace, String obj);

    /**
     * Get all mappings (i.e objects) for a target.
     *
     * @param namespace
     * @param target
     * @return
     */
    public Collection<MappingBo> getMappingsForTarget(String namespace, String target);

    /**
     * Get mappings stats.
     *
     * @param namespace
     * @return
     */
    public Map<String, Long> getStats(String namespace);
}
