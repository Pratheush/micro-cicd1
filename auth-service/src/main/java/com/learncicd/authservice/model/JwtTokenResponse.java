package com.learncicd.authservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class JwtTokenResponse {
    private String token;
    private String type;
    private String validUntil;
}
