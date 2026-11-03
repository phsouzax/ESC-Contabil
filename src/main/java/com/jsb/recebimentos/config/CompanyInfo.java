package com.jsb.recebimentos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CompanyInfo {
    private final String name;
    private final String cnpj;

    public CompanyInfo(@Value("${app.company.name}") String name,
                       @Value("${app.company.cnpj}") String cnpj) {
        this.name = name;
        this.cnpj = cnpj;
    }

    public String getName() { return name; }
    public String getCnpj() { return cnpj; }
}
