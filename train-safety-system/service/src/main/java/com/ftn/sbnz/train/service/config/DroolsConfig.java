package com.ftn.sbnz.train.service.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DroolsConfig {

    private static final String KMODULE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\">\n" +
            "  <kbase name=\"trainKBase\" eventProcessingMode=\"stream\">\n" +
            "    <ksession name=\"trainKsession\" type=\"stateful\" clockType=\"realtime\" default=\"true\"/>\n" +
            "  </kbase>\n" +
            "</kmodule>\n";

    private static final List<String> STATIC_RULE_PATHS = Arrays.asList(
            "rules/forward/forward-rules.drl",
            "rules/accumulate/accumulate-rules.drl",
            "rules/cep/cep-rules.drl",
            "rules/backward/backward-queries.drl"
    );

    // Project-proposal §17 train-profile rows.
    private static final String[][] PROFILES = {
        // type, brakeRate, tBrake, maxV
        { "Cargo",     "0.6", "4.5", "100" },
        { "Passenger", "0.9", "2.5", "160" },
        { "HighSpeed", "1.3", "1.5", "300" }
    };

    // Project-proposal §18 adhesion rows.
    private static final String[][] ADHESIONS = {
        // weather, mu
        { "Dry",  "0.35" },
        { "Wet",  "0.20" },
        { "Snow", "0.10" },
        { "Ice",  "0.05" }
    };

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(KMODULE_XML);

        for (String path : STATIC_RULE_PATHS) {
            String drl = readClasspath(path);
            kfs.write("src/main/resources/" + path, drl);
        }


        kfs.write("src/main/resources/templates-expanded/templates-expanded.drl",
                  expandTemplate("/templates/templates.drt", templateRows()));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            for (Message m : kb.getResults().getMessages(Message.Level.ERROR, Message.Level.WARNING)) {
                System.err.println("[Drools build] " + m.getLevel() + " " + m.getPath() + ": " + m.getText());
            }
            throw new IllegalStateException("Drools rule compilation failed; see logs.");
        }
        return ks.newKieContainer(kb.getKieModule().getReleaseId());
    }

    // ------------------------------------------------------------------
    // TEMPLATE ROW DATA
    // ------------------------------------------------------------------

    private List<Map<String,String>> templateRows() {
        List<Map<String,String>> rows = new ArrayList<>();
        for (String[] p : PROFILES) {
            for (String[] a : ADHESIONS) {
                rows.add(row(
                    "trainType", p[0],
                    "brakeRate", p[1],
                    "tBrake",    p[2],
                    "maxV",      p[3],
                    "weather",   a[0],
                    "mu",        a[1]
                ));
            }
        }
        return rows;
    }

    private static Map<String,String> row(String... kv) {
        Map<String,String> r = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) r.put(kv[i], kv[i + 1]);
        return r;
    }

    // ------------------------------------------------------------------
    // TEMPLATE EXPANSION
    // ------------------------------------------------------------------
    
    private String expandTemplate(String classpath, List<Map<String,String>> rows) {
        String raw = readClasspath(classpath.startsWith("/") ? classpath.substring(1) : classpath);

        String[] lines = raw.split("\\R");
        int i = 0;
        while (i < lines.length && !lines[i].trim().equals("template header")) i++;
        i++;
        List<String> headerCols = new ArrayList<>();
        while (i < lines.length && !lines[i].trim().isEmpty()) {
            headerCols.add(lines[i].trim());
            i++;
        }
        StringBuilder preamble = new StringBuilder();
        while (i < lines.length && !lines[i].trim().startsWith("template \"")) {
            preamble.append(lines[i]).append("\n");
            i++;
        }
        i++;
        StringBuilder body = new StringBuilder();
        while (i < lines.length && !lines[i].trim().equals("end template")) {
            body.append(lines[i]).append("\n");
            i++;
        }

        String bodyStr = body.toString();
        for (String col : headerCols) {
            if (!bodyStr.contains("@{" + col + "}")) {
                System.err.println("[template " + classpath + "] WARNING: header '" + col
                        + "' is declared but never used in body");
            }
        }

        StringBuilder out = new StringBuilder();
        out.append(preamble);
        int rowNum = 1;
        for (Map<String,String> r : rows) {
            String expanded = bodyStr.replace("@{row.rowNumber}", String.valueOf(rowNum++));
            for (Map.Entry<String,String> kv : r.entrySet()) {
                expanded = expanded.replace("@{" + kv.getKey() + "}", kv.getValue());
            }
            out.append(expanded).append("\n");
        }
        return out.toString();
    }

    private String readClasspath(String path) {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Resource missing on classpath: " + path);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
