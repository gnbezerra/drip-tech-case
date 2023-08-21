package br.com.usedrip.techcase.george.transferagent

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {
    public override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(TransferAgentApplication::class.java)
    }
}
