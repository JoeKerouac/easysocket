package com.joe.easysocket.server.ext.mvc.param;

import com.joe.easysocket.server.ext.mvc.container.Provider;
import com.joe.easysocket.server.ext.mvc.context.RequestContext;
import com.joe.easysocket.server.ext.mvc.exception.ParamValidationException;
import com.joe.easysocket.server.ext.mvc.resource.Param;
import com.joe.parse.json.JsonParser;
import com.joe.type.JavaType;
import com.joe.type.JavaTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * 普通参数解析器
 *
 * @author joe
 */
@Provider
public class GeneralParamParser implements ParamInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ParamInterceptor.class);
    private static final JsonParser parser = JsonParser.getInstance();
//	private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//	private static final ValidatorTest validator = factory.getValidator();

    @Override
    public boolean isReadable(Param<?> param, String data) {
        Annotation[] annotations = param.getType().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof GeneralParam) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object read(Param<?> param, RequestContext.RequestWrapper request, String data) throws ParamValidationException {
        try {
            // 只有一个参数
            JavaType type = param.getType();
            logger.debug("将{}解析为{}", data, type);
            logger.debug("参数{}的类型为{}", param.getName(), type);
            Object result = parser.readAsObject(data, JavaTypeUtil.getRealType(type));
            logger.debug("读取出来的参数是：{}", result);
            return validate(result);
        } catch (ParamValidationException e) {
            logger.debug("解析参数{}时出错，用户数据为：{}" , param , data , e);
            return null;
        }
    }

    /**
     * 校验数据
     *
     * @param obj 要校验的数据
     * @return 校验后的数据
     * @throws ParamValidationException 校验失败直接抛出异常
     */
    private Object validate(Object obj) throws ParamValidationException {
        logger.debug("开始校验：{}", obj);
        if (obj == null) {
            return obj;
        }
//		Set<ConstraintViolation<Object>> set = validator.validate(obj);
//		if (!set.isEmpty()) {
//			logger.debug("数据校验失败，原因：{}", set);
//			ConstraintViolation<Object> constraintViolation = set.iterator().next();
//			throw new ParamValidationException(constraintViolation.getPropertyPath().toString(),
//					constraintViolation.getMessage());
//		}
        logger.debug("数据校验成功");
        return obj;
    }
}
