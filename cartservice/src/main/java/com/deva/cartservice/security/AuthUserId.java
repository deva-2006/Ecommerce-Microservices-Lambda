package com.deva.cartservice.security;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
//Custom annotation to pass the logged-in user's ID
public @interface AuthUserId {
}