/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

/**
 *
 * @author sj
 */
public class APIKeyFilter implements ClientRequestFilter {
    
    private final String apiKey;

    public APIKeyFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle("apiKey", apiKey);
    }
}
