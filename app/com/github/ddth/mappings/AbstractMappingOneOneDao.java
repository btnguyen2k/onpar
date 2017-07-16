package com.github.ddth.mappings;

import com.github.ddth.dao.BaseDao;
import com.github.ddth.mappings.utils.MappingsUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Abstract implementation of 1-1 mappings.
 * <p>
 * <p>1-1 mappings: object can be mapped to only one target and vice versa.</p>
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public abstract class AbstractMappingOneOneDao extends BaseDao implements IMappingDao {

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
            switch (cit) {
                case CREATE:
                case UPDATE:
                    putToCache(cacheName, cacheKeyObjTarget(bo), bo);
                    putToCache(cacheName, cacheKeyTargetObj(bo), bo);
                    break;
                case DELETE:
                    removeFromCache(cacheName, cacheKeyObjTarget(bo));
                    removeFromCache(cacheName, cacheKeyTargetObj(bo));
                    break;
            }
        }
    }

    /**
     * Get existing mapping {@code object -> target}.
     *
     * @param namespace
     * @param obj
     * @return
     */
    protected MappingBo getMappingObjTarget(String namespace, String obj) {
        final String cacheKey = cacheKeyObjTarget(namespace, obj);
        MappingBo bo = getFromCache(cacheName, cacheKey, MappingBo.class);
        if (bo == null) {
            bo = storageGetMappingObjTarget(namespace, obj);
            putToCache(cacheName, cacheKey, bo);
        }
        return bo;
    }

    /**
     * Get existing mapping {@code target -> object}.
     *
     * @param namespace
     * @param target
     * @return
     */
    protected MappingBo getMappingTargetObj(String namespace, String target) {
        final String cacheKey = cacheKeyTargetObj(namespace, target);
        MappingBo bo = getFromCache(cacheName, cacheKey, MappingBo.class);
        if (bo == null) {
            bo = storageGetMappingTargetObj(namespace, target);
            putToCache(cacheName, cacheKey, bo);
        }
        return bo;
    }

    /**
     * Get existing mapping {@code object -> target} from storage. Sub-class will implement this
     * method.
     *
     * @param namespace
     * @param obj
     * @return
     */
    protected abstract MappingBo storageGetMappingObjTarget(String namespace, String obj);

    /**
     * Get existing mapping {@code target -> object} from storage. Sub-class will implement this
     * method.
     *
     * @param namespace
     * @param target
     * @return
     */
    protected abstract MappingBo storageGetMappingTargetObj(String namespace, String target);

    /**
     * Save mapping {@code object <-> target} to storage. Sub-class will implement this method.
     *
     * @param namespace
     * @param obj
     * @param target
     * @param existingOT
     * @param existingTO
     * @return
     */
    protected abstract MappingsUtils.DaoResult storageMap(String namespace, String obj, String
            target, MappingBo existingOT, MappingBo existingTO);

    /**
     * Remove mapping {@code object <-> target} from storage. Sub-class will implement this method.
     *
     * @param namespace
     * @param obj
     * @param target
     * @return
     */
    protected abstract MappingsUtils.DaoResult storageUnmap(String namespace, String obj,
            String target);

    /*----------------------------------------------------------------------*/

    /**
     * Map an object to target.
     * <p>Mapping will fail if {@code obj} is currently mapped to another target.</p>
     *
     * @param namespace
     * @param obj
     * @param target
     * @return
     */
    @Override
    public MappingsUtils.DaoResult map(String namespace, String obj, String target) {
        MappingBo existingOT = getMappingObjTarget(namespace, obj);
        MappingBo existingTO = getMappingTargetObj(namespace, target);
        MappingsUtils.DaoResult mapResult = null;
        if (existingOT == null || !StringUtils.equals(target, existingOT.getTarget())) {
            mapResult = storageMap(namespace, obj, target, existingOT, existingTO);
            if (mapResult.status == MappingsUtils.DaoActionStatus.SUCCESSFUL ||
                    mapResult.status == MappingsUtils.DaoActionStatus.DUPLICATED) {
                invalidate(existingOT, MappingsUtils.CacheInvalidationType.DELETE);
                invalidate(existingTO, MappingsUtils.CacheInvalidationType.DELETE);
                invalidate((MappingBo) mapResult.output,
                        MappingsUtils.CacheInvalidationType.CREATE);
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
        MappingsUtils.DaoResult unmapResult = null;
        MappingBo existing = getMappingObjTarget(namespace, obj);
        if (existing != null && StringUtils.equals(target, existing.getTarget())) {
            unmapResult = storageUnmap(namespace, obj, target);
            if (unmapResult.status == MappingsUtils.DaoActionStatus.SUCCESSFUL) {
                invalidate(existing, MappingsUtils.CacheInvalidationType.DELETE);
            }
        }
        if (unmapResult == null) {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.NOT_FOUND, existing);
        } else {
            return new MappingsUtils.DaoResult(unmapResult.status, existing);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MappingBo> getMappingsForObject(String namespace, String obj) {
        MappingBo existing = getMappingObjTarget(namespace, obj);
        return existing != null ? Collections.singleton(existing) : Collections.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MappingBo> getMappingsForTarget(String namespace, String target) {
        MappingBo existing = getMappingTargetObj(namespace, target);
        return existing != null ? Collections.singleton(existing) : Collections.EMPTY_SET;
    }
}
