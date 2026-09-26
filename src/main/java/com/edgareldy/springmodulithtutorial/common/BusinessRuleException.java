package com.edgareldy.springmodulithtutorial.common;

/**
 * Thrown when a request is well formed but violates a business rule (e.g. deleting a category that
 * still has products); translated into a 422 by {@link GlobalExceptionHandler}.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * @param message the rule that was violated
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
