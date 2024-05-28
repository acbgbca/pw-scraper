package com.github.acbgbca.pwscraper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/healthcheck")
public class Healthcheck {
  @GET
  public jakarta.ws.rs.core.Response healthcheck() {
    return jakarta.ws.rs.core.Response.ok("OK", MediaType.TEXT_PLAIN_TYPE).build();
  }
}
