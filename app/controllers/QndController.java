package controllers;

import com.github.ddth.mappings.IMappingDao;
import com.github.ddth.mappings.MappingBo;
import com.github.ddth.mappings.utils.MappingsUtils;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Result;

import java.util.Collection;
import java.util.Date;

/**
 * Created by thanhnb on 7/14/17.
 */
public class QndController extends BaseController {

    public Result qnd() throws Exception {
        String action = request().getQueryString("a");
        String namespace = request().getQueryString("ns");
        String obj = request().getQueryString("o");
        String target = request().getQueryString("t");

        IMappingDao mappingOneOne = getRegistry().getMappingOneOneDao();
        if (StringUtils.equalsIgnoreCase(action, "map")) {
            MappingsUtils.DaoResult result = mappingOneOne.map(namespace, obj, target);
            return ok(result.toString());
        }
        if (StringUtils.equalsIgnoreCase(action, "unmap")) {
            MappingsUtils.DaoResult result = mappingOneOne.unmap(namespace, obj, target);
            return ok(result.toString());
        }
        Collection<MappingBo> result = mappingOneOne.getMappingsForObject(namespace, obj);
        return ok(result.toString());
    }
}
