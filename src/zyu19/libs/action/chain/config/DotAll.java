package zyu19.libs.action.chain.config;

import java.util.List;
import java.util.Objects;

/**
 * A special type to tag the return value of a .then().
 * (If we simply use List&lt;Object&gt;, maybe some .then() also returns such a type, causing a short wait)
 * Created by Zhongzhi Yu on 3/4/16.
 */
public class DotAll {
    public List<Object> objects;
    public DotAll(List<Object> objs) {objects=objs;}
}
