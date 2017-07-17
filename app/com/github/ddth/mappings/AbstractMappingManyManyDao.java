package com.github.ddth.mappings;

import com.github.ddth.dao.BaseDao;
import com.github.ddth.mappings.utils.MappingsUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Abstract implementation of n-n mappings.
 *
 * <p>n-n mappings: one object can be mapped to multiple targets and vice versa.</p>
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public abstract class AbstractMappingManyManyDao extends BaseDao implements IMappingDao {

    private String cacheName;

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    protected String cacheKeyObjTarget(String namespace, String obj) {
        return "OBJ-" + namespace + "_" + obj;
    }

    protected String cacheKeyTargetObj(String namespace, String target) {
        return "TARGET-" + namespace + "_" + target;
    }

    protected String cacheKeyObjTarget(MappingBo bo) {
        return cacheKeyObjTarget(bo.getNamespace(), bo.getObject());
    }

    protected String cacheKeyTargetObj(MappingBo bo) {
        return cacheKeyTargetObj(bo.getNamespace(), bo.getTarget());
    }

    protected void invalidate(MappingBo bo, MappingsUtils.CacheInvalidationType cit) {
        if (bo != null) {
            removeFromCache(cacheName, cacheKeyObjTarget(bo));
            removeFromCache(cacheName, cacheKeyTargetObj(bo));
        }
    }

    /**
     * Get existing mappings {@code object -> targets}.
     *
     * <p>This method returns an empty set if no mapping found.</p>
     *
     * @param namespace
     * @param obj
     * @return
     */
    protected Collection<MappingBo> getMappingsObjTargets(String namespace, String obj) {
        final String cacheKey = cacheKeyObjTarget(namespace, obj);
        Collection<MappingBo> mappings = getFromCache(cacheName, cacheKey, Collection.class);
        if (mappings == null) {
            mappings = storageGetMappingsObjTargets(namespace, obj);
            putToCache(cacheName, cacheKey, mappings);
        }
        return mappings != null ? mappings : Collections.EMPTY_SET;
    }

    /**
     * Get existing mappings {@code target -> objects}.
     *
     * <p>This method returns an empty set if no mapping found.</p>
     *
     * @param namespace
     * @param target
     * @return
     */
    protected Collection<MappingBo> getMappingsTargetObjs(String namespace, String target) {
        final String cacheKey = cacheKeyTargetObj(namespace, target);
        Collection<MappingBo> mappings = getFromCache(cacheName, cacheKey, Collection.class);
        if (mappings == null) {
            mappings = storageGetMappingsTargetObjs(namespace, target);
            putToCache(cacheName, cacheKey, mappings);
        }
        return mappings != null ? mappings : Collections.EMPTY_SET;
    }

    /**
     * Get existing mappings {@code object -> targets} from storage. Sub-class will implement this
     * method.
     *
     * @param namespace
     * @param obj
     * @return
     */
    protected abstract Collection<MappingBo> storageGetMappingsObjTargets(String namespace, String
            obj);

    /**
     * Get existing mappings {@code target -> objects} from storage. Sub-class will implement this
     * method.
     *
     * @param namespace
     * @param target
     * @return
     */
    protected abstract Collection<MappingBo> storageGetMappingsTargetObjs(String namespace, String
            target);

    /**
     * Save mapping {@code object <-> target} to storage. Sub-class will implement this method.
     *
     * @param mappingToAdd
     * @param existingOT
     * @param existingTO
     * @return
     */
    protected abstract MappingsUtils.DaoResult storageMap(MappingBo mappingToAdd,
            Collection<MappingBo> existingOT, Collection<MappingBo> existingTO);

    /**
     * Remove mapping {@code object <-> target} from storage. Sub-class will implement this method.
     *
     * @param mappingToRemove
     * @param existingOT
     * @param existingTO
     * @return
     */
    protected abstract MappingsUtils.DaoResult storageUnmap(MappingBo mappingToRemove,
            Collection<MappingBo> existingOT, Collection<MappingBo> existingTO);

    /*----------------------------------------------------------------------*/

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingsUtils.DaoResult map(String namespace, String obj, String target) {
        MappingBo mappingToAdd = MappingBo.newInstance(namespace, obj, target);
        Collection<MappingBo> existingOT = getMappingsObjTargets(namespace, obj);
        Collection<MappingBo> existingTO = getMappingsTargetObjs(namespace, target);
        MappingsUtils.DaoResult mapResult = null;
        if (existingOT == null || !existingOT.contains(mappingToAdd)) {
            mapResult = storageMap(mappingToAdd, existingOT, existingTO);
            if (mapResult.status == MappingsUtils.DaoActionStatus.SUCCESSFUL ||
                    mapResult.status == MappingsUtils.DaoActionStatus.DUPLICATED) {
                invalidate(mappingToAdd, MappingsUtils.CacheInvalidationType.DELETE);
            }
        }
        if (mapResult == null) {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.SUCCESSFUL,
                    existingOT);
        } else {
            return new MappingsUtils.DaoResult(mapResult.status, existingOT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingsUtils.DaoResult unmap(String namespace, String obj, String target) {
        MappingBo mappingToRemove = MappingBo.newInstance(namespace, obj, target);
        Collection<MappingBo> existingOT = getMappingsObjTargets(namespace, obj);
        Collection<MappingBo> existingTO = getMappingsTargetObjs(namespace, target);
        MappingsUtils.DaoResult unmapResult = null;
        if (existingOT != null && existingOT.contains(mappingToRemove)) {
            unmapResult = storageUnmap(mappingToRemove, existingOT, existingTO);
            if (unmapResult.status == MappingsUtils.DaoActionStatus.SUCCESSFUL) {
                invalidate(mappingToRemove, MappingsUtils.CacheInvalidationType.DELETE);
            }
        }
        if (unmapResult == null) {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.NOT_FOUND, existingOT);
        } else {
            return new MappingsUtils.DaoResult(unmapResult.status, existingOT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MappingBo> getMappingsForObject(String namespace, String obj) {
        return getMappingsObjTargets(namespace, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MappingBo> getMappingsForTarget(String namespace, String target) {
        return getMappingsTargetObjs(namespace, target);
    }
}
