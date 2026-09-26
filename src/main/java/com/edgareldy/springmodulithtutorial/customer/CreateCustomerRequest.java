package com.edgareldy.springmodulithtutorial.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/customers}, validated before it reaches the service.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 *
 * @param firstName the first name
 * @param lastName  the last name
 * @param telephone the telephone number: digits, optionally a leading +, spaces, dashes and parentheses
 * @param email     a valid email address
 * @param address   the postal address
 */
// The size limits repeat the column lengths of V1, so an oversized value is a 400 naming the field
// rather than a database error surfacing as a 500.
public record CreateCustomerRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{5,}$", message = "must be a valid telephone number")
        String telephone,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 255) String address
) {
}
