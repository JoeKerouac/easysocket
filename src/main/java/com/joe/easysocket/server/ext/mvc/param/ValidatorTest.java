package com.joe.easysocket.server.ext.mvc.param;


import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author joe
 */
public class ValidatorTest {
    public static void main(String[] args) throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
//      验证Bean参数，并返回验证结果信息
        Car car = new Car();


//      验证方法参数
        Method method = null;
        try {
            method = Car.class.getMethod("drive", int.class);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
        Object[] parameterValues = {100};
        ExecutableValidator executableValidator = validator.forExecutables();
        Set<ConstraintViolation<Car>> methodValidators = executableValidator.validateParameters(car,
                method, parameterValues);
        for (ConstraintViolation<Car> constraintViolation : methodValidators) {
            System.out.println(constraintViolation.getMessage());
        }

    }

    public static class Car {

        private String name;

        @NotNull(message = "车主不能为空")
        public String getRentalStation() {
            return name;
        }

        public void drive(@Max(90) int speedInMph) {
            System.out.println();
        }

    }
}

